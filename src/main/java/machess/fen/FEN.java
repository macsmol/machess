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

    private static final String WHITE_TURN = "w";
    private static final String BLACK_TURN = "b";

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
                    if (c >= '1' && c <= '8') {
                        file += Character.getNumericValue(c);
                        System.out.println("file: " + file);
                        continue;
                    }
                    Square square = Square.fromLegalInts(file, rank);
                    switch (c) {
                        case BLACK_PAWN:
                            board[square.ordinal()] = Content.BLACK_PAWN.asByte;
                            pieces.addBlackPawn(square);
                            break;
                        case BLACK_KNIGHT:
                            board[square.ordinal()] = Content.BLACK_KNIGHT.asByte;
                            pieces.addBlackKnight(square);
                            break;
                        case BLACK_BISHOP:
                            board[square.ordinal()] = Content.BLACK_BISHOP.asByte;
                            pieces.addBlackBishop(square);
                            break;
                        case BLACK_ROOK:
                            board[square.ordinal()] = Content.BLACK_ROOK.asByte;
                            pieces.addBlackRook(square);
                            break;
                        case BLACK_QUEEN:
                            board[square.ordinal()] = Content.BLACK_QUEEN.asByte;
                            pieces.addBlackQueen(square);
                            break;
                        case BLACK_KING:
                            board[square.ordinal()] = Content.BLACK_KING.asByte;
                            pieces.setBlackKing(square);
                            break;
                        case WHITE_PAWN:
                            board[square.ordinal()] = Content.WHITE_PAWN.asByte;
                            pieces.addWhitePawn(square);
                            break;
                        case WHITE_KNIGHT:
                            board[square.ordinal()] = Content.WHITE_KNIGHT.asByte;
                            pieces.addWhiteKnight(square);
                            break;
                        case WHITE_BISHOP:
                            board[square.ordinal()] = Content.WHITE_BISHOP.asByte;
                            pieces.addWhiteBishop(square);
                            break;
                        case WHITE_ROOK:
                            board[square.ordinal()] = Content.WHITE_ROOK.asByte;
                            pieces.addWhiteRook(square);
                            break;
                        case WHITE_QUEEN:
                            board[square.ordinal()] = Content.WHITE_QUEEN.asByte;
                            pieces.addWhiteQueen(square);
                            break;
                        case WHITE_KING:
                            board[square.ordinal()] = Content.WHITE_KING.asByte;
                            pieces.setWhiteKing(square);
                            break;
                        default:
                            throw new IllegalArgumentException("Character '" + c + "' isn't a known piece.");
                   }
                }
            }
        }

        int flags = 0;
        if (turnStr == WHITE_TURN) {
            flags |= State.WHITE_TURN;
        }

//        if () {
//
//        }
        ////
//        private static final char WHITE_KS_CASTLE_ALLOWED = 'K';
//        private static final char WHITE_QS_CASTLE_ALLOWED = 'Q';
//        private static final char BLACK_KS_CASTLE_ALLOWED = 'k';
//        private static final char BLACK_QS_CASTLE_ALLOWED = 'q';
        ////
//TODO change these flags to isWHITE_KS_ALLOWED etc then you gain 2 bits
//        public static final int WHITE_KING_MOVED 		= 0x02;
//        public static final int BLACK_KING_MOVED 		= 0x04;
//        public static final int WHITE_KS_ROOK_MOVED 	= 0x08;
//        public static final int WHITE_QS_ROOK_MOVED 	= 0x10;
//        public static final int BLACK_KS_ROOK_MOVED 	= 0x20;
//        public static final int BLACK_QS_ROOK_MOVED 	= 0x40;

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
}
