package machess;

import com.sun.istack.internal.Nullable;

import java.util.*;

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
		private static final byte CHECKS_COUNT_MASK = 0x0F;
		private static final byte CHECKS_BY_WHITE_BIT_OFFSET = 8;
		private static final byte CHECKS_BY_BLACK_BIT_OFFSET = 12;
	}

	private static final int INITIAL_PLAYER_PIECES_COUNT = 16;

	// flags
	private final byte flags;
	private static final int WHITE_TURN =           0x01;
	private static final int WHITE_KING_MOVED =     0x02;
	private static final int BLACK_KING_MOVED =     0x04;
	private static final int WHITE_KS_ROOK_MOVED =  0x08;
	private static final int WHITE_QS_ROOK_MOVED =  0x10;
	private static final int BLACK_KS_ROOK_MOVED =  0x20;
	private static final int BLACK_QS_ROOK_MOVED =  0x40;

	/**
	 * one byte per square.
	 */
	private final short[] board;

	// king, queen, rooks and knights, pawns
	private final Square[] squaresWithWhites;
	private final byte whitesCount;

	private final Square[] squaresWithBlacks;
	private final byte blacksCount;
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

	private Square kingAttacker;

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
				case 0:
				case 7:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_ROOK.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_ROOK.asByte;
					break;
				case 1:
				case 6:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_KNIGHT.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_KNIGHT.asByte;
					break;
				case 2:
				case 5:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_BISHOP.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_BISHOP.asByte;
					break;
				case 3:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_QUEEN.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_QUEEN.asByte;
					break;
				case 4:
					board[Square.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_KING.asByte;
					board[Square.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_KING.asByte;
					break;
			}
		}
		squaresWithWhites = new Square[]{
				Square.E1, Square.D1, Square.A1, Square.H1, Square.B1, Square.C1, Square.F1, Square.G1,
				Square.A2, Square.B2, Square.C2, Square.D2, Square.E2, Square.F2, Square.G2, Square.H2
		};
		whitesCount = INITIAL_PLAYER_PIECES_COUNT;

		squaresWithBlacks = new Square[]{
				Square.E8, Square.D8, Square.A8, Square.H8, Square.B8, Square.C8, Square.F8, Square.G8,
				Square.A7, Square.B7, Square.C7, Square.D7, Square.E7, Square.F7, Square.G7, Square.H7
		};
		blacksCount = INITIAL_PLAYER_PIECES_COUNT;
		enPassantSquare = null;
		plyNumber = 1;

		pinnedPieces = new Pin[Square.values().length];
		initSquaresInCheck();
	}

	private State(short[] board, Square[] squaresWithWhites, byte whitesCount, Square[] squaresWithBlacks, byte blacksCount,
				  byte flags, @Nullable Square enPassantSquare, int plyNumber) {
		this.board = board;
		this.squaresWithWhites = squaresWithWhites;
		this.squaresWithBlacks = squaresWithBlacks;
		this.whitesCount = whitesCount;
		this.blacksCount = blacksCount;
		this.flags = flags;
		this.enPassantSquare = enPassantSquare;
		this.plyNumber = plyNumber;
		resetSquaresInCheck();
		initSquaresInCheck();

		pinnedPieces = new Pin[Square.values().length];
		initPinnedPieces();
	}

	private State fromLegalPawnFirstMove(Square from, Square to, Square enPassantSquare) {
		assert enPassantSquare != null;
		return fromLegalMove(from, to, null, enPassantSquare, null);
	}

	private State fromLegalMoveWithPromotion(Square from, Square to, Content promotion) {
		assert promotion != null;
		return fromLegalMove(from, to, promotion, null, null);
	}

	private State fromLegalQueensideCastling(Square kingFrom, Square kingTo) {
		Square rookToCastle = Square.fromLegalInts(File.A, kingFrom.rank);
		return fromLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}

	private State fromLegalKingsideCastling(Square kingFrom, Square kingTo) {
		Square rookToCastle = Square.fromLegalInts(File.H, kingFrom.rank);
		return fromLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}

	/**
	 * Generates new BoardState based on move. Typical move without special events.
	 */
	State fromLegalMove(Square from, Square to) {
		return fromLegalMove(from, to, null, null, null);
	}

	/**
	 * Generates new BoardState based on move. It does not verify game rules - assumes input is a legal move.
	 * This is the root method - it covers all cases. All 'overload' methods should call this one.
	 */
	private State fromLegalMove(Square from, Square to, @Nullable Content promotion, @Nullable Square futureEnPassantSquare,
								@Nullable Square rookToCastle) {
		assert from != to : from + "->" + to + " is no move";
		short[] boardCopy = board.clone();
		Square[] squaresWithWhitesCopy = squaresWithWhites.clone();
		Square[] squaresWithBlacksCopy = squaresWithBlacks.clone();

		//  update boardCopy
		Content movedPiece = Content.fromShort(boardCopy[from.ordinal()]);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		assert movedPiece.isWhite == test(WHITE_TURN) : "Moved " + movedPiece + " on " + (test(WHITE_TURN) ? "white" : "black" ) + " turn";
		boardCopy[from.ordinal()] = Content.EMPTY.asByte;

		Content takenPiece = Content.fromShort(boardCopy[to.ordinal()]);
		assert takenPiece != Content.BLACK_KING && takenPiece != Content.WHITE_KING : from + "->" + to + " is taking king";
		boardCopy[to.ordinal()] = promotion != null ? promotion.asByte : movedPiece.asByte;

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
		} else if (rookToCastle != null) {
			boardCopy[rookToCastle.ordinal()] = Content.EMPTY.asByte;
			byte rookAsByte = test(WHITE_TURN) ? Content.WHITE_ROOK.asByte : Content.BLACK_ROOK.asByte;
			Square rookDestination;
			if (rookToCastle.file == 0) {
				rookDestination = test(WHITE_TURN) ? Square.D1 : Square.D8;
				boardCopy[rookDestination.ordinal()] = rookAsByte;
			} else {
				rookDestination = test(WHITE_TURN) ? Square.F1 : Square.F8;
				boardCopy[rookDestination.ordinal()] = rookAsByte;
			}
			// update pieces lists
			movePieceOnPiecesLists(squaresWithWhitesCopy, squaresWithBlacksCopy, rookToCastle, rookDestination);
		}

		// update pieces lists
		movePieceOnPiecesLists(squaresWithWhitesCopy, squaresWithBlacksCopy, from, to);

		Square[] takenPieces = test(WHITE_TURN) ? squaresWithBlacksCopy : squaresWithWhitesCopy;
		byte takenPiecesCount = test(WHITE_TURN) ? blacksCount : whitesCount;

		if (takenPiece != Content.EMPTY) {
			assert movedPiece.isWhite != takenPiece.isWhite : from + "->" + to + " is a friendly take";
			for (int i = 0; i < takenPiecesCount; i++) {
				if (takenPieces[i] == to || takenPieces[i] == squareWithPawnTakenEnPassant) {
					// decrement size of alive pieces
					takenPiecesCount--;
					takenPieces[i] = takenPieces[takenPiecesCount];
					break;
				}
			}
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

		byte updatedWhitesCount = test(WHITE_TURN) ? whitesCount : takenPiecesCount;
		byte updatedBlacksCount = test(WHITE_TURN) ? takenPiecesCount : blacksCount;
		return new State(boardCopy, squaresWithWhitesCopy, updatedWhitesCount, squaresWithBlacksCopy, updatedBlacksCount,
				(byte)flagsCopy, futureEnPassantSquare, plyNumber + 1);
	}

	private void movePieceOnPiecesLists(Square[] squaresWithWhites, Square[] squaresWithBlacks, Square from, Square to) {
		Square[] movingPieces = test(WHITE_TURN) ? squaresWithWhites : squaresWithBlacks;
		byte movingPiecesCount = test(WHITE_TURN) ? whitesCount : blacksCount;

		for (int i = 0; i < movingPiecesCount; i++) {
			if (movingPieces[i] == from) {
				movingPieces[i] = to;
				break;
			}
		}
	}

	private boolean test(int flagMask) {
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
		sb.append("squaresWithWhites: [");
		for (int i = 0; i < squaresWithWhites.length; i++) {
			sb.append(squaresWithWhites[i]).append(i == (whitesCount - 1) ? ";   " : ", ");
		}
		sb.append("] count: ").append(whitesCount).append('\n');
		sb.append("squaresWithBlacks: [");
		for (int i = 0; i < squaresWithBlacks.length; i++) {
			sb.append(squaresWithBlacks[i]).append(i == (blacksCount - 1) ? ";   " : ", ");
		}
		sb.append("] count: ").append(blacksCount).append('\n');
		sb.append("enPassantSquare: ").append(enPassantSquare).append('\n');
		sb.append("plyNumber: ").append(plyNumber).append('\n');
		sb.append("kingAttacker: ").append(kingAttacker).append('\n');
		return sb.toString();
	}

	public Content getContent(int file, int rank) {
		return getContent(Square.fromLegalInts(file, rank));
	}

	public Content getContent(Square square) {
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
			board[i] = (byte)(contentAsShort & (SquareFormat.PIECE_TYPE_MASK | SquareFormat.IS_WHITE_PIECE_FLAG));
		}
	}

	private void initSquaresInCheck() {
		initSquaresInCheck(BLACK);
		initSquaresInCheck(WHITE);
		initSquaresInCheckByKings();
	}

	private void initPinnedPieces() {
		Square whiteKing = squaresWithWhites[0];
		Square blackKing = squaresWithBlacks[0];
		assert getContent(whiteKing) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing) == Content.BLACK_KING : "Corrupted black king position";

		for (Square f : Square.values()) {
			pinnedPieces[f.ordinal()] = null;
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
		for (int i = 1; true ; i++) {
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

	private void initSquaresInCheck(boolean isCheckedByWhite) {
		int countOfPiecesTakingTurn = isCheckedByWhite ? whitesCount : blacksCount;
		Square[] squaresWithPiecesTakingTurn = isCheckedByWhite ? squaresWithWhites : squaresWithBlacks;

		// for every piece except king
		for (int i = countOfPiecesTakingTurn - 1; i > 0; i--) {
			Square currSquare = squaresWithPiecesTakingTurn[i];
			Content piece = Content.fromShort(board[currSquare.ordinal()]);
			switch (piece) {
				case WHITE_PAWN:
				case BLACK_PAWN:
					initSquaresInCheckByPawn(currSquare, isCheckedByWhite);
					break;
				case WHITE_KNIGHT:
				case BLACK_KNIGHT:
					initSquaresInCheckByKnight(currSquare, isCheckedByWhite);
					break;
				case WHITE_BISHOP:
				case BLACK_BISHOP:
					initSquaresInCheckByBishop(currSquare, isCheckedByWhite);
					break;
				case WHITE_ROOK:
				case BLACK_ROOK:
					initSquaresInCheckByRook(currSquare, isCheckedByWhite);
					break;
				case WHITE_QUEEN:
				case BLACK_QUEEN:
					initSquaresInCheckByQueen(currSquare, isCheckedByWhite);
					break;
				default:
					assert false : "Thing on:" + squaresWithPiecesTakingTurn[i] + " is unknown piece: " + piece;
			}
		}
	}

	private void initSquaresInCheckByKings() {
		Square whiteKing = squaresWithWhites[0];
		Square blackKing = squaresWithBlacks[0];
		assert getContent(whiteKing) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing) == Content.BLACK_KING : "Corrupted black king position";
		assert Math.abs(blackKing.rank - whiteKing.rank) > 1
				|| Math.abs(blackKing.file - whiteKing.file) > 1 : "Kings to close. w: " + whiteKing+ ", b: " + blackKing;

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

	private void initSquaresInCheckByQueen(Square queenSquare, boolean isCheckedByWhite) {
		initSquaresInCheckByBishop(queenSquare, isCheckedByWhite);
		initSquaresInCheckByRook(queenSquare, isCheckedByWhite);
	}

	private void initSquaresInCheckByRook(Square rookSquare, boolean isCheckedByWhite) {
		initCheckFlagsBySlidingPiece(rookSquare, isCheckedByWhite, 1, 0);
		initCheckFlagsBySlidingPiece(rookSquare, isCheckedByWhite, -1, 0);
		initCheckFlagsBySlidingPiece(rookSquare, isCheckedByWhite, 0, 1);
		initCheckFlagsBySlidingPiece(rookSquare, isCheckedByWhite, 0, -1);
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
			incrementChecksAndRefreshKingAttacker(from, underCheck, isCheckedByWhite);
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
			incrementChecksAndRefreshKingAttacker(from, to, isCheckedByWhite);
		}
		// check to the king side
		to = Square.fromInts(from.file + 1, from.rank + pawnDisplacement);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(from, to, isCheckedByWhite);
		}
	}

	private void initSquaresInCheckByKnight(Square knightSquare, boolean isCheckedByWhite) {
		Square to = Square.fromInts(knightSquare.file + 1, knightSquare.rank + 2);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file + 1, knightSquare.rank - 2);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 1, knightSquare.rank + 2);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 1, knightSquare.rank - 2);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file + 2, knightSquare.rank + 1);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file + 2, knightSquare.rank - 1);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 2, knightSquare.rank + 1);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
		to = Square.fromInts(knightSquare.file - 2, knightSquare.rank - 1);
		if (to != null) {
			incrementChecksAndRefreshKingAttacker(knightSquare, to, isCheckedByWhite);
		}
	}

	private void incrementChecksAndRefreshKingAttacker(Square attacker, Square checkedSquare, boolean isCheckedByWhite) {
		Square possiblyCheckedKing = isCheckedByWhite ? squaresWithBlacks[0] : squaresWithWhites[0];
		if (possiblyCheckedKing == checkedSquare) {
			kingAttacker = attacker;
		}
		incrementChecksOnSquare(checkedSquare, isCheckedByWhite);
	}

	private void incrementChecksOnSquare(Square square, boolean isCheckedByWhite) {
		short contentAsShort = board[square.ordinal()];
		byte bitOffset = isCheckedByWhite ? SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET : SquareFormat.CHECKS_BY_BLACK_BIT_OFFSET;
		short checksCount =  (short)((contentAsShort >>> bitOffset) & SquareFormat.CHECKS_COUNT_MASK);
		checksCount++;
		checksCount <<= bitOffset;

		short resettingMask = (short)~(SquareFormat.CHECKS_COUNT_MASK << bitOffset);
		contentAsShort = (short)(contentAsShort & resettingMask);

		board[square.ordinal()] = (short)(contentAsShort | checksCount);
	}

	private void setNoKingFlagOnSquare(Square square) {
		short contentAsShort = board[square.ordinal()];
		board[square.ordinal()] = (byte)(contentAsShort | SquareFormat.NO_KINGS_FLAG);
	}

	private boolean isSquareCheckedBy(Square square, boolean testChecksByWhite) {
		return getChecksCount(square, testChecksByWhite) > 0;
	}

	private byte getChecksCount(Square square, boolean checksByWhite) {
		byte bitOffset = checksByWhite ? SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET : SquareFormat.CHECKS_BY_BLACK_BIT_OFFSET;
		return (byte)((board[square.ordinal()] >> bitOffset) & SquareFormat.CHECKS_COUNT_MASK);
	}

	private boolean isSquareOkForKing(Square square, boolean isKingWhite) {
		return !isSquareCheckedBy(square, !isKingWhite) && (board[square.ordinal()] & SquareFormat.NO_KINGS_FLAG) == 0;
	}

	public List<State> generateLegalMoves() {
		List<State> moves = new ArrayList<>();

		int countOfPiecesTakingTurn = test(WHITE_TURN) ? whitesCount : blacksCount;
		Square[] squaresWithPiecesTakingTurn = test(WHITE_TURN) ? squaresWithWhites : squaresWithBlacks;

		if (isSquareCheckedBy(squaresWithPiecesTakingTurn[0], !test(WHITE_TURN))) {
			generateLegalMovesWhenKingInCheck(squaresWithPiecesTakingTurn[0], moves);
			return moves;
		}

		for (int i = 0; i < countOfPiecesTakingTurn; i++) {
			Square currSquare = squaresWithPiecesTakingTurn[i];
			Content piece = Content.fromShort(board[currSquare.ordinal()]);
			switch (piece) {
				case WHITE_PAWN:
				case BLACK_PAWN:
					generateLegalPawnMoves(currSquare, moves);
					break;
				case WHITE_KNIGHT:
				case BLACK_KNIGHT:
					generateLegalKnightMoves(currSquare, moves);
					break;
				case WHITE_BISHOP:
				case BLACK_BISHOP:
					generateLegalBishopMoves(currSquare, moves);
					break;
				case WHITE_ROOK:
				case BLACK_ROOK:
					generateLegalRookMoves(currSquare, moves);
					break;
				case WHITE_QUEEN:
				case BLACK_QUEEN:
					generateLegalQueenMoves(currSquare, moves);
					break;
				case WHITE_KING:
				case BLACK_KING:
					generateLegalKingMoves(currSquare, moves);
					break;
				default:
					assert false : "Thing on:" + squaresWithPiecesTakingTurn[i] + " is unknown piece: " + piece;
			}
		}
		return moves;
	}

	private void generateLegalKingMoves(Square from, List<State> outputMoves) {
		Square to = Square.fromInts(from.file, from.rank + 1);
		boolean isWhiteTurn = test(WHITE_TURN);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file + 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file + 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file + 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file - 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file - 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file - 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		int kingMovedBitflag = isWhiteTurn ? WHITE_KING_MOVED : BLACK_KING_MOVED;

		if (!test(kingMovedBitflag) &&
		// TODO check below goes away?
				!isSquareCheckedBy(from, !isWhiteTurn)) {

			Content rook = isWhiteTurn ? Content.WHITE_ROOK : Content.BLACK_ROOK;
			int qsRookMovedBitflag = isWhiteTurn ? WHITE_QS_ROOK_MOVED : BLACK_QS_ROOK_MOVED;
			int ksRookMovedBitflag = isWhiteTurn ? WHITE_KS_ROOK_MOVED : BLACK_KS_ROOK_MOVED;
			Square kingQsTo = isWhiteTurn ? Square.C1 : Square.C8;
			Square kingKsTo = isWhiteTurn ? Square.G1 : Square.G8;
			Square qsRook = isWhiteTurn ? Square.A1 : Square.A8;
			Square ksRook = isWhiteTurn ? Square.H1 : Square.H8;

			if (squaresOkForQsCastling(isWhiteTurn) && getContent(qsRook) == rook && !test(qsRookMovedBitflag)) {
				outputMoves.add(fromLegalQueensideCastling(from, kingQsTo));
			}
			if (squaresOkForKsCastling(isWhiteTurn) && getContent(ksRook) == rook && !test(ksRookMovedBitflag)) {
				outputMoves.add(fromLegalKingsideCastling(from, kingKsTo));
			}
		}
	}

	private boolean squaresOkForQsCastling(boolean isWhiteKingCastling) {
		for (int file = File.D; file >= File.C; file--) {
			Square square = Square.fromLegalInts(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(square) != Content.EMPTY || isSquareCheckedBy(square, !isWhiteKingCastling)) {
				return false;
			}
		}
		return getContent(isWhiteKingCastling ? Square.B1 : Square.B8) == Content.EMPTY;
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

	private void generateLegalQueenMoves(Square from, List<State> outputMoves) {
		generateLegalRookMoves(from, outputMoves);
		generateLegalBishopMoves(from, outputMoves);
	}

	private void generateLegalRookMoves(Square from, List<State> outputMoves) {
		generateSlidingPieceMoves(from, 1, 0, outputMoves);
		generateSlidingPieceMoves(from, -1, 0, outputMoves);
		generateSlidingPieceMoves(from, 0, 1, outputMoves);
		generateSlidingPieceMoves(from, 0, -1, outputMoves);
	}

	private void generateLegalBishopMoves(Square from, List<State> outputMoves) {
		generateSlidingPieceMoves(from, 1, 1, outputMoves);
		generateSlidingPieceMoves(from, 1, -1, outputMoves);
		generateSlidingPieceMoves(from, -1, 1, outputMoves);
		generateSlidingPieceMoves(from, -1, -1, outputMoves);
	}

	private void generateSlidingPieceMoves(Square from, int deltaFile, int deltaRank, List<State> outputMoves) {
		if (pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, deltaRank))) {
			for (int i = 1; true; i++) {
				Square to = Square.fromInts(from.file + deltaFile * i, from.rank + i * deltaRank);
				if (to == null || isSameColorPieceOn(to)) {
					break;
				}
				outputMoves.add(fromLegalMove(from, to));
				if (isOppositeColorPieceOn(to)) {
					break;
				}
			}
		}
	}

	private void generateLegalPawnMoves(Square from, List<State> outputMoves) {
		int pawnDisplacement = test(WHITE_TURN) ? 1 : -1;
		int pawnDoubleDisplacement = test(WHITE_TURN) ? 2 : -2;
		Square to = Square.fromLegalInts(from.file, from.rank + pawnDisplacement);
		// head-on move
		if (getContent(to) == Content.EMPTY && pieceIsFreeToMove(from, Pin.FILE)) {
			if (isPromotingSquare(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
				to = Square.fromLegalInts(from.file, from.rank + pawnDoubleDisplacement);
				if (isInitialSquareOfPawn(from) && getContent(to) == Content.EMPTY) {
					outputMoves.add(fromLegalPawnFirstMove(from, to, Square.fromLegalInts(from.file, from.rank + pawnDisplacement)));
				}
			}
		}
		// move with take to the queen-side
		int deltaFile = -1;
		to = Square.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);

		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantSquare)
				&& pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isPromotingSquare(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
			}
		}
		// move with take to the king side
		deltaFile = 1;
		to = Square.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);
		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantSquare)
				&& pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isPromotingSquare(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
			}
		}
	}

	private void generatePromotionMoves(Square from, Square to, List<State> outputMoves) {
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_QUEEN));
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_ROOK));
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_BISHOP));
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_KNIGHT));
	}

	private void generateLegalKnightMoves(Square from, List<State> outputMoves) {
		if (!pieceIsFreeToMove(from, null)) {
			return;
		}
		Square to = Square.fromInts(from.file + 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file + 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file - 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file - 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file + 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file + 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file - 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Square.fromInts(from.file - 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
	}

	/**
	 * Returns true if piece at pieceLocation can move along the movementDirection line (is not absolutely pinned).
	 * @param movementDirection - leave this null in case of knight at pieceLocation
	 */
	private boolean pieceIsFreeToMove(Square pieceLocation, Pin movementDirection) {
		Pin pin = pinnedPieces[pieceLocation.ordinal()];
		return pin == null || pin == movementDirection;
	}

	private void generateLegalMovesWhenKingInCheck(Square checkedKing, List<State> moves) {
		boolean isWhiteTurn = test(WHITE_TURN);

		if (getChecksCount(checkedKing, !isWhiteTurn) >= 2) {
			generateLegalKingMoves(checkedKing, moves);
		} else {
			switch (getContent(kingAttacker)) {
				/*
				if (checkedByKnight) {
					legalMoves.add(killKnight) // possible if knight is in check. (truly in check - now could be in check by a pinned piece)
				}else{
					//trying to find killing/shielding move may require more time than normal move generation + don't have
					legalMoves.add(killAttacker())
					legalMoves.add(shieldFromAttacker())

					legalMoves.add(moveKing)
				}
				*/
				case WHITE_PAWN:
				case BLACK_PAWN:
					break;
				case WHITE_KNIGHT:
				case BLACK_KNIGHT:
					break;
				case WHITE_BISHOP:
				case BLACK_BISHOP:
					break;
				case WHITE_ROOK:
				case BLACK_ROOK:
					break;
				case WHITE_QUEEN:
				case BLACK_QUEEN:
					break;
				default:
					assert false: "Invalid king attacker: " + getContent(kingAttacker);
			}
			// take or shield from attacker
		}
//		move king

		return;
	}
}
