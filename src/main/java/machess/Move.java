package machess;

import static machess.State.*;

/**
 * Used to make and unmake moves from given State.
 */
public class Move {
    /**
     *  MSB                             LSB
     *  87654321 87654321 87654321 87654321
     *  PppEEEee ettttRRR FFFrrrff flllllll
     *  PppEEEee e
     *
     * lllllll - game state flags before this move (encoded as in State.flags except shifted left)
     * fff - from file/origin file
     * rrr - from rank/origing rank
     * FFF - to file/destination file
     * RRR - to rank/destination rank
     * tttt - taken piece and it's color. Encoded as in Content.
     * eee - en passant file (in case of pawn double push or pawn being taken - otherwise zeros)
     * EEE - en passant rank (in case of pawn double push or pawn being taken - otherwise zeros)
     * k - 1 - pawn was taken en-passant in this move, 0 - pawn become vulnerable to en passant.
     * P - 1 - promotion has occurred in this move
     * pp - promote to code
     *      00 - knight
     *      01 - bishop
     *      10 - rook
     *      11 - queen
     *
     */
    public static class Format {
        public static final int MASK_FLAGS = WHITE_TURN |
                WHITE_KING_MOVED | BLACK_KING_MOVED |
                WHITE_KS_ROOK_MOVED | WHITE_QS_ROOK_MOVED |
                BLACK_KS_ROOK_MOVED | BLACK_QS_ROOK_MOVED;

        public static final int MASK_SQUARE         = 0x07;
        public static final int MASK_TAKEN_PIECE    = 0x0F;
        public static final int MASK_PROMOTION_TO   = 0x6000_0000;
        public static final int MASK_PROMOTION_FLAG = 0x8000_0000;

        public static final int CODE_KNIGHT_PROMOTION   = 0x0000_0000;
        public static final int CODE_BISHOP_PROMOTION   = 0x2000_0000;
        public static final int CODE_ROOK_PROMOTION     = 0x4000_0000;
        public static final int CODE_QUEEN_PROMOTION    = 0x6000_0000;

        public static final int BITSHIFT_FROM_SQUARE        = 7;
        public static final int BITSHIFT_TO_SQUARE          = 13;
        public static final int BITSHIFT_TAKEN_PIECE        = 19;
        public static final int BITSHIFT_EN_PASSANT_SQUARE  = 23;
    }
}
