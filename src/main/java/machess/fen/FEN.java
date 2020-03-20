package machess.fen;


import machess.*;

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

    private static final char WHITE_TURN = 'w';
    private static final char BLACK_TURN = 'b';

    private static final char WHITE_KS_CASTLE_ALLOWED = 'K';
    private static final char WHITE_QS_CASTLE_ALLOWED = 'Q';
    private static final char BLACK_KS_CASTLE_ALLOWED = 'k';
    private static final char BLACK_QS_CASTLE_ALLOWED = 'q';



    public static State parse(String fen) {
        String[] strings = fen.split("\\s+");
        if (strings.length != 6) {
            throw new IllegalArgumentException("Invalid FEN string");
        }
//        rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1
        String boardStr = strings[0];
        String turnStr = strings[1];
        String castlingRightsStr = strings[2];
        String epSquareStr = strings[3];
        String halfMoveClockStr = strings[4];
        String fullMoveCounterStr = strings[5];

        short[] board = new short[Square.values().length];
        PieceLists.Builder pieces = new PieceLists.Builder();
        {
            String[] rankStrings = boardStr.split("/");
            if (rankStrings.length != 8) {
                throw new IllegalStateException("Invalid number of ranks");
            }
            for (int i = Rank._1; i <= Rank._8 ; i++) {
                int rank = Rank._8 - i;
                int file = File.A;

                String rankStr = rankStrings[rank];
                for (int j = 0; j < rankStr.length(); j++) {
                    char c = rankStr.charAt(j);
                    if (c >='1' && c <= '8') {
                        file += Character.getNumericValue(c);
                        System.out.println("file: " + file);
                        continue;
                    }
                    switch (c) {
                        case 'p':
                            break;
                        case 'n':
                            break;
                        case 'b':
                            break;
                        case 'r':
                            break;
                        case 'q':
                            break;
                        case 'k':
                            break;
                        case 'P':
                            break;
                        case 'N':
                            break;
                        case 'B':
                            break;
                        case 'R':
                            break;
                        case 'Q':
                            break;
                        case 'K':
                            break;
                        default:
                            throw new IllegalArgumentException("Character '" + c + "' isn't a known piece.");
                   }
                }
                System.out.println(rankStr);
//                board[board[Square.fromLegalInts(file, rank).ordinal()]] =

            }
        }


//        State(short[] board,
//        PieceLists pieces,
//        byte flags,
//        @Nullable Square enPassantSquare,
//        int plyNumber,
//        Square from,
//        Square to)
//        new State()
        return null;
    }

    private
}
