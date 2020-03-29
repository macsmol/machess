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
	 * bbbbwwww--BWcppp
	 *
	 * p - piece type
	 * c - is piece white flag
	 * W - check by white king
	 * B - check by black king
	 * w - checks by white count
	 * b - checks by black count
	 */
	public static final class SquareFormat {
		static final short PIECE_TYPE_MASK 		= 0x07;
		static final short IS_WHITE_PIECE_FLAG 	= 0x08;

		static final short CHECK_BY_WHITE_KING 	= 0x10;
		static final short CHECK_BY_BLACK_KING 	= 0x20;

		// mask to get checks count by black or white. Four MSBits are checks by black, next 4 bits are checks by white.
		static final short CHECKS_COUNT_MASK 	= 0x0F;
		static final byte CHECKS_BY_WHITE_BIT_OFFSET = 8;
		static final byte CHECKS_BY_BLACK_BIT_OFFSET = 12;
	}

	// flags
	private byte flags;
	public static final int WHITE_TURN					= 0x01;
	public static final int WHITE_KS_CASTLE_POSSIBLE	= 0x02;
	public static final int WHITE_QS_CASTLE_POSSIBLE 	= 0x04;
	public static final int BLACK_KS_CASTLE_POSSIBLE 	= 0x08;
	public static final int BLACK_QS_CASTLE_POSSIBLE 	= 0x10;

	/**
	 * one byte per square.
	 */
	final short[] board;

	final PieceLists pieces;
	/**
	 * If not null it means there is a possibility to en-passant on this square
	 */
	private final Square enPassantSquare;

	// TODO 50 move draw rule
	private final byte halfmoveClock;

	// not really necessary - only for debug purposes
	private final int fullMoveCounter;

	/**
	 * Board with absolutely pinned pieces. It's indexed by Square.ordinal()
	 */
	private final Pin[] pinnedPieces;


	private Square from;
	private Square to;

	/**
	 * new game
	 */
	State() {
		flags = WHITE_TURN |
				WHITE_QS_CASTLE_POSSIBLE | WHITE_KS_CASTLE_POSSIBLE |
				BLACK_QS_CASTLE_POSSIBLE | BLACK_KS_CASTLE_POSSIBLE;
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
		halfmoveClock = 0;
		fullMoveCounter = 1;

		pinnedPieces = new Pin[Square.values().length];
		initChecksAroundKings();
	}

	public State(short[] board, PieceLists pieces, byte flags,
		  @Nullable Square enPassantSquare, byte halfmoveClock, int fullMoveCounter, Square from, Square to) {
		this.board = board;
		this.pieces = pieces;
		this.flags = flags;
		this.enPassantSquare = enPassantSquare;
		this.halfmoveClock = halfmoveClock;
		this.fullMoveCounter = fullMoveCounter;
		this.from = from;
		this.to = to;
		pieces.sortOccupiedSquares();

		resetSquaresInCheck();
		initChecksAroundKings();

		pinnedPieces = new Pin[Square.values().length];
		initPinnedPieces();
	}

	State fromPseudoLegalPawnDoublePush(Square from, Square to, Square enPassantSquare) {
		assert enPassantSquare != null;
		if (!isEnPassantLegal(to)) {
			enPassantSquare = null;
		}
		return fromPseudoLegalMove(from, to, null, enPassantSquare, null);
	}

	/**
	 * En passant can sometimes be illegal due to an absolute pin, eg.
	 * 8/8/8/8/RPpk4/8/8/4K3 b - b3 0 1
	 * Program detects such situation before double-push is made
	 */
	private boolean isEnPassantLegal(Square doublePushTo) {
		Square king = doublePushTo.rank == Rank._4 ? pieces.getBlackKing() : pieces.getWhiteKing();
		if (king.rank != doublePushTo.rank) {
			return true;
		}
		Square[] rooks = doublePushTo.rank == Rank._4 ? pieces.whiteRooks : pieces.blackRooks;
		int rooksCount = doublePushTo.rank == Rank._4 ? pieces.whiteRooksCount : pieces.blackRooksCount;
		if (!isEnPassantLegal(king, rooks, rooksCount)) {
			return false;
		}
		Square[] queens = doublePushTo.rank == Rank._4 ? pieces.whiteQueens : pieces.blackQueens;
		int queensCount = doublePushTo.rank == Rank._4 ? pieces.whiteQueensCount : pieces.blackQueensCount;
		return isEnPassantLegal(king, queens, queensCount);
	}

	private boolean isEnPassantLegal(Square king, Square[] rooklikes, int rooklikesCount) {
		for (int i = 0; i < rooklikesCount; i++) {
			if (rooklikes[i].rank != king.rank) {
				continue;
			}
			int fileFrom = Math.min(king.file, rooklikes[i].file) + 1;
			int fileTo = Math.max(king.file, rooklikes[i].file);
			int piecesBetweenCount = 0;
			for (int file = fileFrom; file < fileTo; file++) {
				if (getContent(file, king.rank) != Content.EMPTY) {
					piecesBetweenCount++;
				}
			}
			if (piecesBetweenCount == 1) {
				return false;
			}
		}
		return true;
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
			flagsCopy &= ~(WHITE_KS_CASTLE_POSSIBLE | WHITE_QS_CASTLE_POSSIBLE);
		} else if (from == Square.E8) {
			flagsCopy &= ~(BLACK_KS_CASTLE_POSSIBLE | BLACK_QS_CASTLE_POSSIBLE);
		} else if (from == Square.A1) {
			flagsCopy &= ~WHITE_QS_CASTLE_POSSIBLE;
		} else if (from == Square.H1) {
			flagsCopy &= ~WHITE_KS_CASTLE_POSSIBLE;
		} else if (from == Square.A8) {
			flagsCopy &= ~BLACK_QS_CASTLE_POSSIBLE;
		} else if (from == Square.H8) {
			flagsCopy &= ~BLACK_KS_CASTLE_POSSIBLE;
		}
		if (to == A1) {
			flagsCopy &= ~WHITE_QS_CASTLE_POSSIBLE;
		} else if (to == H1) {
			flagsCopy &= ~WHITE_KS_CASTLE_POSSIBLE;
		} else if (to == A8) {
			flagsCopy &= ~BLACK_QS_CASTLE_POSSIBLE;
		} else if (to == H8) {
			flagsCopy &= ~BLACK_KS_CASTLE_POSSIBLE;
		}

		int newFullMoveClock = test(WHITE_TURN) ? fullMoveCounter : fullMoveCounter + 1;
		return new State(boardCopy, piecesCopy, (byte) flagsCopy, futureEnPassantSquare, (byte)0, newFullMoveClock,
				from, to);
	}

	boolean test(int flagMask) {
		return (flags & flagMask) != 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: ").append(test(WHITE_TURN) ? "WHITE" : "BLACK");

		sb.append(test(WHITE_KS_CASTLE_POSSIBLE) ? "; WHITE_KS_CASTLE_POSSIBLE" : "");
		sb.append(test(WHITE_QS_CASTLE_POSSIBLE) ? "; WHITE_QS_CASTLE_POSSIBLE" : "");
		sb.append(test(BLACK_KS_CASTLE_POSSIBLE) ? "; BLACK_KS_CASTLE_POSSIBLE" : "");
		sb.append(test(BLACK_QS_CASTLE_POSSIBLE) ? "; BLACK_QS_CASTLE_POSSIBLE" : "");
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
				Square square = Square.fromLegalInts(file, rank);
				Content content = getContent(file, rank);
				sb.append(content.symbol);
				if (square == to || square == from) {
					sb.append(" <");
				} else {
					sb.append(" |");
				}

				if (Config.DEBUG_FIELD_IN_CHECK_FLAGS) {
					short contentAsShort = board[square.ordinal()];
					sbCheckFlags.append(Utils.checkCountsToString(contentAsShort)).append('|');
				}
				if (Config.DEBUG_PINNED_PIECES) {
					Pin pinType = pinnedPieces[square.ordinal()];
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
		sb.append("fullmoveClock: ").append(fullMoveCounter).append('\n');
		return sb.toString();
	}

	public String printMove() {
		return "" + from + to;
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
	 * Tells if Square square is occupied by a piece of color that's currently taking turn.
	 */
	boolean isOppositeColorPieceOn(Square square) {
		return test(WHITE_TURN) ? isBlackPieceOn(square) : isWhitePieceOn(square);
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

	private void initChecksAroundKings() {
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
		initPinsByBishops(king, isPinnedToWhiteKing);
		initPinsByRooks(king, isPinnedToWhiteKing);
		initPinsByQueens(king, isPinnedToWhiteKing);
	}

	private void initPinsByBishops(Square king, boolean isPinnedToWhiteKing) {
		Square[] bishops = isPinnedToWhiteKing ? pieces.blackBishops : pieces.whiteBishops;
		int bishopsCount = isPinnedToWhiteKing ? pieces.blackBishopsCount : pieces.whiteBishopsCount;

		for (int i = 0; i < bishopsCount; i++) {
			int deltaRank = bishops[i].rank - king.rank;
			int deltaFile = bishops[i].file - king.file;

			if (Math.abs(deltaFile) == Math.abs(deltaRank)) {
				initPinByBishop(king, isPinnedToWhiteKing, bishops[i]);
			}
		}
	}
	
	private void initPinsByRooks(Square king, boolean isPinnedToWhiteKing) {
		Square[] rooks = isPinnedToWhiteKing ? pieces.blackRooks : pieces.whiteRooks;
		int rooksCount = isPinnedToWhiteKing ? pieces.blackRooksCount : pieces.whiteRooksCount;

		for (int i = 0; i < rooksCount; i++) {
			if (king.rank == rooks[i].rank) {
				initRankPin(king, isPinnedToWhiteKing, rooks[i]);
			} else if (king.file == rooks[i].file) {
				initFilePin(king, isPinnedToWhiteKing, rooks[i]);
			}
		}
	}

	private void initPinsByQueens(Square king, boolean isPinnedToWhiteKing) {
		Square[] queens = isPinnedToWhiteKing ? pieces.blackQueens : pieces.whiteQueens;
		int queensCount = isPinnedToWhiteKing ? pieces.blackQueensCount : pieces.whiteQueensCount;

		for (int i = 0; i < queensCount; i++) {
			int deltaRank = queens[i].rank - king.rank;
			int deltaFile = queens[i].file - king.file;

			if (Math.abs(deltaFile) == Math.abs(deltaRank)) {
				initPinByBishop(king, isPinnedToWhiteKing, queens[i]);
			} else if (king.rank == queens[i].rank) {
				initRankPin(king, isPinnedToWhiteKing, queens[i]);
			} else if (king.file == queens[i].file) {
				initFilePin(king, isPinnedToWhiteKing, queens[i]);
			}
		}
	}

	private void initPinByBishop(Square king, boolean isKingWhite, Square bishoplike) {
		int fileDirection = bishoplike.file - king.file > 0 ? 1 : -1;
		int rankDirection = bishoplike.rank - king.rank > 0 ? 1 : -1;
		initPin(king, isKingWhite, bishoplike, fileDirection, rankDirection);
	}

	private void initRankPin(Square king, boolean isKingWhite, Square rooklike) {
		int fileDirection = rooklike.file - king.file > 0 ? 1 : -1;
		int rankDirection = 0;
		initPin(king, isKingWhite, rooklike, fileDirection, rankDirection);
	}

	private void initFilePin(Square king, boolean isKingWhite, Square rooklike) {
		int fileDirection = 0;
		int rankDirection = rooklike.rank - king.rank > 0 ? 1 : -1;
		initPin(king, isKingWhite, rooklike, fileDirection, rankDirection);
	}

	private void initPin(Square king, boolean isKingWhite, Square slidingPiece, int deltaFile, int deltaRank) {
		Square candidate = null;
		for (int i = 1; true; i++) {
			Square testedSquare = Square.fromInts(king.file + i * deltaFile, king.rank + i * deltaRank);
			if (testedSquare == slidingPiece) {
				if (candidate != null) {
					pinnedPieces[candidate.ordinal()] = Pin.fromDeltas(deltaFile, deltaRank);
				}
				return;
			}
			Content content = getContent(testedSquare);
			if (content != Content.EMPTY) {
				if (content.isWhite != isKingWhite) {
					return;
				}
				if (candidate == null && content.isWhite == isKingWhite) {
					candidate = testedSquare;
				} else {
					// found second piece obstructing ray
					return;
				}
			}
		}
	}

	private void initChecksAroundKing(boolean isCheckedByWhite) {
		Square king = isCheckedByWhite ? pieces.getBlackKing() : pieces.getWhiteKing();
		boolean castlingImpossible;
		if (isCheckedByWhite) {
			castlingImpossible = !(test(BLACK_KS_CASTLE_POSSIBLE) || test(BLACK_QS_CASTLE_POSSIBLE));
		} else {
			castlingImpossible = !(test(WHITE_KS_CASTLE_POSSIBLE) || test(WHITE_QS_CASTLE_POSSIBLE));
		}

		if (isCheckedByWhite) {
			initChecksByPawns(king, castlingImpossible, isCheckedByWhite, pieces.whitePawns, pieces.whitePawnsCount);
			initChecksByKnights(king, castlingImpossible, isCheckedByWhite, pieces.whiteKnights, pieces.whiteKnightsCount);
			initChecksByBishops(king, isCheckedByWhite, pieces.whiteBishops, pieces.whiteBishopsCount);
			initChecksByRooks(king, isCheckedByWhite, pieces.whiteRooks, pieces.whiteRooksCount);
			initChecksByQueens(king, isCheckedByWhite, pieces.whiteQueens, pieces.whiteQueensCount);
		} else {
			initChecksByPawns(king, castlingImpossible, isCheckedByWhite, pieces.blackPawns, pieces.blackPawnsCount);
			initChecksByKnights(king, castlingImpossible, isCheckedByWhite, pieces.blackKnights, pieces.blackKnightsCount);
			initChecksByBishops(king, isCheckedByWhite, pieces.blackBishops, pieces.blackBishopsCount);
			initChecksByRooks(king, isCheckedByWhite, pieces.blackRooks, pieces.blackRooksCount);
			initChecksByQueens(king, isCheckedByWhite, pieces.blackQueens, pieces.blackQueensCount);
		}
	}

	private void initChecksByPawns(Square king, boolean castlingImpossible, boolean isCheckedByWhite,
								   Square[] pawns, byte pawnsCount) {
		if (pawnsCount == 0) {
			return;
		}
		Arrays.sort(pawns, 0, pawnsCount, Comparator.comparingInt(sq -> sq.file));

		byte minKingSafeDistance = (byte)(castlingImpossible ? 2 : 3);
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

	private void initChecksByKnights(Square king, boolean castlingImpossible, boolean isCheckedByWhite, Square[] knights,
			byte knightsCount) {
		byte minKingSafeDistance = (byte)(castlingImpossible ? 4 : 5);
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

		initSquareInCheckByKing(whiteKing, WHITE);
		initSquareInCheckByKing(blackKing, BLACK);
	}

	private void initSquareInCheckByKing(Square king, boolean isKingWhite) {
		short checkFlag = isKingWhite ? SquareFormat.CHECK_BY_WHITE_KING : SquareFormat.CHECK_BY_BLACK_KING;
		Square to = Square.fromInts(king.file, king.rank + 1);
		if (to != null) {
			setFlag(to, checkFlag);
		}
		to = Square.fromInts(king.file + 1, king.rank + 1);
		if (to != null) {
			setFlag(to, checkFlag);
		}
		to = Square.fromInts(king.file + 1, king.rank);
		if (to != null) {
			setFlag(to, checkFlag);
		}
		to = Square.fromInts(king.file + 1, king.rank - 1);
		if (to != null) {
			setFlag(to, checkFlag);
		}
		to = Square.fromInts(king.file, king.rank - 1);
		if (to != null) {
			setFlag(to, checkFlag);
		}
		to = Square.fromInts(king.file - 1, king.rank - 1);
		if (to != null) {
			setFlag(to, checkFlag);
		}
		to = Square.fromInts(king.file - 1, king.rank);
		if (to != null) {
			setFlag(to, checkFlag);
		}
		to = Square.fromInts(king.file - 1, king.rank + 1);
		if (to != null) {
			setFlag(to, checkFlag);
		}
	}

	private void initCheckFlagsBySlidingPiece(Square from, boolean isCheckedByWhite, int deltaFile, int deltaRank) {
		for (int i = 1; true; i++) {
			Square underCheck = Square.fromInts(from.file + deltaFile * i, from.rank + i * deltaRank);
			if (underCheck == null) {
				break;
			}
			incrementChecksOnSquare(underCheck, isCheckedByWhite);
			if (isSquareBlockingSlidingPiece(underCheck, isCheckedByWhite)) {
				break;
			}
		}
	}

	private boolean isSquareBlockingSlidingPiece(Square square, boolean isSlidingPieceWhite) {
		byte contentAsByte =  (byte)(board[square.ordinal()]
				& (SquareFormat.PIECE_TYPE_MASK | SquareFormat.IS_WHITE_PIECE_FLAG));
		if (contentAsByte == Content.EMPTY.asByte) {
			return false;
		}
		byte enemyKing = isSlidingPieceWhite ? Content.BLACK_KING.asByte : Content.WHITE_KING.asByte;
		return contentAsByte != enemyKing;
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

	private void setFlag(Square square, short flag) {
		short contentAsShort = board[square.ordinal()];
		board[square.ordinal()] = (short) (contentAsShort | flag);
	}

	boolean isSquareCheckedBy(Square square, boolean testChecksByWhite) {
		return getChecksCount(square, testChecksByWhite) > 0;
	}

	private byte getChecksCount(Square square, boolean checksByWhite) {
		byte bitOffset = checksByWhite ? SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET : SquareFormat.CHECKS_BY_BLACK_BIT_OFFSET;
		return (byte) ((board[square.ordinal()] >> bitOffset) & SquareFormat.CHECKS_COUNT_MASK);
	}

	private boolean canKingWalkOnSquare(Square square, boolean isKingWhite) {
		short checkedByKingFlag = isKingWhite ? SquareFormat.CHECK_BY_BLACK_KING : SquareFormat.CHECK_BY_WHITE_KING;
		return !isSquareCheckedBy(square, !isKingWhite) && (board[square.ordinal()] & checkedByKingFlag) == 0;
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

	public int countOtherSideLegalMoves() {
		makeNullMove();
		int legalMoves = countLegalMoves();
		makeNullMove();
		return legalMoves;
	}

	private void makeNullMove() {
		flags ^= WHITE_TURN;
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
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}

		int qsCastlePossible = isWhiteTurn ? WHITE_QS_CASTLE_POSSIBLE : BLACK_QS_CASTLE_POSSIBLE;
		int ksCastlePossible = isWhiteTurn ? WHITE_KS_CASTLE_POSSIBLE : BLACK_KS_CASTLE_POSSIBLE;

		if (!isSquareCheckedBy(from, !isWhiteTurn)) {
			Square kingQsTo = isWhiteTurn ? Square.C1 : Square.C8;
			Square kingKsTo = isWhiteTurn ? Square.G1 : Square.G8;

			if (test(qsCastlePossible) && squaresOkForQsCastling(isWhiteTurn)) {
				if (outputMoves != null) {
					outputMoves.add(fromLegalQueensideCastling(from, kingQsTo));
				} else {
					movesCount++;
				}
			}
			if (test(ksCastlePossible) && squaresOkForKsCastling(isWhiteTurn)) {
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
			if (getContent(square) != Content.EMPTY || !canKingWalkOnSquare(square, isWhiteKingCastling)) {
				return false;
			}
		}
		return getContent(isWhiteKingCastling ? B1 : Square.B8) == Content.EMPTY;
	}

	private boolean squaresOkForKsCastling(boolean isWhiteKingCastling) {
		for (int file = File.F; file <= File.G; file++) {
			Square square = Square.fromLegalInts(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(square) != Content.EMPTY || !canKingWalkOnSquare(square, isWhiteKingCastling)) {
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
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_ROOK : Content.BLACK_ROOK));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_BISHOP : Content.BLACK_BISHOP));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_KNIGHT : Content.BLACK_KNIGHT));
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
