package machess;

import com.sun.istack.internal.Nullable;

import java.util.*;

import static machess.Move.*;

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

	private static final int INITIAL_PLAYER_PIECES_COUNT = 16;

	// flags
	private byte flags;
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

	// king, queen, rooks and knights, pawns
	final Square[] squaresWithWhites;
	byte whitesCount;

	final Square[] squaresWithBlacks;
	byte blacksCount;
	/**
	 * The square jumped over by pawn while double-pushing.
	 * If not null it means there is a possibility to en-passant on this square
	 */
	private Square enPassantSquare;
	// not really necessary - only for debug purposes
	private int plyNumber;

	/**
	 * Board with absolutely pinned pieces. It's indexed by Square.ordinal()
	 */
	private final Pin[] pinnedPieces;

	/**
	 * special place kept so that we don't have ever to allocate/free it every time filtering pseudolegal moves when king is in check.
	 */
	private final int[] pseudoLegalMoves = new int[Config.DEFAULT_MOVES_CAPACITY];
	private int pseudoLegalMovesCount = 0;

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

	@Deprecated
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
	@Deprecated
	State fromPseudoLegalPawnDoublePush(Square from, Square to, Square enPassantSquare) {
		assert enPassantSquare != null;
		return fromPseudoLegalMove(from, to, null, enPassantSquare, null);
	}

	@Deprecated
	private State fromPseudoLegalMoveWithPromotion(Square from, Square to, Content promotion) {
		assert promotion != null;
		return fromPseudoLegalMove(from, to, promotion, null, null);
	}
	@Deprecated
	private State fromPseudoLegalQueensideCastling(Square kingFrom, Square kingTo) {
		Square rookToCastle = Square.fromLegalInts(File.A, kingFrom.rank);
		return fromPseudoLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}
	@Deprecated
	private State fromLegalKingsideCastling(Square kingFrom, Square kingTo) {
		Square rookToCastle = Square.fromLegalInts(File.H, kingFrom.rank);
		return fromPseudoLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}

	/**
	 * Generates new BoardState based on move. Typical move without special events.
	 */
	@Deprecated
	State fromPseudoLegalMove(Square from, Square to) {
		return fromPseudoLegalMove(from, to, null, null, null);
	}

	/**
	 *
	 * Modify this State by making a move give. The result might be an illegal position.
	 * @param move - move to make - it's trusted to be pseudolegal
	 */
	void makePseudoLegalMove(int move) {
		Square from = Move.getFrom(move);
		Square to = Move.getTo(move);
		assert from != to : from + "->" + to + " is no move";

		Content movedPiece = getContent(from);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		assert movedPiece.isWhite == test(WHITE_TURN) : "Moved " + movedPiece + " on " + (test(WHITE_TURN) ? "white" : "black") + " turn";

		board[from.ordinal()] = Content.EMPTY.asByte;
		movePieceOnPiecesLists(from, to);
		enPassantSquare = null;

		switch (Move.getMoveSelector(move)) {
			case CODE_NORMAL_MOVE:
				board[to.ordinal()] = movedPiece.asByte;
				if (getTakenPiece(move) != Content.EMPTY) {
					takePieceOnPieceList(to);
				}
				if (from == Square.E1) {
					flags |= WHITE_KING_MOVED;
				} else if (from == Square.E8) {
					flags |= BLACK_KING_MOVED;
				} else if (from == Square.A1) {
					flags |= WHITE_QS_ROOK_MOVED;
				} else if (from == Square.H1) {
					flags |= WHITE_KS_ROOK_MOVED;
				} else if (from == Square.A8) {
					flags |= BLACK_QS_ROOK_MOVED;
				} else if (from == Square.H8) {
					flags |= BLACK_KS_ROOK_MOVED;
				}
				break;
			case CODE_CASTLING_MOVE:
				board[to.ordinal()] = movedPiece.asByte;
				Square rookFrom = Move.getRookCastlingFrom(move);
				Square rookTo = Square.fromLegalInts(rookFrom.file == File.A ? File.D : File.F, rookFrom.rank);

				Content movedRook = getContent(rookFrom);
				board[rookFrom.ordinal()] = Content.EMPTY.asByte;
				board[rookTo.ordinal()] = movedRook.asByte;
				movePieceOnPiecesLists(rookFrom, rookTo);

				flags |= test(WHITE_TURN) ? WHITE_KING_MOVED : BLACK_KING_MOVED;
				break;
			case CODE_DOUBLE_PUSH_MOVE:
				board[to.ordinal()] = movedPiece.asByte;
				enPassantSquare = Move.getEnPassantSquare(move);
				break;
			case CODE_EN_PASSANT_MOVE:
				board[to.ordinal()] = movedPiece.asByte;
				Square killedPawnSquare = Square.fromLegalInts(to.file, test(WHITE_TURN) ? to.rank - 1 : to.rank + 1);
				takePieceOnPieceList(killedPawnSquare);
				board[killedPawnSquare.ordinal()] = Content.EMPTY.asByte;
				break;
			case CODE_PROMOTION_MOVE:
				board[to.ordinal()] = getPromotion(move).asByte;
				if (getTakenPiece(move) != Content.EMPTY) {
					takePieceOnPieceList(to);
				}
				break;
			default:
				assert false : "invalid move selector in " + Integer.toHexString(move);
		}
		flags ^= WHITE_TURN;
		plyNumber++;
		initPinnedPieces();
		resetSquaresInCheck();
		initSquaresInCheck();
	}

	private void takePieceOnPieceList(Square killedOn) {
		Square[] vulnerablePieces = test(WHITE_TURN) ? squaresWithBlacks : squaresWithWhites;
		byte vulnerablePiecesCount = test(WHITE_TURN) ? blacksCount : whitesCount;

		for (int i = 0; i < vulnerablePiecesCount; i++) {
			if (vulnerablePieces[i] == killedOn) {
				vulnerablePieces[i] = vulnerablePieces[vulnerablePiecesCount-1];
				if (test(WHITE_TURN)) {
					blacksCount--;
				} else {
					whitesCount--;
				}
				return;
			}
		}
		assert false : "There was nothing isWhite="+test(WHITE_TURN) + " could take on piece list "
				+ vulnerablePieces + "; size: " + vulnerablePiecesCount;
	}

	/**
	 * Revert input move in this State
	 * @param move
	 */
	void unmakePseudoLegalMove(int move) {
		plyNumber--;
		flags = (byte)(move & MASK_FLAGS);
		enPassantSquare = Move.getEnPassantUnmake(move);

		Square from = Move.getFrom(move);
		Square to = Move.getTo(move);
		assert from != to : from + "->" + to + " is no move";

		movePieceOnPiecesLists(to, from);
		switch (Move.getMoveSelector(move)) {
			case CODE_NORMAL_MOVE:
				Content movedPiece = getContent(to);
				Content takenPiece = Move.getTakenPiece(move);
				board[to.ordinal()] = takenPiece.asByte;
				if (takenPiece != Content.EMPTY) {
					untakePieceOnPieceList(to);
				}
				board[from.ordinal()] = movedPiece.asByte;
				break;
			case CODE_CASTLING_MOVE:
				movedPiece = getContent(to);
				board[to.ordinal()] = Content.EMPTY.asByte;
				board[from.ordinal()] = movedPiece.asByte;

				Square rookFrom = Move.getRookCastlingFrom(move);
				Square rookTo = Square.fromLegalInts(rookFrom.file == File.A ? File.D : File.F, rookFrom.rank);
				Content movedRook = getContent(rookTo);
				board[rookTo.ordinal()] = Content.EMPTY.asByte;
				board[rookFrom.ordinal()] = movedRook.asByte;
				movePieceOnPiecesLists(rookTo, rookFrom);
				break;
			case CODE_DOUBLE_PUSH_MOVE:
				movedPiece = getContent(to);
				board[to.ordinal()] = Content.EMPTY.asByte;
				board[from.ordinal()] = movedPiece.asByte;
				break;
			case CODE_EN_PASSANT_MOVE:
				movedPiece = getContent(to);
				board[to.ordinal()] = Content.EMPTY.asByte;
				board[from.ordinal()] = movedPiece.asByte;

				Square killedPawnSquare = Square.fromLegalInts(to.file, test(WHITE_TURN) ? to.rank - 1 : to.rank + 1);
				board[killedPawnSquare.ordinal()] = Move.getTakenPiece(move).asByte;
				untakePieceOnPieceList(killedPawnSquare);

				break;
			case CODE_PROMOTION_MOVE:
				takenPiece = Move.getTakenPiece(move);
				board[to.ordinal()] = takenPiece.asByte;
				if (takenPiece != Content.EMPTY) {
					untakePieceOnPieceList(to);
				}
				board[from.ordinal()] = test(WHITE_TURN) ? Content.WHITE_PAWN.asByte : Content.BLACK_PAWN.asByte;
				break;
			default:
				assert false : "invalid move selector in " + Integer.toHexString(move);
		}
		initPinnedPieces();
		resetSquaresInCheck();
		initSquaresInCheck();
	}

	private void untakePieceOnPieceList(Square killedOn) {
		Square[] vulnerablePieces = test(WHITE_TURN) ? squaresWithBlacks : squaresWithWhites;
		byte vulnerablePiecesCount = test(WHITE_TURN) ? blacksCount++ : whitesCount++;
		vulnerablePieces[vulnerablePiecesCount] = killedOn;
	}

	/**
	 * Generates new BoardState based on move. It does not verify game rules - assumes input is a legal move.
	 * This is the root method - it covers all cases. All 'overload' methods should call this one.
	 */
	@Deprecated
	private State fromPseudoLegalMove(Square from, Square to, @Nullable Content promotion, @Nullable Square futureEnPassantSquare,
									  @Nullable Square rookToCastle) {
		assert from != to : from + "->" + to + " is no move";
		short[] boardCopy = board.clone();
		Square[] squaresWithWhitesCopy = squaresWithWhites.clone();
		Square[] squaresWithBlacksCopy = squaresWithBlacks.clone();

		//  update boardCopy
		Content movedPiece = Content.fromShort(boardCopy[from.ordinal()]);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		assert movedPiece.isWhite == test(WHITE_TURN) : "Moved " + movedPiece + " on " + (test(WHITE_TURN) ? "white" : "black") + " turn";
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
				(byte) flagsCopy, futureEnPassantSquare, plyNumber + 1);
	}

	private void movePieceOnPiecesLists(Square from, Square to) {
		Square[] movingPieces = test(WHITE_TURN) ? squaresWithWhites : squaresWithBlacks;
		byte movingPiecesCount = test(WHITE_TURN) ? whitesCount : blacksCount;

		for (int i = 0; i < movingPiecesCount; i++) {
			if (movingPieces[i] == from) {
				movingPieces[i] = to;
				return;
			}
		}
	}

	@Deprecated
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

	boolean test(int flagMask) {
		return (flags & flagMask) != 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Utils.flagsToString(flags));
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
		initSquaresInCheck(BLACK);
		initSquaresInCheck(WHITE);
		initSquaresInCheckByKings();
	}

	private void initPinnedPieces() {
		Square whiteKing = squaresWithWhites[0];
		Square blackKing = squaresWithBlacks[0];
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

	@Deprecated
	public State chooseMove(int moveIndex) {
		return generateLegalMoves().get(moveIndex);
	}

	public int countLegalMoves() {
		int countOfPiecesTakingTurn = test(WHITE_TURN) ? whitesCount : blacksCount;
		Square[] squaresWithPiecesTakingTurn = test(WHITE_TURN) ? squaresWithWhites : squaresWithBlacks;
		if (isKingInCheck()) {
			return generateLegalMovesWhenKingInCheck(null, squaresWithPiecesTakingTurn[0], countOfPiecesTakingTurn,
					squaresWithPiecesTakingTurn);
		}
		return generatePseudoLegalMoves(null, countOfPiecesTakingTurn, squaresWithPiecesTakingTurn);
	}

//	change into public int[] generateLegalMoves() {}

	/**
	 * @return count of generated moves
	 * @param moves - preallocated array for output moves
	 */
	public int generateLegalMoves(int[] moves) {
		Square[] squaresWithPiecesTakingTurn = test(WHITE_TURN) ? squaresWithWhites : squaresWithBlacks;
		int countOfPiecesTakingTurn = test(WHITE_TURN) ? whitesCount : blacksCount;
		int movesCount;
		if (isKingInCheck()) {
			movesCount = generateLegalMovesWhenKingInCheck(moves, squaresWithPiecesTakingTurn[0], squaresWithPiecesTakingTurn, countOfPiecesTakingTurn);
		} else {
			movesCount = generatePseudoLegalMoves(moves, squaresWithPiecesTakingTurn, countOfPiecesTakingTurn);
		}
		System.out.println("Generated moves count: " + movesCount);

		for (int i = 0; i < movesCount; i++) {
			int move = moves[i];
			System.out.println(i + ":::" + Move.toString(move));
		}
		return movesCount;
	}

	@Deprecated
	public List<State> generateLegalMoves() {
		List<State> moves = new ArrayList<>(60);

		int countOfPiecesTakingTurn = test(WHITE_TURN) ? whitesCount : blacksCount;
		Square[] squaresWithPiecesTakingTurn = test(WHITE_TURN) ? squaresWithWhites : squaresWithBlacks;

		if (isKingInCheck()) {
			generateLegalMovesWhenKingInCheck(moves, squaresWithPiecesTakingTurn[0], countOfPiecesTakingTurn,
					squaresWithPiecesTakingTurn);
			return moves;
		}

		generatePseudoLegalMoves(moves, countOfPiecesTakingTurn, squaresWithPiecesTakingTurn);
		return moves;
	}

	boolean isKingInCheck() {
		Square[] squaresWithPiecesTakingTurn = test(WHITE_TURN) ? squaresWithWhites : squaresWithBlacks;
		return isSquareCheckedBy(squaresWithPiecesTakingTurn[0], !test(WHITE_TURN));
	}

	private int generatePseudoLegalMoves(int[] moves, Square[] squaresWithPiecesTakingTurn, int countOfPiecesTakingTurn) {
		int movesCount = 0;
		for (int i = 0; i < countOfPiecesTakingTurn; i++) {
			Square currSquare = squaresWithPiecesTakingTurn[i];
			Content piece = Content.fromShort(board[currSquare.ordinal()]);
			switch (piece) {
				case WHITE_PAWN:
				case BLACK_PAWN:
					movesCount = appendPseudoLegalPawnMoves(currSquare, moves, movesCount);
					break;
				case WHITE_KNIGHT:
				case BLACK_KNIGHT:
					movesCount = appendPseudoLegalKnightMoves(currSquare, moves, movesCount);
					break;
				case WHITE_BISHOP:
				case BLACK_BISHOP:
					movesCount = appendPseudoLegalBishopMoves(currSquare, moves, movesCount);
					break;
				case WHITE_ROOK:
				case BLACK_ROOK:
					movesCount = appendPseudoLegalRookMoves(currSquare, moves, movesCount);
					break;
				case WHITE_QUEEN:
				case BLACK_QUEEN:
					movesCount = appendPseudoLegalQueenMoves(currSquare, moves, movesCount);
					break;
				case WHITE_KING:
				case BLACK_KING:
					movesCount = appendLegalKingMoves(currSquare, moves, movesCount);
					break;
				default:
					assert false : "Thing on:" + squaresWithPiecesTakingTurn[i] + " is unknown piece: " + piece;
			}
		}
		return movesCount;
	}

	/**
	 * 	 Generates pseudo-legal moves. Takes into consideration absolute pins. Moves generated by this method are legal
	 * 	 * 	 provided that the king of the side taking turn was not in check.
	 * @param ouputMoves - leave this null to skip generation and just count
	 * @return number of output moves
	 */
	@Deprecated
	private int generatePseudoLegalMoves(List<State> ouputMoves, int countOfPiecesTakingTurn,
										  Square[] squaresWithPiecesTakingTurn) {
		int movesCount = 0;
		for (int i = 0; i < countOfPiecesTakingTurn; i++) {
			Square currSquare = squaresWithPiecesTakingTurn[i];
			Content piece = Content.fromShort(board[currSquare.ordinal()]);
			switch (piece) {
				case WHITE_PAWN:
				case BLACK_PAWN:
					movesCount += generatePseudoLegalPawnMoves(currSquare, ouputMoves);
					break;
				case WHITE_KNIGHT:
				case BLACK_KNIGHT:
					movesCount += generatePseudoLegalKnightMoves(currSquare, ouputMoves);
					break;
				case WHITE_BISHOP:
				case BLACK_BISHOP:
					movesCount += generatePseudoLegalBishopMoves(currSquare, ouputMoves);
					break;
				case WHITE_ROOK:
				case BLACK_ROOK:
					movesCount += generatePseudoLegalRookMoves(currSquare, ouputMoves);
					break;
				case WHITE_QUEEN:
				case BLACK_QUEEN:
					movesCount += generatePseudoLegalQueenMoves(currSquare, ouputMoves);
					break;
				case WHITE_KING:
				case BLACK_KING:
					movesCount += generatePseudoLegalKingMoves(currSquare, ouputMoves);
					break;
				default:
					assert false : "Thing on:" + squaresWithPiecesTakingTurn[i] + " is unknown piece: " + piece;
			}
		}
		return movesCount;
	}

	private int appendLegalKingMoves(Square from, int [] outputMoves, int movesCount) {
		Square to = Square.fromInts(from.file, from.rank + 1);
		boolean isWhiteTurn = test(WHITE_TURN);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isSquareOkForKing(to, isWhiteTurn)) {
			movesCount = appendMove(from, to, outputMoves, movesCount);
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
					outputMoves[movesCount] = Move.encodePseudoLegalCastling(from, kingQsTo, flags, enPassantSquare);
				}
				movesCount++;
			}
			if (squaresOkForKsCastling(isWhiteTurn) && getContent(ksRook) == rook && !test(ksRookMovedBitflag)) {
				if (outputMoves != null) {
					outputMoves[movesCount] = Move.encodePseudoLegalCastling(from, kingKsTo, flags, enPassantSquare);
				}
				movesCount++;
			}
		}
		return movesCount;
	}

	@Deprecated
	private int generatePseudoLegalKingMoves(Square from, List<State> outputMoves) {
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
					outputMoves.add(fromPseudoLegalQueensideCastling(from, kingQsTo));
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

	private int appendPseudoLegalQueenMoves(Square from, int [] moves, int movesCount) {
		movesCount = appendPseudoLegalRookMoves(from, moves, movesCount);
		movesCount = appendPseudoLegalBishopMoves(from, moves, movesCount);
		return movesCount;
	}

	@Deprecated
	private int generatePseudoLegalQueenMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generatePseudoLegalRookMoves(from, outputMoves);
		movesCount += generatePseudoLegalBishopMoves(from, outputMoves);
		return movesCount;
	}

	private int appendPseudoLegalRookMoves(Square from, int [] moves, int movesCount) {
		movesCount = appendSlidingPieceMoves(from, 1, 0, moves, movesCount);
		movesCount = appendSlidingPieceMoves(from, -1, 0, moves, movesCount);
		movesCount = appendSlidingPieceMoves(from, 0, 1, moves, movesCount);
		movesCount = appendSlidingPieceMoves(from, 0, -1, moves, movesCount);
		return movesCount;
	}

	@Deprecated
	private int generatePseudoLegalRookMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generateSlidingPieceMoves(from, 1, 0, outputMoves);
		movesCount += generateSlidingPieceMoves(from, -1, 0, outputMoves);
		movesCount += generateSlidingPieceMoves(from, 0, 1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, 0, -1, outputMoves);
		return movesCount;
	}

	private int appendPseudoLegalBishopMoves(Square from, int [] moves, int movesCount) {
		movesCount = appendSlidingPieceMoves(from, 1, 1, moves, movesCount);
		movesCount = appendSlidingPieceMoves(from, 1, -1, moves, movesCount);
		movesCount = appendSlidingPieceMoves(from, -1, 1, moves, movesCount);
		movesCount = appendSlidingPieceMoves(from, -1, -1, moves, movesCount);
		return movesCount;
	}

	@Deprecated
	private int generatePseudoLegalBishopMoves(Square from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generateSlidingPieceMoves(from, 1, 1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, 1, -1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, -1, 1, outputMoves);
		movesCount += generateSlidingPieceMoves(from, -1, -1, outputMoves);
		return movesCount;
	}

	private int appendSlidingPieceMoves(Square from, int deltaFile, int deltaRank,
										int [] moves, int movesCount) {
		if (pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, deltaRank))) {
			for (int i = 1; true; i++) {
				Square to = Square.fromInts(from.file + deltaFile * i, from.rank + i * deltaRank);
				if (to == null || isSameColorPieceOn(to)) {
					break;
				}
				movesCount = appendMove(from, to, moves, movesCount);
				if (isOppositeColorPieceOn(to)) {
					break;
				}
			}
		}
		return movesCount;
	}

	@Deprecated
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

	/**
	 * @param from - pawn location
	 * @param moves - output moves list to which pawn moves are appended to
	 * @return - count of output moves after generating pawn moves.
	 */
	/*private*/ int appendPseudoLegalPawnMoves(Square from, int[] moves, int movesCount) {
		int pawnDisplacement = test(WHITE_TURN) ? 1 : -1;
		int pawnDoubleDisplacement = test(WHITE_TURN) ? 2 : -2;
		Square to = Square.fromLegalInts(from.file, from.rank + pawnDisplacement);

		// push
		if (getContent(to) == Content.EMPTY && pieceIsFreeToMove(from, Pin.FILE)) {
			if (isPromotingSquare(to)) {
				movesCount = appendPromotionMoves(from, to, moves, movesCount);
			} else {
				movesCount = appendMove(from, to, moves, movesCount);
				to = Square.fromLegalInts(from.file, from.rank + pawnDoubleDisplacement);
				if (isInitialSquareOfPawn(from) && getContent(to) == Content.EMPTY) {
					if (moves != null) {
						moves[movesCount] = Move.encodePseudoLegalDoublePush(from, to,
								Square.fromLegalInts(from.file, from.rank + pawnDisplacement), flags, enPassantSquare);
					}
					movesCount++;
				}
			}
		}
		// queen-side take
		int deltaFile = -1;
		to = Square.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);

		if (to != null && pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isOppositeColorPieceOn(to)) {
				if (isPromotingSquare(to)) {
					movesCount = appendPromotionMoves(from, to, moves, movesCount);
				} else {
					movesCount = appendMove(from, to, moves, movesCount);
				}
			} else if (to == enPassantSquare) {
				if (moves != null) {
					moves[movesCount] = Move.encodePseudoLegalEnPassantMove(from, to, flags, enPassantSquare);
				}
				movesCount++;
			}
		}

		// king-side take
		deltaFile = 1;
		to = Square.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);
		if (to != null && pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isOppositeColorPieceOn(to)) {
				if (isPromotingSquare(to)) {
					movesCount = appendPromotionMoves(from, to, moves, movesCount);
				} else {
					movesCount = appendMove(from, to, moves, movesCount);
				}
			} else if (to == enPassantSquare) {
				if (moves != null) {
					moves[movesCount] = Move.encodePseudoLegalEnPassantMove(from, to, flags, enPassantSquare);
				}
				movesCount++;
			}
		}
		return movesCount;
	}

	@Deprecated
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

	private int appendPromotionMoves(Square from, Square to, int[] outputMoves, int movesCount) {
		if (outputMoves != null) {
			outputMoves[movesCount++] = Move.encodePseudoLegalPromotion(from, to, getContent(to), Content.WHITE_KNIGHT, flags, enPassantSquare);
			outputMoves[movesCount++] = Move.encodePseudoLegalPromotion(from, to, getContent(to), Content.WHITE_BISHOP, flags, enPassantSquare);
			outputMoves[movesCount++] = Move.encodePseudoLegalPromotion(from, to, getContent(to), Content.WHITE_ROOK, flags, enPassantSquare);
			outputMoves[movesCount++] = Move.encodePseudoLegalPromotion(from, to, getContent(to), Content.WHITE_QUEEN, flags, enPassantSquare);
			return movesCount;
		} else {
			return movesCount + 4;
		}
	}

	@Deprecated
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

	private int appendPseudoLegalKnightMoves(Square from, int [] moves, int movesCount) {
		if (!pieceIsFreeToMove(from, null)) {
			return movesCount;
		}
		Square to = Square.fromInts(from.file + 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		to = Square.fromInts(from.file + 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		to = Square.fromInts(from.file - 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		to = Square.fromInts(from.file + 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		to = Square.fromInts(from.file + 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		to = Square.fromInts(from.file - 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		to = Square.fromInts(from.file - 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			movesCount = appendMove(from, to, moves, movesCount);
		}
		return movesCount;
	}

	@Deprecated
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

	private int appendMove(Square from, Square to, int[] outputMoves, int movesCount) {
		if (outputMoves != null) {
			outputMoves[movesCount++] = Move.encodePseudoLegalMove(from, to, getContent(to), flags, enPassantSquare);
			return movesCount;
		}
		return movesCount + 1;
	}

	@Deprecated
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

	private int generateLegalMovesWhenKingInCheck(int [] outputMoves, Square checkedKing,
												  Square[] squaresWithPiecesTakingTurn, int countOfPiecesTakingTurn) {
		int movesCount = 0;
		if (getChecksCount(checkedKing, !test(WHITE_TURN)) < 2) {
			pseudoLegalMovesCount = generatePseudoLegalMoves(pseudoLegalMoves, squaresWithPiecesTakingTurn, countOfPiecesTakingTurn);

			for (int i = 0; i < pseudoLegalMovesCount; i++) {
				int move = pseudoLegalMoves[i];
				makePseudoLegalMove(move);
				if (isLegal()) {
					outputMoves[movesCount++] = move;
				}
				unmakePseudoLegalMove(move);
			}

			// double check
		} else {
			movesCount = appendLegalKingMoves(checkedKing, outputMoves,0);
		}
		return movesCount;
	}

	@Deprecated
	private int generateLegalMovesWhenKingInCheck(List<State> outputMoves, Square checkedKing,
												   int countOfPiecesTakingTurn, Square[] squaresWithPiecesTakingTurn) {
		int movesCount = 0;
		if (getChecksCount(checkedKing, !test(WHITE_TURN)) < 2) { // TODO BUG? no moves generated when double checked..
			List<State> pseudoLegalMoves = new ArrayList<>(Config.DEFAULT_MOVES_CAPACITY);
			generatePseudoLegalMoves(pseudoLegalMoves, countOfPiecesTakingTurn, squaresWithPiecesTakingTurn);
			for (State pseudoLegalState : pseudoLegalMoves) {
				if (pseudoLegalState.isLegal()) {
					if (outputMoves != null) {
						outputMoves.add(pseudoLegalState);
					} else {
						movesCount++;
					}
				}
			}
		}
		return movesCount;
	}

	/**
	 * Is king of side that just moved in check
	 */
	private boolean isLegal() {
		boolean isWhiteTurn = test(WHITE_TURN);
		Square king = isWhiteTurn ? squaresWithBlacks[0] : squaresWithWhites[0];
		return !isSquareCheckedBy(king, isWhiteTurn);
	}
}
