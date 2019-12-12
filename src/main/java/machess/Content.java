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

    private static final Content[] byteToContents = {
            EMPTY, BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING, EMPTY,
            EMPTY, WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING, EMPTY,
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

    static Content fromByte(byte contentAsByte) {
        return byteToContents[contentAsByte & (State.PIECE_TYPE_MASK | State.IS_WHITE_PIECE_FLAG)];
    }
}
