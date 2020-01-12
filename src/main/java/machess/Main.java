package machess;

public class Main {
    public static void main(String[] args) {
        int [] moves = new int [Config.DEFAULT_MOVES_CAPACITY];
        State game = new State();

        game = game
                .fromPseudoLegalMove(Square.A1, Square.E3)
//                .fromPseudoLegalPawnDoublePush(Square.E2, Square.E4, Square.E3)
//                .fromPseudoLegalMove(Square.E7, Square.E6)
//                .fromPseudoLegalMove(Square.E1, Square.E3)

        ;
        System.out.println("new game \n" + game);


        game.generateLegalMoves2(moves);
game.makePseudoLegalMove(moves[4]);

        System.out.println("tada!" + game);
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
