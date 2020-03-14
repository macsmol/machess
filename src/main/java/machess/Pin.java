package machess;

/**
 * Describes the line to which movement is limited
 */
enum Pin {
    FILE('|'),          // |
    RANK('_'),          // _
    DIAGONAL('/'),      // /
    ANTIDIAGONAL('\\'),	// \

    // cases specific for en passant takes - maybe just filter it afterwards? - logic is to iffy
    NO_QS_EN_PASSANT('{'),
    NO_KS_EN_PASSANT('}');

    char symbol;

    Pin(char symbol) {
        this.symbol = symbol;
    }

    private static final Pin[] LUT = {
        DIAGONAL, FILE, ANTIDIAGONAL, RANK, null, RANK, ANTIDIAGONAL, FILE, DIAGONAL
    };

    static Pin fromDeltas(int deltaFile, int deltaRank) {
        assert (deltaFile != 0 || deltaRank != 0) 	: "Zero deltas";
        assert Math.abs(deltaFile) <= 1 			: "Invalid deltaFile: " + deltaFile;
        assert Math.abs(deltaRank) <= 1 			: "Invalid deltaRank: " + deltaRank;

        return LUT[(deltaFile + 1)  + (deltaRank + 1) * 3];
    }
}
