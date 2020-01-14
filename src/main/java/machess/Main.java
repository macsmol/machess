package machess;

public class Main {
    public static void main(String[] args) {
        int [] moves = new int [Config.DEFAULT_MOVES_CAPACITY];
        State game = new State();

        game = game
                .fromPseudoLegalMove(Square.B1, Square.D7)
                .fromPseudoLegalMove(Square.F7, Square.F6)

                .fromPseudoLegalMove(Square.D1, Square.A4)
                .fromPseudoLegalMove(Square.H7, Square.H6)

                .fromPseudoLegalMove(Square.D7, Square.F6)//double-check!
        ;
        System.out.println("new game \n" + game);
        game.generateLegalMoves(moves);
        int moveIdx = 0;
        game.makePseudoLegalMove(moves[moveIdx]);
        System.out.println("tada!" + game);

        game.unmakePseudoLegalMove(moves[moveIdx]);
        System.out.println("adat!" + game);
//        for (int i = 0; i < 150; i++) {
//            Scorer.MoveScore bestMove = Scorer.miniMax(game);
//
//            if(bestMove.moveIndex==-1) {
//                String winMessage = bestMove.score > 0 ? "white win!" : "black win!";
//                System.out.println("Game over: " + winMessage);
//                break;
//            }
//            game = game.chooseMove(bestMove.moveIndex);
//            System.out.println(game);
//        }
    }
}
