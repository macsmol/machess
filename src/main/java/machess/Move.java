package machess;

import static machess.State.*;

/**
 * Used to make and unmake moves from given State.
 * MSB                             LSB
 * 87654321 87654321 87654321 87654321
 * ssssssss ssmm mRRR FFFrrrff flllllll
 *
 * lllllll - game state flags before this move (encoded as in State.flags except shifted left)
 *
 * fff - from file/origin file
 * rrr - from rank/origing rank
 *
 * FFF - to file/destination file
 * RRR - to rank/destination rank
 *
 * mmm - move selector - describes the meaning of ssss  bits. :
 *      000 - NORMAL_MOVE - Ordinary move. Special bits as below:
 *          ------tt tt------ -------- --------
 *          tttt - taken piece and it's color. Encoded as in Content.
 *
 *      001 - CASTLING - this move is a castling. Special bits as below:
 *          ----bbba aa------ -------- --------
 *          ----bbba aa -------- -------- //tuuuu
 *          aaa - castling rook origin file
 *          bbb - castling rook destination file
 *
 *      010 - DOUBLE_PUSH - this move is a pawn doublepush. Special bits as below:
 *          ----EEEe ee------ -------- --------
 *          EEEeee - en-passant square (the one that pawn jumps over)
 *
 *      011 - EN_PASSANT_TAKE - this move is a pawn taking other pawn en-passant. Special bits as below:
 *          ------tt tt------ -------- --------
 *          tttt - taken piece (pawn) and it's color. Encoded as in Content.
 *
 *      100 - PROMOTION - this move is a pawn promoting move. Special bits as below:
 *          ----pptt tt------ -------- --------
 *          tttt - taken piece and it's color. Encoded as in Content.
 *          pp - denotes what to promote to:
 *              00 - knight
 *              01 - bishop
 *              10 - rook
 *              11 - queen
 *
 * ssssssss ss - special bits with meaning dependent on the mmm selector
 */
public class Move {
    private Move(){}

    public static final int MASK_FLAGS = WHITE_TURN |
            WHITE_KING_MOVED | BLACK_KING_MOVED |
            WHITE_KS_ROOK_MOVED | WHITE_QS_ROOK_MOVED |
            BLACK_KS_ROOK_MOVED | BLACK_QS_ROOK_MOVED;

    public static final int MASK_SQUARE = 0x3F; // ok
    public static final int MASK_FILE = 0x07; // ok
    public static final int MASK_TAKEN_PIECE = 0x0F; //ok
    public static final int MASK_MOVE_SELECTOR = 0x0038_0000; //ok

    public static final int CODE_NORMAL_MOVE        = 0x0000_0000;
    public static final int CODE_CASTLING_MOVE      = 0x0008_0000;
    public static final int CODE_DOUBLE_PUSH_MOVE   = 0x0010_0000;
    public static final int CODE_EN_PASSANT_MOVE    = 0x0018_0000;
    public static final int CODE_PROMOTION_MOVE     = 0x0020_0000;

    public static final int MASK_PROMOTION_CODE = 0x0C00_0000; // ok

    public static final int CODE_KNIGHT_PROMOTION   = 0x0000_0000; // ok
    public static final int CODE_BISHOP_PROMOTION   = 0x0400_0000; // ok
    public static final int CODE_ROOK_PROMOTION     = 0x0800_0000; // ok
    public static final int CODE_QUEEN_PROMOTION    = 0x0C00_0000; // ok

    public static final int BITSHIFT_FROM_SQUARE = 7; //ok
    public static final int BITSHIFT_TO_SQUARE = 13; // ok
    public static final int BITSHIFT_TAKEN_PIECE = 22; // ok
    public static final int BITSHIFT_EN_PASSANT_SQUARE = 22; // ok
    public static final int BITSHIFT_ROOK_FROM_FILE = 22; // ok

    public static final int BITSHIFT_RANK_SQUARE = 3;




    public static int getMoveSelector(int move) {
        int moveSelector = move & MASK_MOVE_SELECTOR;
        assert moveSelector <= CODE_PROMOTION_MOVE : "Unknown move selector: " + Integer.toHexString(moveSelector);
        return moveSelector;
    }

    public static Square getRookFromSquare(int move) {
        assert (move & MASK_MOVE_SELECTOR) == CODE_CASTLING_MOVE
                : "Trying to get rook from square from non castling move: " + Integer.toHexString(move);

        int rank = getRankFromSquare(getSquare(move, BITSHIFT_FROM_SQUARE));
        int rookFromFile = (move >> BITSHIFT_ROOK_FROM_FILE) & MASK_FILE;
        return Square.fromInts(rookFromFile, rank);
    }

    public static Content getPromotion(int move) {
        assert (move & MASK_MOVE_SELECTOR) == CODE_PROMOTION_MOVE
                : "Trying to get promoting piece from non promoting move: " + Integer.toHexString(move);
        boolean whiteTurn = (move & WHITE_TURN) != 0;
        int promotionCode = move & MASK_PROMOTION_CODE;

        switch (promotionCode) {
            case CODE_KNIGHT_PROMOTION:
                return whiteTurn ? Content.WHITE_KNIGHT : Content.BLACK_KNIGHT;
            case CODE_BISHOP_PROMOTION:
                return whiteTurn ? Content.WHITE_BISHOP : Content.BLACK_BISHOP;
            case CODE_ROOK_PROMOTION:
                return whiteTurn ? Content.WHITE_ROOK : Content.BLACK_ROOK;
            case CODE_QUEEN_PROMOTION:
                return whiteTurn ? Content.WHITE_QUEEN : Content.BLACK_QUEEN;
            default:
                assert false : "Promotion to an unknown piece: " + Integer.toHexString(promotionCode);
                return null;
        }
    }

    public static Content getTakenPiece(int move) {
        assert (move & MASK_MOVE_SELECTOR) == CODE_EN_PASSANT_MOVE || (move & MASK_MOVE_SELECTOR) != CODE_NORMAL_MOVE
                || (move & MASK_MOVE_SELECTOR) != CODE_PROMOTION_MOVE
                : "Trying to get taken piece from non taking move: " + Integer.toHexString(move);
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
        assert (move & MASK_MOVE_SELECTOR) != CODE_DOUBLE_PUSH_MOVE
                : "En-passant square is initialized only double-push move: " + Integer.toHexString(move);
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
