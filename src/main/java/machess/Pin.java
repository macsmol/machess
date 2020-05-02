package machess;

import machess.board0x88.Direction;

/**
 * Describes the line to which movement is limited
 */
enum Pin {
    FILE('|'),          // |
    RANK('_'),          // _
    DIAGONAL('/'),      // /
    ANTIDIAGONAL('\\');	// \

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

    static Pin fromDirection(byte direction) {
        switch(direction) {
            case Direction.N:
            case Direction.S:
                return FILE;
            case Direction.E:
            case Direction.W:
                return RANK;
            case Direction.SW:
            case Direction.NE:
                return DIAGONAL;
            case Direction.NW:
            case Direction.SE:
                return ANTIDIAGONAL;
            default:
                assert false : "Expected a sliding piece direction";
                return null;
        }
    }
}
