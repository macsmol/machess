package machess;

public enum Content {
    EMPTY(0x00,"   ", State.BLACK),
    BLACK_PAWN(0x01,    "pp ", State.BLACK),
    BLACK_KNIGHT(0x02,  "NN_", State.BLACK),
    BLACK_BISHOP(0x03,  "BB_", State.BLACK),
    BLACK_ROOK(0x04,    "_RR", State.BLACK),
    BLACK_QUEEN(0x05,   "QQ_", State.BLACK),
    BLACK_KING(0x06,    "KK_", State.BLACK),
    WHITE_PAWN(0x09,    " p ", State.WHITE),
    WHITE_KNIGHT(0x0A,  " N_", State.WHITE),
    WHITE_BISHOP(0x0B,  " B_", State.WHITE),
    WHITE_ROOK(0x0C,    " _R", State.WHITE),
    WHITE_QUEEN(0x0D,   " Q_", State.WHITE),
    WHITE_KING(0x0E,    " K_", State.WHITE);

    private static final Content[] BYTE_TO_CONTENT = {
            EMPTY, BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING, EMPTY,
            EMPTY, WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING, EMPTY,
    };

    private static final Content[] DELTAS_TO_BLACK_CONTENT = {
            BLACK_BISHOP, BLACK_ROOK, BLACK_BISHOP, BLACK_ROOK, null, BLACK_ROOK, BLACK_BISHOP, BLACK_ROOK, BLACK_BISHOP
    };
    private static final Content[] DELTAS_TO_WHITE_CONTENT = {
            WHITE_BISHOP, WHITE_ROOK, WHITE_BISHOP, WHITE_ROOK, null, WHITE_ROOK, WHITE_BISHOP, WHITE_ROOK, WHITE_BISHOP
    };

    final byte asByte;
    /**
     * printable symbol for toString()
     */
    final String symbol;

    final boolean isWhite;

    Content(int asByte, String symbol, boolean isWhite) {
        this.asByte = (byte) asByte;
        this.symbol = symbol;
        this.isWhite = isWhite;
    }

    /**
     * MSB           LSB
     * 87654321 87654321
     * ________ ____wccc
     *
     *  ccc - piece code , eg:
     *      000 - empty square
     *      001 - pawn
     *      010 - knight
     *      011 - bishop
     *      100 - rook
     *      101 - queen
     *      110 - king
     *
     *  w - is white bit
     */
    static Content fromShort(short contentAsShort) {
        return BYTE_TO_CONTENT[contentAsShort & (State.SquareFormat.PIECE_TYPE_MASK | State.SquareFormat.IS_WHITE_PIECE_FLAG)];
    }

    static Content rookOrBishop(boolean isWhite, int deltaFile, int deltaRank) {
        assert (deltaFile != 0 || deltaRank != 0) 	: "Zero deltas";
        assert Math.abs(deltaFile) <= 1 			: "Invalid deltaFile: " + deltaFile;
        assert Math.abs(deltaRank) <= 1 			: "Invalid deltaRank: " + deltaRank;

        Content[] lut = isWhite ? DELTAS_TO_WHITE_CONTENT : DELTAS_TO_BLACK_CONTENT;
        return lut[(deltaFile + 1)  + (deltaRank + 1) * 3];
    }
}
