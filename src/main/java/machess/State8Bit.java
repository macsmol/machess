package machess;

import java.util.*;

/**
 * basic snapshot of game state. Supplementary data will be derived from this class.
 * <p>
 * From wiki:
 * A full description of a chess position, i.e. the position "state", must contain the following elements:
 * <p>
 * -The location of each piece on the board
 * -Whose turn it is to move
 * -Status of the 50-move draw rule. The name of this is sometimes a bit confusing, as it is 50 moves by each player, and therefore 100 half-moves, or ply. For example, if the previous 80 half-moves passed without a capture or a pawn move, the fifty-move rule will kick in after another twenty half-moves.
 * -Whether either player is permanently disqualified to castle, both kingside and queenside.
 * -If an en passant capture is possible.
 */
public class State8Bit {
	private static int NUMBER_OF_PIECES = 32;
	private static byte FILES_COUNT = 8;
	private static byte RANKS_COUNT = 8;

	private static byte ALIVE_BIT_MASK = (byte)0x2;
	// use to mask file or rank once they are shifted
	private static byte COORD_BIT_MASK = (byte)0x07;

	private static byte FILE_OFFSET = 5;
	private static byte RANK_OFFSET = 2;

	/**
	 * list of pieces. byte per figure. 3 bit for file, 3 bits for row, 1 bit isAlive, not sure what lsb will be used for .
	 *      b0      b1       b2       b3       b4       b5       b6       b7       b8
	 * +--------+--------+--------+--------+--------+--------+--------+--------+--------+
	 * |FFFRRRx_|FFFRRRx_|FFFRRRx_|FFFRRRx_|FFFRRRx_|FFFRRRx_|FFFRRRx_|FFFRRRx_|FFFRRRx_|
	 */
	private byte[] pieces = new byte[NUMBER_OF_PIECES];

	/*
	   Initializes pieces for new game
	*/
	State8Bit() {
		for (PieceName pieceName : PieceName.values()) {
			pieces[pieceName.ordinal()] = pieceName.initialValue;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Map<Field, PieceName> pieceLocations = getPieceLocations();
		sb.append(" | a | b | c | d | e | f | g | h |\n");
		sb.append(" =================================\n");
		for (int rank = RANKS_COUNT - 1; rank >= 0; rank--) {
			sb.append(rank + 1).append("|");
			for (byte file = 0; file < FILES_COUNT; file++) {
				PieceName piece = pieceLocations.get(new Field(file, rank));
				if (piece != null) {
					sb.append(" ").append(piece.symbol).append(" |");
				} else {
					sb.append("   |");
				}
			}
			sb.append("\n-+---+---+---+---+---+---+---+---+\n");
		}
		return sb.toString();
	}

	private Map<Field, PieceName> getPieceLocations() {
		Map<Field, PieceName> locations = new LinkedHashMap<>();
		for (PieceName name : PieceName.values()) {
			byte piece = pieces[name.ordinal()];
			if ((piece & ALIVE_BIT_MASK) == 0) {
				continue;
			}
			byte file = (byte) ((piece >>> FILE_OFFSET) & COORD_BIT_MASK);
			byte rank = (byte) ((piece >>> RANK_OFFSET) & COORD_BIT_MASK);
			locations.put(new Field(file, rank), name);
		}
		return locations;
	}

	private class Field {
		byte file;
		byte rank;

		Field(int file, int rank) {
			this.file= (byte) file;
			this.rank= (byte) rank;
		}

		@Override
		public String toString() {
			return "Field{" +
					"file=" + file +
					", rank=" + rank +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Field)) {
				return false;
			}
			Field that = (Field) o;
			return file == that.file &&
					rank == that.rank;
		}

		@Override
		public int hashCode() {
			return Objects.hash(file, rank);
		}
	}

	public enum PieceName {
		WHITE_PAWN_A(   0x06, 'p'), // a2, alive
		WHITE_PAWN_B(   0x26, 'p'), // b2, alive
		WHITE_PAWN_C(   0x46, 'p'),
		WHITE_PAWN_D(   0x66, 'p'),
		WHITE_PAWN_E(   0x86, 'p'),
		WHITE_PAWN_F(   0xA6, 'p'),
		WHITE_PAWN_G(   0xC6, 'p'),
		WHITE_PAWN_H(   0xE6, 'p'),
		WHITE_ROOK_QS(  0x02, 'r'), // a1, alive
		WHITE_KNIGHT_QS(0x22, 'n'), // b1, alive
		WHITE_BISHOP_QS(0x42, 'b'), // c1, alive
		WHITE_QUEEN(    0x62, 'q'), // d1, alive
		WHITE_KING(     0x82, 'k'), // e1, alive
		WHITE_BISHOP_KS(0xA2, 'b'), // f1, alive
		WHITE_KNIGHT_KS(0xC2, 'n'), // g1, alive
		WHITE_ROOK_KS(  0xE2, 'r'), // h1, alive

		BLACK_PAWN_A(   0x1A, 'P'), // a7, alive
		BLACK_PAWN_B(   0x3A, 'P'), // b7, alive
		BLACK_PAWN_C(   0x5A, 'P'),
		BLACK_PAWN_D(   0x7A, 'P'),
		BLACK_PAWN_E(   0x9A, 'P'),
		BLACK_PAWN_F(   0xBA, 'P'),
		BLACK_PAWN_G(   0xDA, 'P'),
		BLACK_PAWN_H(   0xFA, 'P'),
		BLACK_ROOK_QS(  0x1E, 'R'), // a8, alive
		BLACK_KNIGHT_QS(0x3E, 'N'), // b8, alive
		BLACK_BISHOP_QS(0x5E, 'B'), // c8, alive
		BLACK_QUEEN(    0x7E, 'Q'), // d8, alive
		BLACK_KING(     0x9E, 'K'), // e8, alive
		BLACK_BISHOP_KS(0xBE, 'B'), // f8, alive
		BLACK_KNIGHT_KS(0xDE, 'N'), // g8, alive
		BLACK_ROOK_KS(  0xFE, 'R'); // h8, alive

		/**
		 Initial position and alive flag, lowest bit unset (has no meaning yet)
		 */
		public final byte initialValue;

		/**
		 * printable symbol
		 */
		public final char symbol;

		PieceName(int initialValue, char symbol) {
			this.initialValue = (byte)initialValue;
			this.symbol = symbol;
		}
	}
}

