package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();

        game = game
                .fromPseudoLegalMove(Square.B1, Square.B3)
                .fromPseudoLegalMove(Square.B8, Square.B6)
                .fromPseudoLegalMove(Square.C1, Square.C3)
                .fromPseudoLegalMove(Square.C8, Square.C6)
                .fromPseudoLegalMove(Square.D1, Square.D3)
                .fromPseudoLegalMove(Square.D8, Square.D6)
                .fromPseudoLegalMove(Square.F1, Square.F3)
                .fromPseudoLegalMove(Square.F8, Square.F6)
                .fromPseudoLegalMove(Square.G1, Square.G3)
                .fromPseudoLegalMove(Square.G8, Square.G3)
                .fromPseudoLegalMove(Square.B3, Square.F6)
        ;
        System.out.println("new game \n" + game);

        game.generateLegalMoves2();
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
