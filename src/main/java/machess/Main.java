package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();
        System.out.println("new game \n" + game);
        for (int i = 0; i < 150; i++) {
            Scorer.MoveScore bestMove = Scorer.miniMax(game);

            if (bestMove.moveAsInt == Move.NULL) {
                String winMessage = bestMove.score > 0 ? "white win!" : "black win!";
                System.out.println("Game over: " + winMessage);
                break;
            }
            game.makePseudoLegalMove(bestMove.moveAsInt);
            System.out.println(game);
        }
    }
}
