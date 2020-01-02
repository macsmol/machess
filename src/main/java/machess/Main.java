package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();
        System.out.println("new game \n" + game);

        for (int i = 0; i < 110; i++) {
            NegaMaxScorer.MoveScore bestMove = NegaMaxScorer.negamax(game);

            game = game.makeMove(bestMove.moveIndex);
            System.out.println(game);
        }
    }
}
