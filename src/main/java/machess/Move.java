package machess;

import static machess.State.*;

/**
 * Used to make and unmake moves from given State.
 * MSB                             LSB
 * 87654321 87654321 87654321 87654321
 * ssssssss ssmmmRRR FFFrrrff flllllll
 * ssssssss ss000RRR FFFrrrff flllllll - normal
 * ssssssss ss001RRR FFFrrrff flllllll - castling
 * ssssssss ss010RRR FFFrrrff flllllll - Double push
 * ssssssss ss011RRR FFFrrrff flllllll - en passant take
 * ssssssss ss100RRR FFFrrrff flllllll - promotion
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

    public static final int NULL = 0;

    public static final int MASK_FLAGS = WHITE_TURN |
            WHITE_KING_MOVED | BLACK_KING_MOVED |
            WHITE_KS_ROOK_MOVED | WHITE_QS_ROOK_MOVED |
            BLACK_KS_ROOK_MOVED | BLACK_QS_ROOK_MOVED;

    public static final int MASK_SQUARE = 0x3F;
    public static final int MASK_FILE = 0x07;
    public static final int MASK_TAKEN_PIECE = 0x0F;
    public static final int MASK_MOVE_SELECTOR = 0x0038_0000;

    public static final int CODE_NORMAL_MOVE        = 0x0000_0000;
    public static final int CODE_CASTLING_MOVE      = 0x0008_0000;
    public static final int CODE_DOUBLE_PUSH_MOVE   = 0x0010_0000;
    public static final int CODE_EN_PASSANT_MOVE    = 0x0018_0000;
    public static final int CODE_PROMOTION_MOVE     = 0x0020_0000;

    public static final int MASK_PROMOTION_CODE = 0x0C00_0000;

    public static final int CODE_KNIGHT_PROMOTION   = 0x0000_0000;
    public static final int CODE_BISHOP_PROMOTION   = 0x0400_0000;
    public static final int CODE_ROOK_PROMOTION     = 0x0800_0000;
    public static final int CODE_QUEEN_PROMOTION    = 0x0C00_0000;

    public static final int MODE_SELECTOR_BITCOUNT = 3;
    public static final int FLAGS_BITCOUNT = 7;
    public static final int TAKEN_PIECE_BITCOUNT = 4;
    public static final int BITSHIFT_TO_SQUARE = 13;
    public static final int BITSHIFT_SPECIAL_BITS = 22;
    public static final int BITSHIFT_MOVE_SELECTOR = 19;

    public static final int BITSHIFT_RANK_OR_FILE = 3;

    /**
     * Trusts the parameters that this move is pseudo-legal
     * Encodes normal move from 'from' and 'to' square. Set takenPiece to Content.EMPTY if nothing was taken.
     */
    public static int encodePseudoLegalMove(Square from, Square to, Content takenPiece, int flags) {
        int move = takenPiece.asByte << MODE_SELECTOR_BITCOUNT;
        return encodeCommonPartInMove(move, from, to, flags);
    }

    public static int encodePseudoLegalCastling(Square kingFrom, Square kingTo, int flags) {
        int rookFromFile = kingTo.file == File.C ? File.A : File.H;
        int rookToFile = kingTo.file == File.C ? File.D : File.F;
        int move = rookToFile << BITSHIFT_RANK_OR_FILE;
        move = (move | rookFromFile) << MODE_SELECTOR_BITCOUNT;
        move = move | (CODE_CASTLING_MOVE >> BITSHIFT_MOVE_SELECTOR);

        return encodeCommonPartInMove(move, kingFrom, kingTo, flags);
    }

    public static int encodePseudoLegalDoublePush(Square from, Square to, Square epSquare, int flags) {
        int move = epSquare.rank << BITSHIFT_RANK_OR_FILE;
        move = (move | epSquare.file) << MODE_SELECTOR_BITCOUNT;
        move = move | (CODE_DOUBLE_PUSH_MOVE >> BITSHIFT_MOVE_SELECTOR);
        return encodeCommonPartInMove(move, from, to, flags);
    }

    public static int encodePseudoLegalEnPassantMove(Square from, Square to, int flags) {
        byte pawnCode = (flags & WHITE_TURN) == 0 ? Content.WHITE_PAWN.asByte : Content.BLACK_PAWN.asByte;
        int move = pawnCode << MODE_SELECTOR_BITCOUNT;
        move = move | (CODE_EN_PASSANT_MOVE >> BITSHIFT_MOVE_SELECTOR);
        return encodeCommonPartInMove(move, from, to, flags);
    }

    public static int encodePseudoLegalPromotion(Square from, Square to, Content takenPiece,
                                                 Content promoteTo, int flags) {
        int move;
        switch (promoteTo) {
            case WHITE_KNIGHT:
                move = 0;
                break;
            case WHITE_BISHOP:
                move = 1;
                break;
            case WHITE_ROOK:
                move = 2;
                break;
            case WHITE_QUEEN:
                move = 3;
                break;
            default:
                assert false : "Promotion to " + promoteTo + " is invalid." +
                        "Pass here only WHITE pieces. Color is not encoded anyway";
                return NULL;
        }
        move <<= TAKEN_PIECE_BITCOUNT;
        move = (move | takenPiece.asByte) << MODE_SELECTOR_BITCOUNT;
        move = move | (CODE_PROMOTION_MOVE >> BITSHIFT_MOVE_SELECTOR);
        return encodeCommonPartInMove(move, from, to, flags);
    }

    private static int encodeCommonPartInMove(int move, Square from, Square to, int flags) {
        move <<= BITSHIFT_RANK_OR_FILE;
        move = (move | to.rank) << BITSHIFT_RANK_OR_FILE;
        move = (move | to.file) << BITSHIFT_RANK_OR_FILE;
        move = (move | from.rank) << BITSHIFT_RANK_OR_FILE;
        move = (move | from.file) << FLAGS_BITCOUNT;
        move = move | flags;
        return move;
    }

    public static int getMoveSelector(int move) {
        int moveSelector = move & MASK_MOVE_SELECTOR;
        assert moveSelector <= CODE_PROMOTION_MOVE : "Unknown move selector: " + Integer.toHexString(moveSelector);
        return moveSelector;
    }

    public static Square getRookCastlingFrom(int move) {
        assert (move & MASK_MOVE_SELECTOR) == CODE_CASTLING_MOVE
                : "Trying to get rook from square from non castling move: " + Integer.toHexString(move);

        int rank = getRankFromSquare(getSquare(move, FLAGS_BITCOUNT));
        int rookFromFile = (move >> BITSHIFT_SPECIAL_BITS) & MASK_FILE;
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
        Content takenPiece = Content.fromShort((short)((move >> BITSHIFT_SPECIAL_BITS) & MASK_TAKEN_PIECE));
        assert takenPiece != Content.WHITE_KING && takenPiece != Content.BLACK_KING
                : "move took king: " + Utils.asHexWithTrailingZeros(move);
        return takenPiece;
    }

    public static Square getFrom(int move) {
        int sq = getSquare(move, FLAGS_BITCOUNT);
        return Square.fromLegalInts(getFileFromSquare(sq), getRankFromSquare(sq));
    }

    public static Square getTo(int move) {
        int sq = getSquare(move, BITSHIFT_TO_SQUARE);
        return Square.fromLegalInts(getFileFromSquare(sq), getRankFromSquare(sq));
    }

    public static Square getEnPassantSquare(int move) {
        assert (move & MASK_MOVE_SELECTOR) == CODE_DOUBLE_PUSH_MOVE
                : "En-passant square is initialized only double-push move: " + Integer.toHexString(move);
        int sq = getSquare(move, BITSHIFT_SPECIAL_BITS);
        return Square.fromLegalInts(getFileFromSquare(sq), getRankFromSquare(sq));
    }

    public static String toString(int move) {
        StringBuilder sb = new StringBuilder();
        sb.append("from: ").append(Move.getFrom(move));
        sb.append(";  to: ").append(Move.getTo(move)).append(";  ");
        int moveSelector = move & MASK_MOVE_SELECTOR;
        switch (moveSelector) {
            case CODE_NORMAL_MOVE:
                if (getTakenPiece(move) != Content.EMPTY) {
                    sb.append("taken: ").append(getTakenPiece(move));
                }
                break;
            case CODE_CASTLING_MOVE:
                sb.append("castling rook from: ").append(getRookCastlingFrom(move));
                break;
            case CODE_DOUBLE_PUSH_MOVE:
                sb.append("double pushed over: ").append(getEnPassantSquare(move));
                break;
            case CODE_EN_PASSANT_MOVE:
                sb.append("taken ep: ").append(getTakenPiece(move));
                break;
            case CODE_PROMOTION_MOVE:
                sb.append("taken piece: ").append(getTakenPiece(move));
                sb.append(";  promote to: ").append(getPromotion(move));
                break;
            default:
                assert false : "invalid move selector in " + Integer.toHexString(move);
        }
        sb.append("     ").append(Utils.flagsToString(move));
        return sb.toString();
    }

    private static int getFileFromSquare(int square) {
        return square & MASK_FILE;
    }

    private static int getRankFromSquare(int square) {
        return square >> BITSHIFT_RANK_OR_FILE;
    }

    private static int getSquare(int move, int shift) {
        return (move >> shift) & MASK_SQUARE;
    }
}
