package machess;

public class Main {
    public static void main(String[] args) {
        State game = new State();
        System.out.println("new game \n" + game);

        for (int i = 0; i < 150; i++) {
            Scorer.MoveScore bestMove = Scorer.miniMax(game);

            if(bestMove.moveIndex==-1) {
                String winMessage = bestMove.score > 0 ? "white win!" : "black win!";
                System.out.println("Game over: " + winMessage);
                break;
            }
            game = game.makeMove(bestMove.moveIndex);
            System.out.println(game);
        }
    }
}
