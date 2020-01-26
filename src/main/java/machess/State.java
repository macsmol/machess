package machess;

import com.sun.istack.internal.Nullable;

import java.util.*;

import static machess.Square.*;

/**
 * Game state seen as game board rather than a list of figures.
 * All is static in order to avoid allocations.
 */
public class State {
	public static final boolean WHITE = true;
	public static final boolean BLACK = false;

	/**
	 * Bit layout of each square is
	 * bbbbwwww---ncppp
	 *
	 * p - piece type
	 * c - is piece white flag
	 * n - no kings flag (for squares adjacent to two kings at once)
	 * w - checks by white count
	 * b - checks by black count
	 */
	public static final class SquareFormat {
		static final byte PIECE_TYPE_MASK = 0x07;
		static final byte IS_WHITE_PIECE_FLAG = 0x08;
		// neither king can walk onto a square with this flag set - squares adjacent to both kings at once
		static final byte NO_KINGS_FLAG = 0x10;
		// mask to get checks count by black or white. Four MSBits are checks by black, next 4 bits are checks by white.
		static final byte CHECKS_COUNT_MASK = 0x0F;
		static final byte CHECKS_BY_WHITE_BIT_OFFSET = 8;
		static final byte CHECKS_BY_BLACK_BIT_OFFSET = 12;
	}

	// flags
	private final byte flags;
	static final int WHITE_TURN 			= 0x01;
	static final int WHITE_KING_MOVED 		= 0x02;
	static final int BLACK_KING_MOVED 		= 0x04;
	static final int WHITE_KS_ROOK_MOVED 	= 0x08;
	static final int WHITE_QS_ROOK_MOVED 	= 0x10;
	static final int BLACK_KS_ROOK_MOVED 	= 0x20;
	static final int BLACK_QS_ROOK_MOVED 	= 0x40;

	/**
	 * one byte per square.
	 */
	final short[] board;

	final PieceLists pieces;
	/**
	 * If not null it means there is a possibility to en-passant on this square
	 */
	private final Square enPassantSquare;
	// not really necessary - only for debug purposes
	private final int plyNumber;

	/**
	 * Board with absolutely pinned pieces. It's indexed by Square.ordinal()
	 */
	private final Pin[] pinnedPieces;

	/**
	 * new game
	 */
	State() {
		flags = WHITE_TURN;
		board = new short[Square.values().length];
		for (int file = File.A; file <= File.H; file++) {
			board[Square.fromLegalInts(file, Rank._2).ordinal()] = Content.WHITE_PAWN.asByte;
			board[Square.fromLegalInts(file, Rank._7).ordinal()] = Content.BLACK_PAWN.asByte;

			switch (file) {
				case File.A:
				case File.H:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_ROOK.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_ROOK.asByte;
					break;
				case File.B:
				case File.G:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_KNIGHT.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_KNIGHT.asByte;
					break;
				case File.C:
				case File.F:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_BISHOP.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_BISHOP.asByte;
					break;
				case File.D:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_QUEEN.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_QUEEN.asByte;
					break;
				case File.E:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_KING.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_KING.asByte;
					break;
			}
		}
		pieces = new PieceLists();
		enPassantSquare = null;
		plyNumber = 1;

		pinnedPieces = new Pin[Square.values().length];
		initSquaresInCheck();
	}

	private State(short[] board, PieceLists pieces, byte flags,
				  @Nullable Square enPassantSquare, int plyNumber) {
		this.board = board;
		this.pieces = pieces;
		this.flags = flags;
		this.enPassantSquare = enPassantSquare;
		this.plyNumber = plyNumber;
		pieces.sortOccupiedSquares();

		resetSquaresInCheck();
		initSquaresInCheck();

		pinnedPieces = new Pin[Square.values().length];
		initPinnedPieces();
	}



	private State fromPseudoLegalPawnDoublePush(Square from, Square to, Square enPassantSquare) {
		assert enPassantSquare != null;
		return fromPseudoLegalMove(from, to, null, enPassantSquare, null);
	}

	private State fromPseudoLegalMoveWithPromotion(Square from, Square to, Content promotion) {
		assert promotion != null;
		return fromPseudoLegalMove(from, to, promotion, null, null);
	}

	private State fromLegalQueensideCastling(Square kingFrom, Square kingTo) {
		Square rookToCastle = Square.fromLegalInts(File.A, kingFrom.rank);
		return fromPseudoLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}

	private State fromLegalKingsideCastling(Square kingFrom, Square kingTo) {
		Square rookToCastle = Square.fromLegalInts(File.H, kingFrom.rank);
		return fromPseudoLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}

	/**
	 * Generates new BoardState based on move. Typical move without special events.
	 */
	State fromPseudoLegalMove(Square from, Square to) {
		return fromPseudoLegalMove(from, to, null, null, null);
	}

	/**
	 * Generates new BoardState based on move. It does not verify game rules - assumes input is a legal move.
	 * This is the root method - it covers all cases. All 'overload' methods should call this one.
	 */
	private State fromPseudoLegalMove(Square from, Square to, @Nullable Content promotion, @Nullable Square futureEnPassantSquare,
									  @Nullable Square rookCastleFrom) {
		assert from != to : from + "->" + to + " is no move";
		short[] boardCopy = board.clone();
		PieceLists piecesCopy = pieces.clone();

		//  update boardCopy
		Content movedPiece = Content.fromShort(boardCopy[from.ordinal()]);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		assert movedPiece.isWhite == test(WHITE_TURN) : "Moved " + movedPiece + " on " + (test(WHITE_TURN) ? "white" : "black") + " turn";
		boardCopy[from.ordinal()] = Content.EMPTY.asByte;

		Content takenPiece = Content.fromShort(boardCopy[to.ordinal()]);
		assert takenPiece != Content.BLACK_KING && takenPiece != Content.WHITE_KING : from + "->" + to + " is taking king";
		boardCopy[to.ordinal()] = movedPiece.asByte;

		piecesCopy.move(movedPiece, from, to);

		Square squareWithPawnTakenEnPassant = null;
		if (enPassantSquare == to) {
			if (movedPiece == Content.WHITE_PAWN) {
				squareWithPawnTakenEnPassant = Square.fromLegalInts(to.file, to.rank - 1);
				takenPiece = Content.fromShort(boardCopy[squareWithPawnTakenEnPassant.ordinal()]);
				boardCopy[squareWithPawnTakenEnPassant.ordinal()] = Content.EMPTY.asByte;
			} else if (movedPiece == Content.BLACK_PAWN) {
				squareWithPawnTakenEnPassant = Square.fromLegalInts(to.file, to.rank + 1);
				takenPiece = Content.fromShort(boardCopy[squareWithPawnTakenEnPassant.ordinal()]);
				boardCopy[squareWithPawnTakenEnPassant.ordinal()] = Content.EMPTY.asByte;
			}
		} else if (rookCastleFrom != null) {
			boardCopy[rookCastleFrom.ordinal()] = Content.EMPTY.asByte;
			Content rook = test(WHITE_TURN) ? Content.WHITE_ROOK : Content.BLACK_ROOK;
			Square rookDestination;
			if (rookCastleFrom.file == 0) {
				rookDestination = test(WHITE_TURN) ? Square.D1 : Square.D8;
				boardCopy[rookDestination.ordinal()] = rook.asByte;
			} else {
				rookDestination = test(WHITE_TURN) ? Square.F1 : Square.F8;
				boardCopy[rookDestination.ordinal()] = rook.asByte;
			}
			// update pieces lists
			piecesCopy.move(rook, rookCastleFrom, rookDestination);
		} else if (promotion != null) {
			boardCopy[to.ordinal()] = promotion.asByte;
			piecesCopy.promote(to, promotion);
		}

		if (takenPiece != Content.EMPTY) {
			assert movedPiece.isWhite != takenPiece.isWhite : from + "->" + to + " is a friendly take";
			piecesCopy.kill(takenPiece, squareWithPawnTakenEnPassant != null ? squareWithPawnTakenEnPassant : to);
		}

		int flagsCopy = flags ^ WHITE_TURN;
		if (from == Square.E1) {
			flagsCopy |= WHITE_KING_MOVED;
		} else if (from == Square.E8) {
			flagsCopy |= BLACK_KING_MOVED;
		} else if (from == Square.A1) {
			flagsCopy |= WHITE_QS_ROOK_MOVED;
		} else if (from == Square.H1) {
			flagsCopy |= WHITE_KS_ROOK_MOVED;
		} else if (from == Square.A8) {
			flagsCopy |= BLACK_QS_ROOK_MOVED;
		} else if (from == Square.H8) {
			flagsCopy |= BLACK_KS_ROOK_MOVED;
		}

		return new State(boardCopy, piecesCopy, (byte) flagsCopy, futureEnPassantSquare, plyNumber + 1);
	}

	boolean test(int flagMask) {
		return (flags & flagMask) != 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: ").append(test(WHITE_TURN) ? "WHITE" : "BLACK");
		sb.append(test(WHITE_KING_MOVED) ? "; WHITE_KING_MOVED" : "");
		sb.append(test(WHITE_QS_ROOK_MOVED) ? "; WHITE_QS_ROOK_MOVED" : "");
		sb.append(test(WHITE_KS_ROOK_MOVED) ? "; WHITE_KS_ROOK_MOVED" : "");
		sb.append(test(BLACK_KING_MOVED) ? "; BLACK_KING_MOVED" : "");
		sb.append(test(BLACK_QS_ROOK_MOVED) ? "; BLACK_QS_ROOK_MOVED" : "");
		sb.append(test(BLACK_KS_ROOK_MOVED) ? "; BLACK_KS_ROOK_MOVED" : "");
		sb.append("\n | a  | b  | c  | d  | e  | f  | g  | h  |");
		sb.append(Config.DEBUG_FIELD_IN_CHECK_FLAGS	? "| a  | b  | c  | d  | e  | f  | g  | h  |" : "");
		sb.append(Config.DEBUG_PINNED_PIECES 		? "| a  | b  | c  | d  | e  | f  | g  | h  |" : "");
		sb.append('\n');
		sb.append("==========================================");
		sb.append(Config.DEBUG_FIELD_IN_CHECK_FLAGS	? "=========================================" : "");
		sb.append(Config.DEBUG_PINNED_PIECES 		? "=========================================" : "");
		sb.append('\n');
		for (byte rank = Rank._8; rank >= Rank._1; rank--) {
			StringBuilder sbCheckFlags = 	new StringBuilder(Config.DEBUG_FIELD_IN_CHECK_FLAGS 	? "|" : "");
			StringBuilder sbPins = 			new StringBuilder(Config.DEBUG_PINNED_PIECES 			? "|" : "");
			sb.append(rank + 1).append("|");
			for (byte file = File.A; file <= File.H; file++) {
				Content content = getContent(file, rank);
				sb.append(content.symbol).append(" |");

				if (Config.DEBUG_FIELD_IN_CHECK_FLAGS) {
					short contentAsShort = board[Square.fromLegalInts(file, rank).ordinal()];
					sbCheckFlags.append(Utils.checkCountsToString(contentAsShort)).append('|');
				}
				if (Config.DEBUG_PINNED_PIECES) {
					Pin pinType = pinnedPieces[Square.fromLegalInts(file, rank).ordinal()];
					sbPins.append(" ").append(pinType != null ? pinType.symbol : ' ').append("  |");
				}
			}
			sb.append(sbCheckFlags).append(sbPins)
					.append("\n-+----+----+----+----+----+----+----+----+")
					.append(Config.DEBUG_FIELD_IN_CHECK_FLAGS 	? "+----+----+----+----+----+----+----+----+" : "")
					.append(Config.DEBUG_PINNED_PIECES 			? "+----+----+----+----+----+----+----+----+" : "")
					.append('\n');
		}
		sb.append(pieces);
		sb.append("enPassantSquare: ").append(enPassantSquare).append('\n');
		sb.append("plyNumber: ").append(plyNumber).append('\n');
		return sb.toString();
	}

	private Content getContent(int file, int rank) {
		return getContent(Square.fromLegalInts(file, rank));
	}

	Content getContent(Square square) {
		return Content.fromShort(board[square.ordinal()]);
	}

	private boolean isPromotingSquare(Square square) {
		return test(WHITE_TURN) ? square.rank == Rank.WHITE_PROMOTION_RANK : square.rank == Rank.BLACK_PROMOTION_RANK;
	}

	private boolean isInitialSquareOfPawn(Square square) {
		return test(WHITE_TURN) ? square.rank == Rank.WHITE_PAWN_INITIAL_RANK : square.rank == Rank.BLACK_PAWN_INITIAL_RANK;
	}

	/**
	 * Tells if Square square is occupied by a piece of color that's currently taking turn.
	 */
	boolean isSameColorPieceOn(Square square) {
		return test(WHITE_TURN) ? isWhitePieceOn(square) : isBlackPieceOn(square);
	}

	/**
	 * Tells if Square square is occupied by a piece of color isWhite
	 */
	boolean isSameColorPieceOn(Square square, boolean isWhite) {
		return isWhite ? isWhitePieceOn(square) : isBlackPieceOn(square);
	}

	/**
	 * Tells if Square square is occupied by a piece of color that's currently taking turn.
	 */
	boolean isOppositeColorPieceOn(Square square) {
		return test(WHITE_TURN) ? isBlackPieceOn(square) : isWhitePieceOn(square);
	}

	/**
	 * Tells if Square square is occupied by a piece of color isWhite.
	 */
	boolean isOppositeColorPieceOn(Square square, boolean isWhite) {
		return isWhite ? isBlackPieceOn(square) : isWhitePieceOn(square);
	}

	private boolean isWhitePieceOn(Square square) {
		short contentAsShort = board[square.ordinal()];
		return (contentAsShort & SquareFormat.IS_WHITE_PIECE_FLAG) != 0 && (contentAsShort & SquareFormat.PIECE_TYPE_MASK) != 0;
	}

	private boolean isBlackPieceOn(Square square) {
		short contentAsShort = board[square.ordinal()];
		return (contentAsShort & SquareFormat.IS_WHITE_PIECE_FLAG) == 0 && (contentAsShort & SquareFormat.PIECE_TYPE_MASK) != 0;
	}

	private void resetSquaresInCheck() {
		for (int i = 0; i < board.length; i++) {
			short contentAsShort = board[i];
			board[i] = (byte) (contentAsShort & (SquareFormat.PIECE_TYPE_MASK | SquareFormat.IS_WHITE_PIECE_FLAG));
		}
	}

	private void initSquaresInCheck() {
		initChecksAroundKing(BLACK);
		initChecksAroundKing(WHITE);
		initSquaresInCheckByKings();
	}

	private void initPinnedPieces() {
		Square whiteKing = pieces.getWhiteKing();
		Square blackKing = pieces.getBlackKing();
		assert getContent(whiteKing) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing) == Content.BLACK_KING : "Corrupted black king position";

		for (Square sq : Square.values()) {
			pinnedPieces[sq.ordinal()] = null;
		}

		initPinnedPieces(whiteKing, WHITE);
		initPinnedPieces(blackKing, BLACK);
	}

	private void initPinnedPieces(Square king, boolean isPinnedToWhiteKing) {
		initPinInOneDirection(king, isPinnedToWhiteKing, -1, -1);
		initPinInOneDirection(king, isPinnedToWhiteKing, -1, 0);
		initPinInOneDirection(king, isPinnedToWhiteKing, -1, 1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 0, -1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 0, 1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 1, -1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 1, 0);
		initPinInOneDirection(king, isPinnedToWhiteKing, 1, 1);
	}

	private void initPinInOneDirection(Square king, boolean isPinnedToWhiteKing, int deltaFile, int deltaRank) {
		Square candidate = null;
		for (int i = 1; true; i++) {
			Square square = Square.fromInts(king.file + i * deltaFile, king.rank + i * deltaRank);

			if (square == null || (isOppositeColorPieceOn(square, isPinnedToWhiteKing) && candidate == null)) {
				return;
			}
			if (isSameColorPieceOn(square, isPinnedToWhiteKing)) {
				if (candidate == null) {
					candidate = square;
					continue;
				} else {
					return;
				}
			}
			if (candidate != null) {
				Content afterCandidate = getContent(square);
				Content enemyQueen = isPinnedToWhiteKing ? Content.BLACK_QUEEN : Content.WHITE_QUEEN;
				Content enemySlider = Content.rookOrBishop(!isPinnedToWhiteKing, deltaFile, deltaRank);
				if (afterCandidate == enemyQueen || afterCandidate == enemySlider) {
					pinnedPieces[candidate.ordinal()] = Pin.fromDeltas(deltaFile, deltaRank);
					return;
				} else if (afterCandidate != Content.EMPTY) {
					return;
				}
			}
		}
	}

	private void initChecksAroundKing(boolean isCheckedByWhite) {
		Square king = isCheckedByWhite ? pieces.getBlackKing() : pieces.getWhiteKing();
		int kingMovedFlag = isCheckedByWhite ? BLACK_KING_MOVED : WHITE_KING_MOVED;
		boolean hasKingMoved = test(kingMovedFlag);

		if (isCheckedByWhite) {
			initChecksByPawns(king, hasKingMoved, isCheckedByWhite, pieces.whitePawns, pieces.whitePawnsCount);
			initChecksByKnights(king, hasKingMoved, isCheckedByWhite, pieces.whiteKnights, pieces.whiteKnightsCount);
			initChecksByBishops(king, isCheckedByWhite, pieces.whiteBishops, pieces.whiteBishopsCount);
			initChecksByRooks(king, isCheckedByWhite, pieces.whiteRooks, pieces.whiteRooksCount);
			initChecksByQueens(king, isCheckedByWhite, pieces.whiteQueens, pieces.whiteQueensCount);
		} else {
			initChecksByPawns(king, hasKingMoved, isCheckedByWhite, pieces.blackPawns, pieces.blackPawnsCount);
			initChecksByKnights(king, hasKingMoved, isCheckedByWhite, pieces.blackKnights, pieces.blackKnightsCount);
			initChecksByBishops(king, isCheckedByWhite, pieces.blackBishops, pieces.blackBishopsCount);
			initChecksByRooks(king, isCheckedByWhite, pieces.blackRooks, pieces.blackRooksCount);
			initChecksByQueens(king, isCheckedByWhite, pieces.blackQueens, pieces.blackQueensCount);
		}
	}


	private void initChecksByPawns(Square king, boolean hasKingMoved, boolean isCheckedByWhite,
								   Square[] pawns, byte pawnsCount) {
		Arrays.sort(pawns, 0, pawnsCount, Comparator.comparingInt(sq -> sq.file));

		byte minKingSafeDistance = (byte)(hasKingMoved ? 2 : 3);
		for (int i = pawnsCount / 2; i >= 0; i--) {
			if (pawns[i].file < king.file - minKingSafeDistance) {
				break;
			}
			initSquaresInCheckByPawn(pawns[i], isCheckedByWhite);
		}
		for (int i = pawnsCount / 2 + 1; i < pawnsCount; i++) {
			if (pawns[i].file > king.file + minKingSafeDistance) {
				break;
			}
			initSquaresInCheckByPawn(pawns[i], isCheckedByWhite);
		}
	}

	private void initChecksByKnights(Square king, boolean hasKingMoved, boolean isCheckedByWhite, Square[] knights,
			byte knightsCount) {
		byte minKingSafeDistance = (byte)(hasKingMoved ? 4 : 5);
		for (int i = 0; i < knightsCount; i++) {
			if (Math.abs(knights[i].file - king.file) < minKingSafeDistance && Math.abs(knights[i].rank - king.rank) < minKingSafeDistance) {
				initSquaresInCheckByKnight(knights[i],isCheckedByWhite);
			}
		}
	}

	private void initChecksByBishops(Square king, boolean isCheckedByWhite, Square[] bishops, byte bishopsCount) {
		for (int i = 0; i < bishopsCount; i++) {
			byte deltaRank = (byte)(king.rank - bishops[i].rank);

			if (deltaRank > 0) {
				// king above
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, 1, 1);
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, -1, 1);
			} else if (deltaRank < 0) {
				//king below
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, 1, -1);
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, -1, -1);
			} else {
				byte deltaFile = (byte) (king.file - bishops[i].file);
				if (deltaFile > 0) { // king to the right
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, 1, 1);
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, 1, -1);
				} else {
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, -1, 1);
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, -1, -1);
				}
			}
		}
	}

	private void initChecksByRooks(Square king, boolean isCheckedByWhite, Square[] rooks, byte rooksCount) {
		for (int i = 0; i < rooksCount; i++) {
			initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, 0, 1);
			initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, 0, -1);

			byte deltaRank = (byte)(king.rank - rooks[i].rank);
			if (isCloseEnoughToRank(deltaRank)) {
				initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, 1, 0);
				initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, -1, 0);
			}
		}
	}

	private boolean isCloseEnoughToRank(byte deltaRank) {
		return Math.abs(deltaRank) <= 1;
	}

	private void initChecksByQueens(Square king, boolean isCheckedByWhite, Square[] queens, byte queensCount) {
		for (int i = 0; i < queensCount; i++) {
			byte deltaRank = (byte) (king.rank - queens[i].rank);
			if (deltaRank > 1) { // king at least two ranks above
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, 1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, 1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, 1);
			} else if (deltaRank < -1) { // king at least two ranks below
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, -1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, -1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, -1);
			} else if (deltaRank > 0) { // king just above
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, 1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, 1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, 1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, 0);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, 0);
			} else if (deltaRank < 0) { // king just below
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, -1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, -1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, -1);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, 0);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, 0);
			} else { // same rank
				byte deltaFile = (byte) (king.file - queens[i].file);
				if (deltaFile > 0) { // king to the right
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, 1);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, 0);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 1, -1);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, 1);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, -1);
				} else { // king to the left
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, 1);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, 0);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, -1, -1);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, 1);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, 0, -1);
				}
			}
		}
	}

	private void initSquaresInCheckByKings() {
		Square whiteKing = pieces.getWhiteKing();
		Square blackKing = pieces.getBlackKing();
		assert getContent(whiteKing) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing) == Content.BLACK_KING : "Corrupted black king position";
		assert Math.abs(blackKing.rank - whiteKing.rank) > 1
				|| Math.abs(blackKing.file - whiteKing.file) > 1 : "Kings to close. w: " + whiteKing + ", b: " + blackKing;

		Set<Square> whiteNeighbourhood = getKingNeighbourhood(whiteKing);
		Set<Square> blackNeighbourhood = getKingNeighbourhood(blackKing);

		Set<Square> noKingsZone = new HashSet<>(whiteNeighbourhood);
		if (noKingsZone.retainAll(blackNeighbourhood)) {
			for (Square square : noKingsZone) {
				setNoKingFlagOnSquare(square);
			}
			whiteNeighbourhood.removeAll(noKingsZone);
			blackNeighbourhood.removeAll(noKingsZone);
		}
		for (Square square : whiteNeighbourhood) {
			setCheckFlagOnSquareByKing(square, WHITE);
		}
		for (Square square : blackNeighbourhood) {
			setCheckFlagOnSquareByKing(square, BLACK);
		}
	}

	private void setCheckFlagOnSquareByKing(Square square, boolean isKingWhite) {
		if (!isSquareCheckedBy(square, !isKingWhite)) {
			incrementChecksOnSquare(square, isKingWhite);
		}
	}

	private Set<Square> getKingNeighbourhood(Square king) {
		// TODO use spare flags in SquareFormat to find NO_KING_SQUARES
		Set<Square> neighbourhood = new HashSet<>();
		Square to = Square.fromInts(king.file, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Square.fromInts(king.file + 1, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Square.fromInts(king.file + 1, king.rank);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Square.fromInts(king.file + 1, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Square.fromInts(king.file, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Square.fromInts(king.file - 1, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Square.fromInts(king.file - 1, king.rank);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Square.fromInts(king.file - 1, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		return neighbourhood;
	}

	private void initSquaresInCheckByBishop(Square bishopSquare, boolean isCheckedByWhite) {
		initCheckFlagsBySlidingPiece(bishopSquare, isCheckedByWhite, 1, 1);
		initCheckFlagsBySlidingPiece(bishopSquare, isCheckedByWhite, 1, -1);
		initCheckFlagsBySlidingPiece(bishopSquare, isCheckedByWhite, -1, 1);
		initCheckFlagsBySlidingPiece(bishopSquare, isCheckedByWhite, -1, -1);
	}

	private void initCheckFlagsBySlidingPiece(Square from, boolean isCheckedByWhite, int deltaFile, int deltaRank) {
		for (int i = 1; true; i++) {
			Square underCheck = Square.fromInts(from.file + deltaFile * i, from.rank + i * deltaRank);
			if (underCheck == null) {
				break;
			} else if (isSameColorPieceOn(underCheck, isCheckedByWhite)) {
				incrementChecksOnSquare(underCheck, isCheckedByWhite);
				break;
			}
			incrementChecksOnSquare(underCheck, isCheckedByWhite);
			if (isOppositeColorPieceOn(underCheck, isCheckedByWhite)) {
				break;
			}
		}
	}

	private void initSquaresInCheckByPawn(Square from, boolean isCheckedByWhite) {
		int pawnDisplacement = isCheckedByWhite ? 1 : -1;
		// check to the queen side
		Square to = Square.fromInts(from.file - 1, from.rank + pawnDisplacement);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		// check to the king side
		to = Square.fromInts(from.file + 1, from.rank + pawnDisplacement);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
	}

	private void initSquaresInCheckByKnight(Square knightSquare, boolean isCheckedByWhite) {
		Square to = Square.fromInts(knightSquare.file + 1, knightSquare.rank + 2);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file + 1, knightSquare.rank - 2);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 1, knightSquare.rank + 2);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 1, knightSquare.rank - 2);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file + 2, knightSquare.rank + 1);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file + 2, knightSquare.rank - 1);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 2, knightSquare.rank + 1);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 2, knightSquare.rank - 1);
		if (to != null) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
	}

	private void incrementChecksOnSquare(Square square, boolean isCheckedByWhite) {
		short contentAsShort = board[square.ordinal()];
		byte bitOffset = isCheckedByWhite ? SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET : SquareFormat.CHECKS_BY_BLACK_BIT_OFFSET;
		short checksCount = (short) ((contentAsShort >>> bitOffset) & SquareFormat.CHECKS_COUNT_MASK);
		checksCount++;
		checksCount <<= bitOffset;

		short resettingMask = (short) ~(SquareFormat.CHECKS_COUNT_MASK << bitOffset);
		contentAsShort = (short) (contentAsShort & resettingMask);

		board[square.ordinal()] = (short) (contentAsShort | checksCount);
	}

	private void setNoKingFlagOnSquare(Square square) {
		short contentAsShort = board[square.ordinal()];
		board[square.ordinal()] = (byte) (contentAsShort | SquareFormat.NO_KINGS_FLAG);
	}

	boolean isSquareCheckedBy(Square square, boolean testChecksByWhite) {
		return getChecksCount(square, testChecksByWhite) > 0;
	}

	private byte getChecksCount(Square square, boolean checksByWhite) {
		byte bitOffset = checksByWhite ? SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET : SquareFormat.CHECKS_BY_BLACK_BIT_OFFSET;
		return (byte) ((board[square.ordinal()] >> bitOffset) & SquareFormat.CHECKS_COUNT_MASK);
	}

	private boolean isSquareOkForKing(Square square, boolean isKingWhite) {
		return !isSquareCheckedBy(square, !isKingWhite) && (board[square.ordinal()] & SquareFormat.NO_KINGS_FLAG) == 0;
	}

	public State chooseMove(int moveIndex) {
		return generateLegalMoves().get(moveIndex);
	}

	public int countLegalMoves() {
		if (isKingInCheck()) {
			Square checkedKing = test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing();
			return generateLegalMovesWhenKingInCheck(null, checkedKing);
		}
		return generatePseudoLegalMoves(null);
	}

	public List<State> generateLegalMoves() {
		assert isLegal() : "King is still left in check after previous move!\n" + this;
		List<State> moves = new ArrayList<>(Config.DEFAULT_MOVES_LIST_CAPACITY);
		try {

			if (isKingInCheck()) {
				Square checkedKing = test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing();
				generateLegalMovesWhenKingInCheck(moves, checkedKing);
				return moves;
			}

			generatePseudoLegalMoves(moves);
		} catch (AssertionError ae) {
			System.out.println("------------------FAILED ASSERTION IN MOVE GENERATION----------------------------");
			System.out.println(" STATE: " + this);
			throw ae;
		}
		return moves;
	}

	boolean isKingInCheck() {
		return isSquareCheckedBy(test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing(), !test(WHITE_TURN));
	}

	/**
	 * 	 Generates pseudo-legal moves. Takes into consideration absolute pins. Moves generated by this method are legal
	 * 	 * 	 provided that the king of the side taking turn was not in check.
	 * @param ouputMoves - leave this null to skip generation and just count
	 * @return number of output moves
	 */
	private int generatePseudoLegalMoves(List<State> ouputMoves) {
		int movesCount = 0;
		Square[] piecesOfOneType = test(WHITE_TURN) ? pieces.whitePawns : pieces.blackPawns;
		byte piecesCount = test(WHITE_TURN) ? pieces.whitePawnsCount : pieces.blackPawnsCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalPawnMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteKnights : pieces.blackKnights;
		piecesCount = test(WHITE_TURN) ? pieces.whiteKnightsCount : pieces.blackKnightsCount;
		for (byte i = 0; i < piecesCount; i++) {
				movesCount += generatePseudoLegalKnightMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteBishops : pieces.blackBishops;
		piecesCount = test(WHITE_TURN) ? pieces.whiteBishopsCount : pieces.blackBishopsCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalBishopMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteRooks : pieces.blackRooks;
		piecesCount = test(WHITE_TURN) ? pieces.whiteRooksCount : pieces.blackRooksCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalRookMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteQueens : pieces.blackQueens;
		piecesCount = test(WHITE_TURN) ? pieces.whiteQueensCount : pieces.blackQueensCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalQueenMoves(piecesOfOneType[i], ouputMoves);
		}
		movesCount += generateLegalKingMoves(test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing(),
				ouputMoves);
		return movesCount;
	}

	private int generateLegalKingMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		Square to = Square.fromInts(from.file, from.rank + 1);
		boolean isWhiteTurn = test(WHITE_TURN);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		int kingMovedBitflag = isWhiteTurn ? WHITE_KING_MOVED : BLACK_KING_MOVED;

		if (!test(kingMovedBitflag) && !isSquareCheckedBy(from, !isWhiteTurn)) {
			Content rook = isWhiteTurn ? Content.WHITE_ROOK : Content.BLACK_ROOK;
			int qsRookMovedBitflag = isWhiteTurn ? WHITE_QS_ROOK_MOVED : BLACK_QS_ROOK_MOVED;
			int ksRookMovedBitflag = isWhiteTurn ? WHITE_KS_ROOK_MOVED : BLACK_KS_ROOK_MOVED;
			Square kingQsTo = isWhiteTurn ? Square.C1 : Square.C8;
			Square kingKsTo = isWhiteTurn ? Square.G1 : Square.G8;
			Square qsRook = isWhiteTurn ? Square.A1 : Square.A8;
			Square ksRook = isWhiteTurn ? Square.H1 : Square.H8;

			if (squaresOkForQsCastling(isWhiteTurn) && getContent(qsRook) == rook && !test(qsRookMovedBitflag)) {
				if (outputMoves != null) {
					outputMoves.add(fromLegalQueensideCastling(from, kingQsTo));
				} else {
					movesCount++;
				}
			}
			if (squaresOkForKsCastling(isWhiteTurn) && getContent(ksRook) == rook && !test(ksRookMovedBitflag)) {
				if (outputMoves != null) {
					outputMoves.add(fromLegalKingsideCastling(from, kingKsTo));
				} else {
					movesCount++;
				}
			}
		}
		return movesCount;
	}

	private boolean squaresOkForQsCastling(boolean isWhiteKingCastling) {
		for (int file = File.D; file >= File.C; file--) {
			Square square = Square.fromLegalInts(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(square) != Content.EMPTY || isSquareCheckedBy(square, !isWhiteKingCastling)) {
				return false;
			}
		}
		return getContent(isWhiteKingCastling ? B1 : Square.B8) == Content.EMPTY;
	}

	private boolean squaresOkForKsCastling(boolean isWhiteKingCastling) {
		for (int file = File.F; file <= File.G; file++) {
			Square square = Square.fromLegalInts(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(square) != Content.EMPTY || isSquareCheckedBy(square, !isWhiteKingCastling)) {
				return false;
			}
		}
		return true;
	}

	private int generatePseudoLegalQueenMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generatePseudoLegalRookMoves(from, outputMoves);
		movesCount += generatePseudoLegalBishopMoves(from, outputMoves);
		return movesCount;
	}

	private int generatePseudoLegalRookMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generateSlidingPieceMoves(from, 1, 0, outputMoves);
		movesCount += generateSlidingPieceMoves(from, -1, 0, outputMoves);
		movesCount += generateSlidingPieceMoves(from, 0, 1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, 0, -1, outputMoves);
		return movesCount;
	}

	private int generatePseudoLegalBishopMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generateSlidingPieceMoves(from, 1, 1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, 1, -1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, -1, 1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, -1, -1, outputMoves);
		return movesCount;
	}

	private int generateSlidingPieceMoves(Square from, int deltaFile, int deltaRank, List<State> outputMoves) {
		int movesCount = 0;
		if (pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, deltaRank))) {
			for (int i = 1; true; i++) {
				Square to = Square.fromInts(from.file + deltaFile * i, from.rank + i * deltaRank);
				if (to == null || isSameColorPieceOn(to)) {
					break;
				}
				movesCount = createOrCountMove(from, to, outputMoves, movesCount);
				if (isOppositeColorPieceOn(to)) {
					break;
				}
			}
		}
		return movesCount;
	}

	private int generatePseudoLegalPawnMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		int pawnDisplacement = test(WHITE_TURN) ? 1 : -1;
		int pawnDoubleDisplacement = test(WHITE_TURN) ? 2 : -2;
		Square to = Square.fromLegalInts(from.file, from.rank + pawnDisplacement);
		// head-on move
		if (getContent(to) == Content.EMPTY && pieceIsFreeToMove(from, Pin.FILE)) {
			if (isPromotingSquare(to)) {
				movesCount += generatePromotionMoves(from, to, outputMoves);
			} else {
				movesCount = createOrCountMove(from, to, outputMoves, movesCount);
				to = Square.fromLegalInts(from.file, from.rank + pawnDoubleDisplacement);
				if (isInitialSquareOfPawn(from) && getContent(to) == Content.EMPTY) {
					if (outputMoves != null) {
						outputMoves.add(fromPseudoLegalPawnDoublePush(from, to, Square.fromLegalInts(from.file, from.rank + pawnDisplacement)));
					} else {
						movesCount++;
					}
				}
			}
		}
		// move with take to the queen-side
		int deltaFile = -1;
		to = Square.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);

		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantSquare)
				&& pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isPromotingSquare(to)) {
				movesCount += generatePromotionMoves(from, to, outputMoves);
			} else {
				movesCount = createOrCountMove(from, to, outputMoves, movesCount);
			}
		}
		// move with take to the king side
		deltaFile = 1;
		to = Square.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);
		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantSquare)
				&& pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isPromotingSquare(to)) {
				movesCount += generatePromotionMoves(from, to, outputMoves);
			} else {
				movesCount = createOrCountMove(from, to, outputMoves, movesCount);
			}
		}
		return movesCount;
	}

	private int generatePromotionMoves(Square from, Square to, List<State> outputMoves) {
		if (outputMoves == null) {
			return 4;
		}
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_QUEEN : Content.BLACK_QUEEN));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_ROOK : Content.BLACK_QUEEN));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_BISHOP : Content.BLACK_QUEEN));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_KNIGHT : Content.BLACK_QUEEN));
		return 4;
	}

	private int generatePseudoLegalKnightMoves(Square from, List<State> outputMoves) {
		if (!pieceIsFreeToMove(from, null)) {
			return 0;
		}
		int movesCount = 0;
		Square to = Square.fromInts(from.file + 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		return movesCount;
	}

	private int createOrCountMove(Square from, Square to, List<State> outputMoves, int movesCount) {
		if (outputMoves != null) {
			outputMoves.add(fromPseudoLegalMove(from, to));
			return 0;
		}
		return movesCount + 1;
	}

	/**
	 * Returns true if piece at pieceLocation can move along the movementDirection line (is not absolutely pinned).
	 * @param movementDirection - leave this null in case of knight at pieceLocation
	 */
	private boolean pieceIsFreeToMove(Square pieceLocation, Pin movementDirection) {
		Pin pin = pinnedPieces[pieceLocation.ordinal()];
		return pin == null || pin == movementDirection;
	}

	private int generateLegalMovesWhenKingInCheck(List<State> outputMoves, Square checkedKing) {
		int movesCount = 0;
		if (getChecksCount(checkedKing, !test(WHITE_TURN)) < 2) {
			List<State> pseudoLegalMoves = new ArrayList<>(Config.DEFAULT_MOVES_LIST_CAPACITY);
			generatePseudoLegalMoves(pseudoLegalMoves);
			for (State pseudoLegalState : pseudoLegalMoves) {
				if (pseudoLegalState.isLegal()) {
					if (outputMoves != null) {
						outputMoves.add(pseudoLegalState);
					} else {
						movesCount++;
					}
				}
			}
		} else {
			movesCount = generateLegalKingMoves(checkedKing, outputMoves);
		}
		return movesCount;
	}

	/**
	 * Is king left in check after move
	 */
	private boolean isLegal() {
		boolean isWhiteTurn = test(WHITE_TURN);
		Square king = isWhiteTurn ? pieces.getBlackKing() : pieces.getWhiteKing();
		return !isSquareCheckedBy(king, isWhiteTurn);
	}
}
