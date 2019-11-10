package machess;

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
public class State {
	private static int NUMBER_OF_PIECES = 32;
	private static int BITS_PER_PIECE = 7;
	private static int FILE_OFFSET = 0;
	private static int ROW_OFFSET = 3;
	private static int LIFE_FLAG_OFFSET = 6;

	private static byte ALIVE_BIT_MASK = 0x2;

	/**
	 * list of pieces. 7 bit per figure. 3 bit for file, 3 bits for row, 1 bit isAlive. Type of figure denoted by location.
	 * 7 bits * 32 figures = 224 bits -> 28 bytes
	 *      b0      b1       b2       b3       b4       b5       b6
	 * +--------+--------+--------+--------+--------+--------+--------+
	 * |FFFRRRxF|FFRRRxFF|FRRRxFFF|RRRxFFFR|RRxFFFRR|RxFFFRRR|xFFFRRRx|
	           7       14      21      28      35      42      49
	 */
	private byte[] pieces = new byte[28];

	/*
	   Initializes pieces for new game
	*/
	State() {
		for (int i = 0; i < pieces.length; i++) {
			// init first piece in this byte
			int byteBeginInBits = i * Byte.SIZE;
			Piece piece1 = Piece.findByBitIndex(byteBeginInBits);
			byte pieceAsByte = piece1.initialValue;
			pieces[i] = (byte)(pieceAsByte << (byteBeginInBits - piece1.bitIndex));

			// init first part of the other piece in this byte
			int byteEndInBits = byteBeginInBits + Byte.SIZE - 1;
			Piece piece2 = Piece.findByBitIndex(byteEndInBits);
			byte piece2AsByte = piece2.initialValue;
			// OR with prievious piece from this byte in order not to overwrite
			pieces[i] = (byte)(pieces[i] | piece2AsByte >>> (piece2.bitIndex - byteBeginInBits));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Piece piece: Piece.values()) {
			byte pieceAsByte = readPieceAndZeroLsb(piece);
			sb.append("" + piece + ": " + String.format("%02x, ", pieceAsByte));
		}

		// something wrong is output...
		return sb.toString();
	}

	/**
	 * Reads the seven bits representing piece from pieces array. Returns them as MSBs of a byte.
	 */
	private byte readPieceAsByte(Piece piece) {
		byte msbsOfPiece = (byte) (pieces[toByteIndexForMsbs(piece)] << toMsbHalfBitIndex(piece));
		byte lsbsOfPiece = 0;
		if (toByteIndexForLsbs(piece) < pieces.length) {
			lsbsOfPiece = (byte) (pieces[toByteIndexForLsbs(piece)] >>> (toLsbHalfBitIndex(piece)));
		}
		// LSBs from second half are rubbish from next byte - dont overwrite it!
		return (byte)(msbsOfPiece | lsbsOfPiece);
	}
	 private byte readPieceAndZeroLsb(Piece piece) {
		return (byte)(readPieceAsByte(piece) & ~1);
	 }

	/**
	 *  Writes piece into pieces array.
	 * @param piece - what type of figure is written
	 * @param pieceByte - byte containing piece as read from readPieceAsByte()
	 */

	//czasami msbByteIndex to to samo co lsbByteIndex - co wtedy?
	private void storePiece(Piece piece, byte pieceByte) {
		byte msbsOfPiece = (byte) (pieceByte >>> (toMsbHalfBitIndex(piece)));
		byte lsbsOfPiece = (byte) (pieceByte << toLsbHalfBitIndex(piece));

		pieces[toByteIndexForMsbs(piece)] |= msbsOfPiece;
		if (toByteIndexForMsbs(piece) != toByteIndexForLsbs(piece)) {//może to można wywalić bo shift wyzeruje?
			pieces[toByteIndexForLsbs(piece)] |= lsbsOfPiece;
		}
	}

	private int toByteIndexForMsbs(Piece piece) {
		return piece.bitIndex / Byte.SIZE;
	}

	private int toByteIndexForLsbs(Piece piece) {
		return (piece.bitIndex + BITS_PER_PIECE) / Byte.SIZE;
	}

	private int toMsbHalfBitIndex(Piece piece) {
		return piece.bitIndex % Byte.SIZE;
	}

	private int toLsbHalfBitIndex(Piece piece) {
		return Byte.SIZE - toMsbHalfBitIndex(piece);
	}

	public enum Piece {
		WHITE_PAWN_A(0,                         0x06, '♙'), // a2, alive
		WHITE_PAWN_B(BITS_PER_PIECE,                    0x16, '♙'), // b2, alive
		WHITE_PAWN_C(BITS_PER_PIECE * 2,        0x26, '♙'),
		WHITE_PAWN_D(BITS_PER_PIECE * 3,        0x36, '♙'),
		WHITE_PAWN_E(BITS_PER_PIECE * 4,        0x46, '♙'),
		WHITE_PAWN_F(BITS_PER_PIECE * 5,        0x56, '♙'),
		WHITE_PAWN_G(BITS_PER_PIECE * 6,        0x66, '♙'),
		WHITE_PAWN_H(BITS_PER_PIECE * 7,        0x76, '♙'),
		WHITE_ROOK_QS(BITS_PER_PIECE * 8,       0x02,'♖'), // a1, alive
		WHITE_KNIGHT_QS(BITS_PER_PIECE * 9,     0x22,'♘'), // b1, alive
		WHITE_BISHOP_QS(BITS_PER_PIECE * 10,    0x42,'♗'), // c1, alive
		WHITE_QUEEN(BITS_PER_PIECE * 11,        0x62,'♕'), // d1, alive
		WHITE_KING(BITS_PER_PIECE * 12,         0x82,'♔'), // e1, alive
		WHITE_BISHOP_KS(BITS_PER_PIECE * 13,    0xA2,'♗'), // f1, alive
		WHITE_KNIGHT_KS(BITS_PER_PIECE * 14,    0xC2,'♘'), // g1, alive
		WHITE_ROOK_KS(BITS_PER_PIECE * 15,      0xE2,'♖'), // h1, alive

		BLACK_PAWN_A(BITS_PER_PIECE * 16,       0x1A, '♟'), // a7, alive
		BLACK_PAWN_B(BITS_PER_PIECE * 17,       0x3A, '♟'), // b7, alive
		BLACK_PAWN_C(BITS_PER_PIECE * 18,       0x5A, '♟'),
		BLACK_PAWN_D(BITS_PER_PIECE * 19,       0x7A, '♟'),
		BLACK_PAWN_E(BITS_PER_PIECE * 20,       0x9A, '♟'),
		BLACK_PAWN_F(BITS_PER_PIECE * 21,       0xBA, '♟'),
		BLACK_PAWN_G(BITS_PER_PIECE * 22,       0xDA, '♟'),
		BLACK_PAWN_H(BITS_PER_PIECE * 23,       0xFA, '♟'),
		BLACK_ROOK_QS(BITS_PER_PIECE * 24,      0x1E, '♜'), // a8, alive
		BLACK_KNIGHT_QS(BITS_PER_PIECE * 25,    0x3E, '♞'), // b8, alive
		BLACK_BISHOP_QS(BITS_PER_PIECE * 26,    0x5E, '♝'), // c8, alive
		BLACK_QUEEN(BITS_PER_PIECE * 27,        0x7E,'♛'), // d8, alive
		BLACK_KING(BITS_PER_PIECE * 28,         0x9E,'♚'), // e8, alive
		BLACK_BISHOP_KS(BITS_PER_PIECE * 29,    0xBE,'♝'), // f8, alive
		BLACK_KNIGHT_KS(BITS_PER_PIECE * 30,    0xDE,'♞'), // g8, alive
		BLACK_ROOK_KS(BITS_PER_PIECE * 31,      0xFE, '♜'); // h8, alive

		/**
		 * Index to pieces array, in bits.
		 */
		public final int bitIndex;
		
		/**
		 Initial position and alive flag, lowest bit unset (has no meaning)
		 */
		public final byte initialValue;

		/**
		 * printable symbol
		 */
		public final char symbol;

		Piece(int bitIndex, int initialValue, char symbol) {
			this.bitIndex = bitIndex;
			this.initialValue = (byte)initialValue;
			this.symbol = symbol;
		}

		static Piece findByBitIndex(int bitIndex) {
			return Piece.values()[bitIndex / BITS_PER_PIECE];
		}
	}
}

