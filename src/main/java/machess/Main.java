package machess;

public class Main {
    public static void main(String[] args) {
        int [] moves = new int [Config.DEFAULT_MOVES_CAPACITY];
        State game = new State();

        game = game
                .fromPseudoLegalMove(Square.C1, Square.C3)
                .fromPseudoLegalMove(Square.C7, Square.C2)

                .fromPseudoLegalPawnDoublePush(Square.E2, Square.E4, Square.E3)
        ;
        System.out.println("new game \n" + game);
        game.generateLegalMoves2(moves);
        int moveIdx = 21;
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
