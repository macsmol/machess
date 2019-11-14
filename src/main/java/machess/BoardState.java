package machess;

import java.util.Arrays;

/**
 * Game state seen as game board rather than a list of figures.
 * All is static in order to avoid allocations.
 */
public class BoardState {

	static final byte PIECE_TYPE_MASK 	= 0x07;
	static final byte IS_WHITE_FLAG		= 0x08;
	private static final byte IN_CHECK_BY_WHITE	= 0x10;
	private static final byte IN_CHECK_BY_BLACK	= 0x20;
	/**
	 * one byte per field.
	 */
	private static final byte[] board = new byte[Field.FILES_COUNT * Field.RANKS_COUNT];

	public static void from(MasterState8Bit piecesState) {
		clear();
		for (MasterState8Bit.PieceName pieceName : MasterState8Bit.PieceName.values()) {
			byte piece = piecesState.pieces[pieceName.ordinal()];
			if ((piece & MasterState8Bit.ALIVE_BIT_MASK) == 0) {
				continue;
			}
			byte file = (byte) ((piece >>> MasterState8Bit.FILE_OFFSET) & MasterState8Bit.COORD_BIT_MASK);
			byte rank = (byte) ((piece >>> MasterState8Bit.RANK_OFFSET) & MasterState8Bit.COORD_BIT_MASK);

			setFieldContent(file, rank, pieceName);
		}
	}

	public static String debugString() {
		StringBuilder sb = new StringBuilder();
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
		return sb.toString();
	}

	public static byte getFieldContent(int file, int rank) {
		return board[Field.fromInts(file, rank).ordinal()];
	}

	public static void setFieldContent(int file, int rank, MasterState8Bit.PieceName piece) {
		board[Field.fromInts(file, rank).ordinal()] = FieldContent.fromPieceName(piece).contentAsByte;
	}

	private static void clear() {
		for (int i = 0; i < Field.FIELDS_COUNT; i++) {
			board[i] = FieldContent.EMPTY.contentAsByte;
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

		final byte contentAsByte;
		/**
		 * printable symbol for toString()
		 */
		public final char symbol;

		FieldContent(int contentAsByte, char symbol) {
			this.contentAsByte = (byte) contentAsByte;
			this.symbol = symbol;
		}

		static FieldContent fromByte(byte contentAsByte) {
			return byteToContents[contentAsByte & (BoardState.PIECE_TYPE_MASK | BoardState.IS_WHITE_FLAG)];
		}

		/**
		 * Implementing pawn promotions will be a problem here..?
		 */
		static FieldContent fromPieceName(MasterState8Bit.PieceName name) {
			switch (name) {
			case WHITE_PAWN_A:
			case WHITE_PAWN_B:
			case WHITE_PAWN_C:
			case WHITE_PAWN_D:
			case WHITE_PAWN_E:
			case WHITE_PAWN_F:
			case WHITE_PAWN_G:
			case WHITE_PAWN_H:
				return WHITE_PAWN;  
			case WHITE_ROOK_QS:
			case WHITE_ROOK_KS:
				return WHITE_ROOK;  
			case WHITE_KNIGHT_QS:
			case WHITE_KNIGHT_KS:
				return WHITE_KNIGHT;
			case WHITE_BISHOP_QS:
			case WHITE_BISHOP_KS:
				return WHITE_BISHOP;
			case WHITE_QUEEN:
				return WHITE_QUEEN;
			case WHITE_KING:
				return WHITE_KING;
			case BLACK_PAWN_A:
			case BLACK_PAWN_B:
			case BLACK_PAWN_C:
			case BLACK_PAWN_D:
			case BLACK_PAWN_E:
			case BLACK_PAWN_F:
			case BLACK_PAWN_G:
			case BLACK_PAWN_H:
				return BLACK_PAWN;  
			case BLACK_ROOK_QS:
			case BLACK_ROOK_KS:
				return BLACK_ROOK;  
			case BLACK_KNIGHT_QS:
			case BLACK_KNIGHT_KS:
				return BLACK_KNIGHT;
			case BLACK_BISHOP_QS:
			case BLACK_BISHOP_KS:
				return BLACK_BISHOP;
			case BLACK_QUEEN:
				return BLACK_QUEEN;
			case BLACK_KING:
				return BLACK_KING;
			default:
				throw new IllegalArgumentException("Unknown PieceName: " + name);
			}
		}
	}

}
