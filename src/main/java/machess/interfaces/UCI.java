package machess.interfaces;

import machess.Lan;
import machess.Scorer;
import machess.State;

import java.util.Scanner;

public class UCI {
    private static final String POSITION = "position";
    private static final String STARTPOS = "startpos";
    private static final String MOVES = "moves";

    private static final String GO = "go";
    private static final String DEPTH = "depth";

    private static final String[] EMPTY_ARRAY = {};

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
            }
            else if (input.startsWith("tostr")) {
                System.out.println(state);
            }
        }

    }

    private void go(String input) {
        if (input.startsWith(DEPTH)) {
            int depth = Integer.parseInt(input.substring(DEPTH.length()).trim());
            Scorer.MoveScore score = Scorer.startMiniMax(state, depth);
            state = state.chooseMove(score.moveIndex);
            System.out.println("bestmove " + Lan.printLastMove(state));
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
