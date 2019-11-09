package machess;

import java.util.Arrays;

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
	 * fffrrrX
	 * 7 bits * 32 figures = 224 bits -> 28 bytes
	 */
	private byte[] pieces = new byte[28];

	/*
	   Initializes pieces for new game
	*/
	State() {
		for (Piece piece : Piece.values()) {
			byte pieceByte = readPieceAsByte(piece);

			pieceByte |= piece.initialValue;

			storePiece(piece, pieceByte);

		}
	}

	@Override
	public String toString() {
		return Arrays.toString(pieces);
	}

	/**
	 * Reads the seven bits representing piece from pieces array. Returns them as MSBs of a byte.
	 */
	private byte readPieceAsByte(Piece piece) {
		byte msbsOfPiece = (byte) (pieces[toByteIndexForMsbs(piece)] << toMsbHalfBitIndex(piece));
		byte lsbsOfPiece = (byte) (pieces[toByteIndexForLsbs(piece)] >>> (toLsbHalfBitIndex(piece)));

		// LSBs from second half are rubbish from next byte - dont overwrite it!
		return (byte)(msbsOfPiece | lsbsOfPiece);
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
		WHITE_PAWN_A(0, 2),
		WHITE_PAWN_B(BITS_PER_PIECE, 2),
		WHITE_PAWN_C(BITS_PER_PIECE * 2, 2),
		WHITE_PAWN_D(BITS_PER_PIECE * 3, 2),
		WHITE_PAWN_E(BITS_PER_PIECE * 4, 2),
		WHITE_PAWN_F(BITS_PER_PIECE * 5, 2),
		WHITE_PAWN_G(BITS_PER_PIECE * 6, 2),
		WHITE_PAWN_H(BITS_PER_PIECE * 7, 2),
		WHITE_KNIGHT_QS(BITS_PER_PIECE * 8, 2),
		WHITE_KNIGHT_KS(BITS_PER_PIECE * 9, 2),
		WHITE_BISHOP_QS(BITS_PER_PIECE * 10, 2),
		WHITE_BISHOP_KS(BITS_PER_PIECE * 11, 2),
		WHITE_ROOK_QS(BITS_PER_PIECE * 12, 2),
		WHITE_ROOK_KS(BITS_PER_PIECE * 13, 2),
		WHITE_QUEEN(BITS_PER_PIECE * 14, 2),
		WHITE_KING(BITS_PER_PIECE * 15, 2),
		BLACK_PAWN_A(BITS_PER_PIECE * 16, 2),
		BLACK_PAWN_B(BITS_PER_PIECE * 17, 2),
		BLACK_PAWN_C(BITS_PER_PIECE * 18, 2),
		BLACK_PAWN_D(BITS_PER_PIECE * 19, 2),
		BLACK_PAWN_E(BITS_PER_PIECE * 20, 2),
		BLACK_PAWN_F(BITS_PER_PIECE * 21, 2),
		BLACK_PAWN_G(BITS_PER_PIECE * 22, 2),
		BLACK_PAWN_H(BITS_PER_PIECE * 23, 2),
		BLACK_KNIGHT_QS(BITS_PER_PIECE * 24, 2),
		BLACK_KNIGHT_KS(BITS_PER_PIECE * 25, 2),
		BLACK_BISHOP_QS(BITS_PER_PIECE * 26, 2),
		BLACK_BISHOP_KS(BITS_PER_PIECE * 27, 2),
		BLACK_ROOK_QS(BITS_PER_PIECE * 28, 2),
		BLACK_ROOK_KS(BITS_PER_PIECE * 29, 2),
		BLACK_QUEEN(BITS_PER_PIECE * 30, 2),
		BLACK_KING(BITS_PER_PIECE * 31, 2);

		public final int bitIndex;
		public final int initialValue;

		Piece(int bitIndex, int initialValue) {
			this.bitIndex = bitIndex;
			this.initialValue = initialValue;
		}
	}
}

