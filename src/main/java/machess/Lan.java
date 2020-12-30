package machess;

import machess.board0x88.Square0x88;
import machess.board8x8.Rank;

import static machess.Content.*;
import static machess.Content.BLACK_QUEEN;

/**
 * Parser/formater for moves in Long Algebraic Notation as used in UCI
 */
public class Lan {
    public static final char KNIGHT = 'n';
    public static final char BISHOP = 'b';
    public static final char ROOK = 'r';
    public static final char QUEEN = 'q';

    /**
     * Produces new State based on moves given in Long Algebraic Notation
     * @param state state after move is made
     * @param move - move given in long algebraic notation. Examples:  e2e4, e7e5, e1g1 (white short castling), e7e8q (for promotion)
     * @return state after the move is made
     */
    public static State move(State state, String move) {
        if (isPromotion(move)) {
            return state.fromPseudoLegalMoveWithPromotion(getFrom(move), getTo(move), getPromotion(move));
        } else if (isQsCastling(state, move)) {
            return state.fromLegalQueensideCastling(getFrom(move), getTo(move));
        } else if (isKsCastling(state, move)) {
            return state.fromLegalKingsideCastling(getFrom(move), getTo(move));
        } else if (isDoublePush(state, move)) {
            return state.fromPseudoLegalPawnDoublePush(getFrom(move), getTo(move), getEnPassantSquare(move));
        }
        return state.fromPseudoLegalMove(getFrom(move), getTo(move));
    }

    private static byte getEnPassantSquare(String move) {
        int fromFile = move.charAt(0) - 'a';
        int fromRank = move.charAt(1) - '1';
        return Square0x88.from07(fromFile, fromRank == Rank._2 ? Rank._3 : Rank._6);
    }

    private static boolean isPromotion(String move) {
        return move.length() == 5;
    }

    private static byte getFrom(String move) {
        return getSquare(move, 0, 1);
    }

    private static byte getTo(String move) {
        return getSquare(move, 2, 3);
    }

    private static boolean isQsCastling(State state, String move) {
        byte from = getFrom(move);
        byte to = getTo(move);
        return (from == Square0x88.E1 && state.getContent(from) == WHITE_KING && to == Square0x88.C1) ||
                (from == Square0x88.E8 && state.getContent(from) == BLACK_KING && to == Square0x88.C8);
    }

    private static boolean isKsCastling(State state, String move) {
        byte from = getFrom(move);
        byte to = getTo(move);
        return (from == Square0x88.E1 && state.getContent(from) == WHITE_KING && to == Square0x88.G1) ||
                (from == Square0x88.E8 && state.getContent(from) == BLACK_KING && to == Square0x88.G8);
    }

    private static boolean isDoublePush(State state, String move) {
        byte from = getFrom(move);
        byte to = getTo(move);
        return (Square0x88.getRank(from) == Rank._2 && Square0x88.getRank(to) == Rank._4 && state.getContent(from)== WHITE_PAWN) ||
                (Square0x88.getRank(from) == Rank._7 && Square0x88.getRank(to) == Rank._5 && state.getContent(from)== BLACK_PAWN);
    }

    private static Content getPromotion(String move) {
        boolean isWhite = move.charAt(3) == '8';
        switch (move.charAt(4)) {
            case KNIGHT:
                return isWhite ? WHITE_KNIGHT : BLACK_KNIGHT;
            case BISHOP:
                return isWhite ? WHITE_BISHOP : BLACK_BISHOP;
            case ROOK:
                return isWhite ? WHITE_ROOK : BLACK_ROOK;
            case QUEEN:
                return isWhite ? WHITE_QUEEN : BLACK_QUEEN;
            default:
                throw new IllegalArgumentException("Invalid promotion in move: " + move);
        }
    }

    public static String toStringLastMove(State state) {
        String fromTo = "" + Square0x88.toString(state.from) + Square0x88.toString(state.to);
        fromTo = fromTo.toLowerCase();
        if (state.promotion == null) {
            return fromTo;
        }
        switch (state.promotion) {
            case WHITE_KNIGHT:
            case BLACK_KNIGHT:
                return  fromTo + Lan.KNIGHT;
            case WHITE_BISHOP:
            case BLACK_BISHOP:
                return  fromTo + Lan.BISHOP;
            case WHITE_ROOK:
            case BLACK_ROOK:
                return fromTo + Lan.ROOK;
            case WHITE_QUEEN :
            case BLACK_QUEEN :
                return  fromTo + Lan.QUEEN;
            default:
                return  fromTo;
        }
    }

    private static byte getSquare(String move, int fileIndex, int rankIndex) {
        int file = move.charAt(fileIndex) - 'a';
        int rank = move.charAt(rankIndex) - '1';
        return Square0x88.from07(file, rank);
    }
}
