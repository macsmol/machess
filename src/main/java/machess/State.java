package machess;

import java.util.ArrayList;
import java.util.List;

/**
 * Game state seen as game board rather than a list of figures.
 * All is static in order to avoid allocations.
 */
public class State {
	static final byte PIECE_TYPE_MASK 	= 0x07;
	static final byte IS_WHITE_FLAG		= 0x08;
	private static final byte IN_CHECK_BY_WHITE	= 0x10;
	private static final byte IN_CHECK_BY_BLACK	= 0x20;

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
			board[Field.fromInts(file, 1).ordinal()] = FieldContent.WHITE_PAWN.asByte;
			board[Field.fromInts(file, 6).ordinal()] = FieldContent.BLACK_PAWN.asByte;

			switch (file) {
				case 0:
				case 7:
					board[Field.fromInts(file, 0).ordinal()] = FieldContent.WHITE_ROOK.asByte;
					board[Field.fromInts(file, 7).ordinal()] = FieldContent.BLACK_ROOK.asByte;
					break;
				case 1:
				case 6:
					board[Field.fromInts(file, 0).ordinal()] = FieldContent.WHITE_KNIGHT.asByte;
					board[Field.fromInts(file, 7).ordinal()] = FieldContent.BLACK_KNIGHT.asByte;
					break;
				case 2:
				case 5:
					board[Field.fromInts(file, 0).ordinal()] = FieldContent.WHITE_BISHOP.asByte;
					board[Field.fromInts(file, 7).ordinal()] = FieldContent.BLACK_BISHOP.asByte;
					break;
				case 3:
					board[Field.fromInts(file, 0).ordinal()] = FieldContent.WHITE_QUEEN.asByte;
					board[Field.fromInts(file, 7).ordinal()] = FieldContent.BLACK_QUEEN.asByte;
					break;
				case 4:
					board[Field.fromInts(file, 0).ordinal()] = FieldContent.WHITE_KING.asByte;
					board[Field.fromInts(file, 7).ordinal()] = FieldContent.BLACK_KING.asByte;
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

	/**
	 * Generates new BoardState based on move. It's unsafe - does not verify game rules.
	 */
	State fromUnsafeMove(Field from, Field to) {
		byte[] boardCopy = board.clone();
		Field[] fieldsWithWhitesCopy = fieldsWithWhites.clone();
		Field[] fieldsWithBlacksCopy = fieldsWithBlacks.clone();

		//  update boardCopy
		FieldContent movedPiece = FieldContent.fromByte(boardCopy[from.ordinal()]);
		assert movedPiece != FieldContent.EMPTY :  from + "->" + to;
		boardCopy[from.ordinal()] = FieldContent.EMPTY.asByte;

		FieldContent takenPiece = FieldContent.fromByte(boardCopy[to.ordinal()]);
		assert takenPiece != FieldContent.BLACK_KING && takenPiece != FieldContent.WHITE_KING : from + "->" + to;
		boardCopy[to.ordinal()] = movedPiece.asByte;

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
		if (takenPiece != FieldContent.EMPTY) {
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
				byte contentAsByte = getFieldContent(file, rank);
				FieldContent fieldContent = FieldContent.fromByte(contentAsByte);
				sb.append("  ").append(fieldContent.symbol).append(" |");
			}
			sb.append("\n-+----+----+----+----+----+----+----+----+\n");
		}
		sb.append("fieldsWithWhites: [");
		for (int i = 0; i < fieldsWithWhites.length; i++) {
			sb.append(fieldsWithWhites[i]).append(i == (whitesCount-1) ? ";  " : ", ");
		}
		sb.append("] count: ").append(whitesCount).append('\n');
		sb.append("fieldsWithBlacks: [");
		for (int i = 0; i < fieldsWithBlacks.length; i++) {
			sb.append(fieldsWithBlacks[i]).append(i == (blacksCount-1) ? ";  " : ", ");
		}
		sb.append("] count: ").append(blacksCount).append('\n');
		return sb.toString();
	}

	public byte getFieldContent(int file, int rank) {
		return getFieldContent(Field.fromInts(file, rank));
	}

	public byte getFieldContent(Field field) {
		return board[field.ordinal()];
	}

	public boolean isWhitePieceOn(int file, int rank) {
		return isWhitePieceOn(Field.fromInts(file, rank));
	}

	boolean isWhitePieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_FLAG) != 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	public boolean isBlackPieceOn(int file, int rank) {
		return isBlackPieceOn(Field.fromInts(file, rank));
	}

	public boolean isBlackPieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_FLAG) == 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	public void debugIsPiecesOn() {
		for (int rank = Field.RANKS_COUNT - 1; rank >= 0; rank--) {
			for (byte file = 0; file < Field.FILES_COUNT; file++) {
				System.out.println("(file: " + file + "; rank: " + rank + ") isWhite/black: " + isWhitePieceOn(file,rank)+ "/" + isBlackPieceOn(file,rank));
			}
		}
	}

	public enum FieldContent {
		EMPTY(			0x00, ' '),
		BLACK_PAWN(		0x01, 'P'),
		BLACK_KNIGHT(	0x02, 'N'),
		BLACK_BISHOP(	0x03, 'B'),
		BLACK_ROOK(		0x04, 'R'),
		BLACK_QUEEN(	0x05, 'Q'),
		BLACK_KING(		0x06, 'K'),
		WHITE_PAWN(		0x09, 'p'),
		WHITE_KNIGHT(	0x0A, 'n'),
		WHITE_BISHOP(	0x0B, 'b'),
		WHITE_ROOK(		0x0C, 'r'),
		WHITE_QUEEN(	0x0D, 'q'),
		WHITE_KING(		0x0E, 'k');

		private static final FieldContent[] byteToContents = {
				EMPTY, BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN,	BLACK_KING,	EMPTY,
				EMPTY, WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN,	WHITE_KING,	EMPTY,
		};

		final byte asByte;
		/**
		 * printable symbol for toString()
		 */
		public final char symbol;

		FieldContent(int asByte, char symbol) {
			this.asByte = (byte) asByte;
			this.symbol = symbol;
		}

		static FieldContent fromByte(byte contentAsByte) {
			return byteToContents[contentAsByte & (State.PIECE_TYPE_MASK | State.IS_WHITE_FLAG)];
		}
	}

	List<State> generatePawnMoves(boolean isWhiteTurn) {
		List<State> pawnMoves = new ArrayList<>();
		if (isWhiteTurn) {

		}
		return  pawnMoves;
	}
}
