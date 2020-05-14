package machess.interfaces;

import machess.Lan;
import machess.Scorer;
import machess.State;

import java.util.Scanner;

public class UCI {
    public static final String POSITION = "position";
    public static final String STARTPOS = "startpos";
    public static final String MOVES = "moves";

    public static final String INFO = "info";
    // principal variation
    public static final String PV = "pv";
    // time in millis
    public static final String TIME = "time";
    public static final String NODES = "nodes";
    // nodes per second
    public static final String NPS = "nps";

    public static final String GO = "go";
    public static final String DEPTH = "depth";

    public static final String QUIT = "quit";

    private State state;

    public void startEngine() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            if (input.startsWith("uci")) {
                enterUci();
            } else if (input.startsWith("isready")) {
                isReady();
            } else if (input.startsWith("ucinewgame")) {
                newGame();
            } else if (input.startsWith(POSITION)) {
                setPosition(input.substring(POSITION.length()).trim());
            } else if (input.startsWith(GO)) {
                go(input.substring(GO.length()).trim());
            } else if (input.startsWith("tostr")) {
                System.out.println(state);
            }else if (input.startsWith(QUIT)) {
                System.exit(0);
            }
        }

    }

    private void go(String input) {
        if (input.startsWith(DEPTH)) {
            int depth = Integer.parseInt(input.substring(DEPTH.length()).trim());
            Scorer.MoveScore score = Scorer.startMiniMax(state, depth);
            State bestMove = state.chooseMove(score.moveIndex);
            System.out.println("bestmove " + Lan.printLastMove(bestMove));
        }
    }

    private void setPosition(String positionCommand) {
        int movesIdx = positionCommand.indexOf(MOVES);
        if (movesIdx == -1) {
            state = parseState(positionCommand);
        } else {
            state = parseState(positionCommand.substring(0, movesIdx));

            String[] moves = positionCommand
                    .substring(movesIdx + MOVES.length())
                    .trim()
                    .toLowerCase()
                    .split(" ");

            for (String move : moves) {
                state = Lan.move(state, move);
            }
        }
    }

    private State parseState(String positionWithoutMoves) {
        if (positionWithoutMoves.startsWith(STARTPOS)) {
            return new State();
        }
        return FEN.parse(positionWithoutMoves);
    }

    private void enterUci() {
        System.out.println("id machess 0.1");
        System.out.println("id author Maciej Smolczewski");
        presentOptions();
        System.out.println("uciok");
    }

    private void isReady() {
        System.out.println("readyok");
    }

    private void newGame() {
    }

    private void presentOptions() {
    }
}
