package machess;

import com.sun.istack.internal.Nullable;

public enum Field {
	A1(0, 0), A2(0, 1), A3(0, 2), A4(0, 3), A5(0, 4), A6(0, 5), A7(0, 6), A8(0, 7),
	B1(1, 0), B2(1, 1), B3(1, 2), B4(1, 3), B5(1, 4), B6(1, 5), B7(1, 6), B8(1, 7),
	C1(2, 0), C2(2, 1), C3(2, 2), C4(2, 3), C5(2, 4), C6(2, 5), C7(2, 6), C8(2, 7),
	D1(3, 0), D2(3, 1), D3(3, 2), D4(3, 3), D5(3, 4), D6(3, 5), D7(3, 6), D8(3, 7),
	E1(4, 0), E2(4, 1), E3(4, 2), E4(4, 3), E5(4, 4), E6(4, 5), E7(4, 6), E8(4, 7),
	F1(5, 0), F2(5, 1), F3(5, 2), F4(5, 3), F5(5, 4), F6(5, 5), F7(5, 6), F8(5, 7),
	G1(6, 0), G2(6, 1), G3(6, 2), G4(6, 3), G5(6, 4), G6(6, 5), G7(6, 6), G8(6, 7),
	H1(7, 0), H2(7, 1), H3(7, 2), H4(7, 3), H5(7, 4), H6(7, 5), H7(7, 6), H8(7, 7);

	public static final byte FILES_COUNT = 8;
	public static final byte RANKS_COUNT = 8;

	public final byte file;
	public final byte rank;

	private static final Field[] INTS_TO_FIELDS = {
		A1, A2, A3, A4, A5, A6, A7, A8,
		B1, B2, B3, B4, B5, B6, B7, B8,
		C1, C2, C3, C4, C5, C6, C7, C8,
		D1, D2, D3, D4, D5, D6, D7, D8,
		E1, E2, E3, E4, E5, E6, E7, E8,
		F1, F2, F3, F4, F5, F6, F7, F8,
		G1, G2, G3, G4, G5, G6, G7, G8,
		H1, H2, H3, H4, H5, H6, H7, H8
	};

	Field(int file, int rank) {
		this.file = (byte) file;
		this.rank = (byte) rank;
	}

	static Field fromLegalInts(int file, int rank) {
		assert file >= 0 && file < FILES_COUNT : "invalid file in: " + file + ", " + rank;
		assert rank >= 0 && rank < RANKS_COUNT : "invalid rank in: " + file + ", " + rank;
		return INTS_TO_FIELDS[file * FILES_COUNT + rank];
	}

	@Nullable
	static Field fromInts(int file, int rank) {
		if (file < 0 || file >= FILES_COUNT || rank < 0 || rank >= RANKS_COUNT) {
			return null;
		}
		return INTS_TO_FIELDS[file * FILES_COUNT + rank];
	}
}
