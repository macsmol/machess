package machess;

import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Game state seen as game board rather than a list of figures.
 * All is static in order to avoid allocations.
 */
public class State {
	static final byte PIECE_TYPE_MASK = 0x07;
	static final byte IS_WHITE_FLAG = 0x08;
	private static final byte IN_CHECK_BY_WHITE = 0x10;
	private static final byte IN_CHECK_BY_BLACK = 0x20;

	private static final int INITIAL_PLAYER_PIECES_COUNT = 16;

	private final boolean isWhiteTurn;
	/**
	 * one byte per field.
	 */
	private final byte[] board;

	// king, queen, rooks and knights, pawns
	private final Field[] fieldsWithWhites;
	private final byte whitesCount;

	private final Field[] fieldsWithBlacks;
	private final byte blacksCount;

	/**
	 * new game
	 */
	State() {
		isWhiteTurn = true;
		board = new byte[Field.FILES_COUNT * Field.RANKS_COUNT];
		for (int file = 0; file < Field.FILES_COUNT; file++) {
			board[Field.fromUnsafeInts(file, 1).ordinal()] = Content.WHITE_PAWN.asByte;
			board[Field.fromUnsafeInts(file, 6).ordinal()] = Content.BLACK_PAWN.asByte;

			switch (file) {
				case 0:
				case 7:
					board[Field.fromUnsafeInts(file, 0).ordinal()] = Content.WHITE_ROOK.asByte;
					board[Field.fromUnsafeInts(file, 7).ordinal()] = Content.BLACK_ROOK.asByte;
					break;
				case 1:
				case 6:
					board[Field.fromUnsafeInts(file, 0).ordinal()] = Content.WHITE_KNIGHT.asByte;
					board[Field.fromUnsafeInts(file, 7).ordinal()] = Content.BLACK_KNIGHT.asByte;
					break;
				case 2:
				case 5:
					board[Field.fromUnsafeInts(file, 0).ordinal()] = Content.WHITE_BISHOP.asByte;
					board[Field.fromUnsafeInts(file, 7).ordinal()] = Content.BLACK_BISHOP.asByte;
					break;
				case 3:
					board[Field.fromUnsafeInts(file, 0).ordinal()] = Content.WHITE_QUEEN.asByte;
					board[Field.fromUnsafeInts(file, 7).ordinal()] = Content.BLACK_QUEEN.asByte;
					break;
				case 4:
					board[Field.fromUnsafeInts(file, 0).ordinal()] = Content.WHITE_KING.asByte;
					board[Field.fromUnsafeInts(file, 7).ordinal()] = Content.BLACK_KING.asByte;
					break;
			}
		}
		fieldsWithWhites = new Field[]{
				Field.E1, Field.D1, Field.A1, Field.H1, Field.B1, Field.C1, Field.F1, Field.G1,
				Field.A2, Field.B2, Field.C2, Field.D2, Field.E2, Field.F2, Field.G2, Field.H2
		};
		whitesCount = INITIAL_PLAYER_PIECES_COUNT;

		fieldsWithBlacks = new Field[]{
				Field.E8, Field.D8, Field.A8, Field.H8, Field.B8, Field.C8, Field.F8, Field.G8,
				Field.A7, Field.B7, Field.C7, Field.D7, Field.E7, Field.F7, Field.G7, Field.H7
		};
		blacksCount = INITIAL_PLAYER_PIECES_COUNT;
	}

	State(byte[] board, Field[] fieldsWithWhites, byte whitesCount, Field[] fieldsWithBlacks, byte blacksCount, boolean isWhiteTurn) {
		this.board = board;
		this.fieldsWithWhites = fieldsWithWhites;
		this.fieldsWithBlacks = fieldsWithBlacks;
		this.whitesCount = whitesCount;
		this.blacksCount = blacksCount;
		this.isWhiteTurn = isWhiteTurn;
	}

	State fromUnsafeMove(Field from, Field to) {
		return fromUnsafeMove(from, to, null);
	}
	/**
	 * Generates new BoardState based on move. It's unsafe - does not verify game rules.
	 */
	private State fromUnsafeMove(Field from, Field to, @Nullable Content promotion) {
		assert from != to : from + "->" + to + " is no move";
		byte[] boardCopy = board.clone();
		Field[] fieldsWithWhitesCopy = fieldsWithWhites.clone();
		Field[] fieldsWithBlacksCopy = fieldsWithBlacks.clone();

		//  update boardCopy
		Content movedPiece = Content.fromByte(boardCopy[from.ordinal()]);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		boardCopy[from.ordinal()] = Content.EMPTY.asByte;

		Content takenPiece = Content.fromByte(boardCopy[to.ordinal()]);
		assert takenPiece != Content.BLACK_KING && takenPiece != Content.WHITE_KING : from + "->" + to + " is taking king";
		boardCopy[to.ordinal()] = promotion != null ? promotion.asByte : movedPiece.asByte;

		// update pieces lists
		Field[] movingPieces = isWhiteTurn ? fieldsWithWhitesCopy : fieldsWithBlacksCopy;
		byte movingPiecesCount = isWhiteTurn ? whitesCount : blacksCount;
		Field[] takenPieces = isWhiteTurn ? fieldsWithBlacksCopy : fieldsWithWhitesCopy;
		byte takenPiecesCount = isWhiteTurn ? blacksCount : whitesCount;

		for (int i = 0; i < movingPiecesCount; i++) {
			if (movingPieces[i] == from) {
				movingPieces[i] = to;
			}
		}
		if (takenPiece != Content.EMPTY) {
			assert movedPiece.isWhite != takenPiece.isWhite : from + "->" + to + " is friendly take";
			for (int i = 0; i < takenPiecesCount; i++) {
				if (takenPieces[i] == to) {
					// decrement size of alive pieces
					takenPiecesCount--;
					takenPieces[i] = takenPieces[takenPiecesCount];
				}
			}
		}
		byte updatedWhitesCount = isWhiteTurn ? whitesCount : takenPiecesCount;
		byte updatedBlacksCount = isWhiteTurn ? takenPiecesCount : blacksCount;
		return new State(boardCopy, fieldsWithWhitesCopy, updatedWhitesCount, fieldsWithBlacksCopy, updatedBlacksCount, !isWhiteTurn);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: ").append(isWhiteTurn ? "WHITE" : "BLACK").append('\n');
		sb.append(" | a  | b  | c  | d  | e  | f  | g  | h  |\n");
		sb.append(" =========================================\n");
		for (int rank = Field.RANKS_COUNT - 1; rank >= 0; rank--) {
			sb.append(rank + 1).append("|");
			for (byte file = 0; file < Field.FILES_COUNT; file++) {
				Content content = getContent(file, rank);
				sb.append(" ").append(content.symbol).append(" |");
			}
			sb.append("\n-+----+----+----+----+----+----+----+----+\n");
		}
		sb.append("fieldsWithWhites: [");
		for (int i = 0; i < fieldsWithWhites.length; i++) {
			sb.append(fieldsWithWhites[i]).append(i == (whitesCount - 1) ? ";   " : ", ");
		}
		sb.append("] count: ").append(whitesCount).append('\n');
		sb.append("fieldsWithBlacks: [");
		for (int i = 0; i < fieldsWithBlacks.length; i++) {
			sb.append(fieldsWithBlacks[i]).append(i == (blacksCount - 1) ? ";   " : ", ");
		}
		sb.append("] count: ").append(blacksCount).append('\n');
		return sb.toString();
	}

	public Content getContent(int file, int rank) {
		return getContent(Field.fromUnsafeInts(file, rank));
	}

	public Content getContent(Field field) {
		return Content.fromByte(board[field.ordinal()]);
	}

	public boolean isWhitePieceOn(int file, int rank) {
			return isWhitePieceOn(Field.fromUnsafeInts(file, rank));
	}

	boolean isWhitePieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_FLAG) != 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	public boolean isBlackPieceOn(int file, int rank) {
			return isBlackPieceOn(Field.fromUnsafeInts(file, rank));
	}

	public boolean isBlackPieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_FLAG) == 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	public void debugIsPiecesOn() {
		for (int rank = Field.RANKS_COUNT - 1; rank >= 0; rank--) {
			for (byte file = 0; file < Field.FILES_COUNT; file++) {
				System.out.println("(file: " + file + "; rank: " + rank + ") isWhite/black: " + isWhitePieceOn(file, rank) + "/" + isBlackPieceOn(file, rank));
			}
		}
	}

	public enum Content {
		EMPTY(0x00,"  ", false),
		BLACK_PAWN(0x01,    "PP", false),
		BLACK_KNIGHT(0x02,  "NN", false),
		BLACK_BISHOP(0x03,  "BB", false),
		BLACK_ROOK(0x04,    "RR", false),
		BLACK_QUEEN(0x05,   "QQ", false),
		BLACK_KING(0x06,    "KK", false),
		WHITE_PAWN(0x09,    "P ", true),
		WHITE_KNIGHT(0x0A,  "N ", true),
		WHITE_BISHOP(0x0B,  "B ", true),
		WHITE_ROOK(0x0C,    "R ", true),
		WHITE_QUEEN(0x0D,   "Q ", true),
		WHITE_KING(0x0E,    "K ", true);

		private static final Content[] byteToContents = {
				EMPTY, BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING, EMPTY,
				EMPTY, WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING, EMPTY,
		};

		final byte asByte;
		/**
		 * printable symbol for toString()
		 */
		public final String symbol;

		final boolean isWhite;

		Content(int asByte, String symbol, boolean isWhite) {
			this.asByte = (byte) asByte;
			this.symbol = symbol;
			this.isWhite = isWhite;
		}

		static Content fromByte(byte contentAsByte) {
			return byteToContents[contentAsByte & (State.PIECE_TYPE_MASK | State.IS_WHITE_FLAG)];
		}
	}

	List<State> generateMoves() {
		List<State> moves = new ArrayList<>();
		if (isWhiteTurn) {
			for (int i = 0; i < whitesCount; i++) {
				Field currField = fieldsWithWhites[i];
				Content whitePiece = Content.fromByte(board[currField.ordinal()]);
				switch (whitePiece) {
					case WHITE_PAWN:
						moves.addAll(generateWhitePawnMoves(currField));
						break;
					case WHITE_KNIGHT:
						break;
					case WHITE_BISHOP:
						break;
					case WHITE_ROOK:
						break;
					case WHITE_QUEEN:
						break;
					case WHITE_KING:
						break;
					default:
						assert false : "Thing on:" + fieldsWithWhites[i] + " should be white but is: " + whitePiece;
				}
			}
		}
		return moves;
	}

	private List<State> generateWhitePawnMoves(Field from) {
		List<State> pawnMoves = new ArrayList<>();
		Field to = Field.fromUnsafeInts(from.file, from.rank + 1);
		// head-on move
		if (getContent(to) == Content.EMPTY) {
			if (to.rank == 7) {
				State e = fromUnsafeMove(from, to, Content.WHITE_QUEEN);
				System.out.println(e);
				pawnMoves.add(e);
				e = fromUnsafeMove(from, to, Content.WHITE_KNIGHT);
				System.out.println(e);
				pawnMoves.add(e);
			} else {
				State e = fromUnsafeMove(from, to);
				System.out.println(e);
				pawnMoves.add(e);
				if (from.rank == 1 && getContent(from.file, from.rank + 2) == Content.EMPTY) {
					State e1 = fromUnsafeMove(from, Field.fromUnsafeInts(from.file, from.rank + 2));
					System.out.println(e1);
					pawnMoves.add(e1);
				}
			}
		}

		to = Field.fromInts(from.file - 1, from.rank + 1);
		if (to != null && isBlackPieceOn(to)) {
			if (to.rank == 7) {
				State e = fromUnsafeMove(from, to, Content.WHITE_QUEEN);
				System.out.println(e);
				pawnMoves.add(e);
				e = fromUnsafeMove(from, to, Content.WHITE_KNIGHT);
				System.out.println(e);
				pawnMoves.add(e);
			} else {
				State e = fromUnsafeMove(from, to);
				System.out.println(e);
				pawnMoves.add(e);
			}
		}
		to = Field.fromInts(from.file + 1, from.rank + 1);
		if (to != null && isBlackPieceOn(to)) {
			if (to.rank == 7) {
				State e = fromUnsafeMove(from, to, Content.WHITE_QUEEN);
				System.out.println(e);
				pawnMoves.add(e);
				e = fromUnsafeMove(from, to, Content.WHITE_KNIGHT);
				System.out.println(e);
				pawnMoves.add(e);
			} else {
				State e = fromUnsafeMove(from, to);
				System.out.println(e);
				pawnMoves.add(e);
			}
		}
		return pawnMoves;
	}


}
