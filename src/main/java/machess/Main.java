package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();
        System.out.println("new game \n" + game);

        for (int i = 0; i < 150; i++) {
            // TODO bug: at ply 65 queen rather than checkmate does a very bad move...
            NegaMaxScorer.MoveScore bestMove = NegaMaxScorer.negamax(game, i % 2 != 0);

            game = game.makeMove(bestMove.moveIndex);
            System.out.println(game);
        }
    }
}
