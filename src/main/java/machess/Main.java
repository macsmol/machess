package machess;

import machess.interfaces.UCI;

public class Main {
    public static void main(String[] args) {
//        startUci();
        play();
    }

    private static void startUci() {
        UCI uci = new UCI();
        uci.startEngine();
    }

    private static void play() {
        State game = new State();
        System.out.println("new game \n" + game);
        for (int ply = 1; ply <= 150; ply++) {
            Scorer.MoveScore bestMove = Scorer.startMiniMax(game, Config.SEARCH_DEPTH);

            if (bestMove.moveIndex == -1) {
                String winMessage = bestMove.score > 0 ? "white win!" : "black win!";
                System.out.println("Game over: " + winMessage);
                break;
            }
            game = game.chooseMove(bestMove.moveIndex);
            System.out.println(game);
        }
    }
}
