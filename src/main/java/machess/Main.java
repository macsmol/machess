package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();
        System.out.println("new game \n" + game);

        for (int i = 0; i < 30; i++) {
            long before = System.currentTimeMillis();
            NegaMaxScorer.MoveScore bestMove = NegaMaxScorer.negamax(game);
            long after = System.currentTimeMillis();
            System.out.println(bestMove + " time elapsed: " + (after - before));

            game = game.makeMove(bestMove.moveIndex);
            System.out.println(game);
        }
    }
}
