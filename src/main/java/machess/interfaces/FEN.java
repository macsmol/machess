package machess.interfaces;


import machess.Content;
import machess.PieceLists;
import machess.State;
import machess.board0x88.Square0x88;
import machess.board8x8.File;
import machess.board8x8.Rank;

import static machess.board0x88.Square0x88.NULL;

/**
 * Parses position Forsyth–Edwards Notation strings
 * https://www.chessprogramming.org/Forsyth-Edwards_Notation
 *
 * example FEN (after 1.e4):
 * rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1
 *
 */
public class FEN {
    private static final char WHITE_PAWN = 'P'; 
    private static final char WHITE_KNIGHT = 'N'; 
    private static final char WHITE_BISHOP = 'B'; 
    private static final char WHITE_ROOK = 'R'; 
    private static final char WHITE_QUEEN = 'Q'; 
    private static final char WHITE_KING = 'K';
    
    private static final char BLACK_PAWN = 'p';
    private static final char BLACK_KNIGHT = 'n';
    private static final char BLACK_BISHOP = 'b';
    private static final char BLACK_ROOK = 'r';
    private static final char BLACK_QUEEN = 'q';
    private static final char BLACK_KING = 'k';

    private static final String WHITE_TURN = "w";
    private static final String BLACK_TURN = "b";

    private static final String WHITE_KS_CASTLE_ALLOWED = "K";
    private static final String WHITE_QS_CASTLE_ALLOWED = "Q";
    private static final String BLACK_KS_CASTLE_ALLOWED = "k";
    private static final String BLACK_QS_CASTLE_ALLOWED = "q";



    public static State parse(String fen) {
        String[] strings = fen.split("\\s+");
        if (strings.length < 4) {
            throw new IllegalArgumentException("Invalid FEN string");
        }
        String boardStr = strings[0];
        String turnStr = strings[1];
        String castlingRightsStr = strings[2];
        String epSquareStr = strings[3];
        String halfMoveClockStr = strings.length >=5 ? strings[4] : "0";
        String fullMoveCounterStr = strings.length >=6 ? strings[5] : "777";

        short[] board0x88 = new short[128];
        PieceLists.Builder pieces = new PieceLists.Builder();
        {
            String[] rankStrings = boardStr.split("/");
            if (rankStrings.length != 8) {
                throw new IllegalStateException("Invalid number of ranks");
            }
            for (int i = Rank._1; i <= Rank._8 ; i++) {
                int rank = Rank._8 - i;
                int file = File.A;

                String rankStr = rankStrings[i];
                for (int j = 0; j < rankStr.length(); j++) {
                    char c = rankStr.charAt(j);
                    if (c >= '1' && c <= '8') {
                        file += Character.getNumericValue(c);
                        continue;
                    }
                    byte square0x88 = Square0x88.from07(file, rank);
                    switch (c) {
                        case BLACK_PAWN:
                            board0x88[square0x88] = Content.BLACK_PAWN.asByte;
                            pieces.addBlackPawn(square0x88);
                            break;
                        case BLACK_KNIGHT:
                            board0x88[square0x88] = Content.BLACK_KNIGHT.asByte;
                            pieces.addBlackKnight(square0x88);
                            break;
                        case BLACK_BISHOP:
                            board0x88[square0x88] = Content.BLACK_BISHOP.asByte;
                            pieces.addBlackBishop(square0x88);
                            break;
                        case BLACK_ROOK:
                            board0x88[square0x88] = Content.BLACK_ROOK.asByte;
                            pieces.addBlackRook(square0x88);
                            break;
                        case BLACK_QUEEN:
                            board0x88[square0x88] = Content.BLACK_QUEEN.asByte;
                            pieces.addBlackQueen(square0x88);
                            break;
                        case BLACK_KING:
                            board0x88[square0x88] = Content.BLACK_KING.asByte;
                            pieces.setBlackKing(square0x88);
                            break;
                        case WHITE_PAWN:
                            board0x88[square0x88] = Content.WHITE_PAWN.asByte;
                            pieces.addWhitePawn(square0x88);
                            break;
                        case WHITE_KNIGHT:
                            board0x88[square0x88] = Content.WHITE_KNIGHT.asByte;
                            pieces.addWhiteKnight(square0x88);
                            break;
                        case WHITE_BISHOP:
                            board0x88[square0x88] = Content.WHITE_BISHOP.asByte;
                            pieces.addWhiteBishop(square0x88);
                            break;
                        case WHITE_ROOK:
                            board0x88[square0x88] = Content.WHITE_ROOK.asByte;
                            pieces.addWhiteRook(square0x88);
                            break;
                        case WHITE_QUEEN:
                            board0x88[square0x88] = Content.WHITE_QUEEN.asByte;
                            pieces.addWhiteQueen(square0x88);
                            break;
                        case WHITE_KING:
                            board0x88[square0x88] = Content.WHITE_KING.asByte;
                            pieces.setWhiteKing(square0x88);
                            break;
                        default:
                            throw new IllegalArgumentException("Character '" + c + "' isn't a known piece.");
                   }
                   file++;
                }
            }
        }

        int flags = initFlags(turnStr, castlingRightsStr);
        byte enPassantSquare = initEnPassantSquare(epSquareStr);
        byte halfmoveClock = Byte.parseByte(halfMoveClockStr);
        int fullmoveCounter = Integer.parseInt(fullMoveCounterStr);

        return new State(board0x88, pieces.build(), (byte)flags, enPassantSquare, halfmoveClock, fullmoveCounter,
                NULL, NULL);
    }

    private static byte initEnPassantSquare(String epSquareStr) {
        if (epSquareStr.equals("-")) {
            return NULL;
        }
        int file = epSquareStr.toLowerCase().charAt(0) - 'a';
        if (file < File.A || file > File.H) {
            throw new IllegalArgumentException("Invalid file in en passant square: " + epSquareStr);
        }
        int rank = Character.getNumericValue(epSquareStr.charAt(1)) - 1;
        if (rank != Rank._3 && rank != Rank._6) {
            throw new IllegalArgumentException("Invalid rank in en passant square: " + epSquareStr);
        }
        return Square0x88.from07(file, rank);
    }

    private static int initFlags(String turnStr, String castlingRightsStr) {
        int flags = 0;
        if (turnStr.equals(WHITE_TURN)) {
            flags |= State.WHITE_TURN;
        }
        if (castlingRightsStr.contains(WHITE_KS_CASTLE_ALLOWED)) {
            flags |= State.WHITE_KS_CASTLE_POSSIBLE;
        }
        if (castlingRightsStr.contains(WHITE_QS_CASTLE_ALLOWED)) {
            flags |= State.WHITE_QS_CASTLE_POSSIBLE;
        }
        if (castlingRightsStr.contains(BLACK_KS_CASTLE_ALLOWED)) {
            flags |= State.BLACK_KS_CASTLE_POSSIBLE;
        }
        if (castlingRightsStr.contains(BLACK_QS_CASTLE_ALLOWED)) {
            flags |= State.BLACK_QS_CASTLE_POSSIBLE;
        }
        return flags;
    }
}
