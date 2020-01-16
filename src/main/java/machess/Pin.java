package machess;

import static machess.State.MetaSquareFormat.*;

/**
 * Describes the line to which movement is limited
 */
public class Pin {

    private static final byte[] LUT = {
            PIN_DIAGONAL, PIN_FILE, PIN_ANTIDIAGONAL, PIN_RANK, NULL_PIN, PIN_RANK, PIN_ANTIDIAGONAL, PIN_FILE, PIN_DIAGONAL
    };

    static byte fromDeltas(int deltaFile, int deltaRank) {
        assert (deltaFile != 0 || deltaRank != 0) 	: "Zero deltas";
        assert Math.abs(deltaFile) <= 1 			: "Invalid deltaFile: " + deltaFile;
        assert Math.abs(deltaRank) <= 1 			: "Invalid deltaRank: " + deltaRank;

        return LUT[(deltaFile + 1)  + (deltaRank + 1) * 3];
    }
}
