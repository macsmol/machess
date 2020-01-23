package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();
//        System.out.println("new game \n" + game);

//        game = game.fromPseudoLegalMove(Square.D1, Square.H3)
//				.fromPseudoLegalMove(Square.C8, Square.C1)
//
//				.fromPseudoLegalMove(Square.B1, Square.G3)
//				.fromPseudoLegalMove(Square.C1, Square.C2)
//				;
//        System.out.println("castle test \n" + game);

        for (int ply = 1; ply < 2; ply++) {
            Scorer.MoveScore bestMove = Scorer.miniMax(game);

            if (bestMove.moveIndex == -1) {
                String winMessage = bestMove.score > 0 ? "white win!" : "black win!";
                System.out.println("Game over: " + winMessage);
                break;
            }
            game = game.chooseMove(bestMove.moveIndex);
            System.out.println("*****************************************************");
            System.out.println("*****************************************************");
            System.out.println("*****************************************************");
            System.out.println("*****************************************************");
            System.out.println("*****************************************************");
            System.out.println("*****************************************************");
//            System.out.println(game);
        }
    }
}
