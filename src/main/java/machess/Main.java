package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();
        System.out.println("new game \n" + game);

        game = game.fromPseudoLegalMove(Square.A1, Square.D8)
				.fromPseudoLegalMove(Square.A7, Square.A6)

				.fromPseudoLegalMove(Square.D8, Square.C8)
				.fromPseudoLegalMove(Square.B8, Square.C6)

				.fromPseudoLegalMove(Square.C8, Square.C7)
				;

        System.out.println("castle test \n" + game);
        for (int ply = 1; ply < 150; ply++) {
            Scorer.MoveScore bestMove = Scorer.miniMax(game);

            if(bestMove.moveIndex==-1) {
                String winMessage = bestMove.score > 0 ? "white win!" : "black win!";
                System.out.println("Game over: " + winMessage);
                break;
            }
            game = game.chooseMove(bestMove.moveIndex);
            System.out.println(game);
        }
    }
}
