package machess;

import static machess.State.*;

/**
 * Used to make and unmake moves from given State.
 * MSB                             LSB
 * 87654321 87654321 87654321 87654321
 * PppEEEee ettttRRR FFFrrrff flllllll
 *
 * lllllll - game state flags before this move (encoded as in State.flags except shifted left)
 *
 * fff - from file/origin file
 * rrr - from rank/origing rank
 *
 * FFF - to file/destination file
 * RRR - to rank/destination rank
 *
 * tttt - taken piece and it's color. Encoded as in Content.
 *
 * eee - en passant file (in case of pawn double push or pawn being taken - otherwise zeros)
 * EEE - en passant rank (in case of pawn double push or pawn being taken - otherwise zeros)
 *
 * k - 1 - pawn was taken en-passant in this move, 0 - pawn become vulnerable to en passant.
 * P - 1 - promotion has occurred in this move
 * pp - promote to code
 *      00 - knight
 *      01 - bishop
 *      10 - rook
 *      11 - queen
 */
public class Move {
    public static final int MASK_FLAGS = WHITE_TURN |
            WHITE_KING_MOVED | BLACK_KING_MOVED |
            WHITE_KS_ROOK_MOVED | WHITE_QS_ROOK_MOVED |
            BLACK_KS_ROOK_MOVED | BLACK_QS_ROOK_MOVED;

    public static final int MASK_SQUARE = 0x3F;
    public static final int MASK_FILE = 0x07;
    public static final int MASK_RANK = 0x07;
    public static final int MASK_TAKEN_PIECE = 0x0F;
    public static final int MASK_PROMOTION_CODE = 0x6000_0000;
    public static final int MASK_PROMOTION_FLAG = 0x8000_0000;

    public static final int CODE_KNIGHT_PROMOTION = 0x0000_0000;
    public static final int CODE_BISHOP_PROMOTION = 0x2000_0000;
    public static final int CODE_ROOK_PROMOTION = 0x4000_0000;
    public static final int CODE_QUEEN_PROMOTION = 0x6000_0000;

    public static final int BITSHIFT_FROM_SQUARE = 7;
    public static final int BITSHIFT_TO_SQUARE = 13;
    public static final int BITSHIFT_TAKEN_PIECE = 19;
    public static final int BITSHIFT_EN_PASSANT_SQUARE = 23;

    public static final int BITSHIFT_RANK_SQUARE = 3;

    Content getPromotion(int move) {
        boolean whiteTurn = (move & WHITE_TURN) != 0;
        int promotionCode = move & MASK_PROMOTION_CODE;

        switch (move) {
            case CODE_KNIGHT_PROMOTION:
                return whiteTurn ? Content.WHITE_KNIGHT : Content.BLACK_KNIGHT;
            case CODE_BISHOP_PROMOTION:
                return whiteTurn ? Content.WHITE_BISHOP : Content.BLACK_BISHOP;
            case CODE_ROOK_PROMOTION:
                return whiteTurn ? Content.WHITE_ROOK : Content.BLACK_ROOK;
            case CODE_QUEEN_PROMOTION:
                return whiteTurn ? Content.WHITE_QUEEN : Content.BLACK_QUEEN;
            default:
                assert false : "Unknown piece to promote: " + Integer.toHexString(promotionCode);
                return null;
        }
    }

    boolean isPromoting(int move) {
        return (move & MASK_PROMOTION_FLAG) != 0;
    }

    public static Content getTakenPiece(int move) {
        return Content.fromShort((short)((move >> BITSHIFT_TAKEN_PIECE) & MASK_TAKEN_PIECE));
    }

    public static Square getFrom(int move) {
        int sq = getSquare(move, BITSHIFT_FROM_SQUARE);
        return Square.fromLegalInts(getFileFromSquare(sq), getRankFromSquare(sq));
    }

    public static Square getTo(int move) {
        int sq = getSquare(move, BITSHIFT_TO_SQUARE);
        return Square.fromLegalInts(getFileFromSquare(sq), getRankFromSquare(sq));
    }

    public static Square getEnPassantSquare(int move) {
        int sq = getSquare(move, BITSHIFT_EN_PASSANT_SQUARE);
        return Square.fromLegalInts(getFileFromSquare(sq), getRankFromSquare(sq));
    }

    private static int getFileFromSquare(int square) {
        return square & MASK_FILE;
    }

    private static int getRankFromSquare(int square) {
        return square >> BITSHIFT_RANK_SQUARE;
    }

    private static int getSquare(int move, int shift) {
        return (move >> shift) & MASK_SQUARE;
    }
}
