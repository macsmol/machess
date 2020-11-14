package machess.interfaces;

import machess.*;

import java.util.Scanner;

import static machess.Utils.spaces;

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
    public static final String WHITE_TIME = "wtime";
    public static final String BLACK_TIME = "btime";
    public static final String WHITE_INCREMENT = "winc";
    public static final String BLACK_INCREMENT = "binc";

    public static final String BESTMOVE = "bestmove";

    public static final String QUIT = "quit";

    private State state;

    public void startEngine() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            tryToParseInput(scanner.nextLine());
        }
    }

    private void tryToParseInput(String input) {
        try {
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
            } else if (input.startsWith(QUIT)) {
                System.exit(0);
            }
        } catch (Exception ex) {
            System.out.println("Cannot parse input: " + input + " ex: "+ ex);
        }
    }

    private void go(String input) {
        if (input.startsWith(DEPTH)) {
            int depth = Integer.parseInt(input.substring(DEPTH.length()).trim());
            Scorer.Result result = Scorer.startMiniMax(state, depth);
            System.out.println(BESTMOVE + " " + result.pv.moves[0]);
        } else if (input.contains(WHITE_TIME)) {
            SuddenDeathWorker worker = new SuddenDeathWorker(input, state.test(State.WHITE_TURN));
            worker.doIterativeDeepening();
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
        System.out.println("id machess 1.0-SNAPSHOT_14.11.2020_23:18");
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

    private static String info(int nodesEvaluated, Scorer.PrincipalVariation pvLine, long elapsedMillis, int depth, int pvUpdates, int nodesPerSecond) {
        return spaces(UCI.INFO,
                UCI.NODES, Integer.toString(nodesEvaluated),
                UCI.PV, pvLine.toString(),
                UCI.TIME, Long.toString(elapsedMillis),
                UCI.DEPTH, Integer.toString(depth),
                UCI.NPS, Integer.toString(nodesPerSecond),
                "pvUpdates", Integer.toString(pvUpdates));
    }

    private class SuddenDeathWorker {

        private int whiteLeftMillis;
        private int blackLeftMillis;
        private int whiteIncrementMillis = 0;
        private int blackIncrementMillis = 0;
        private boolean whiteTurn;

        public SuddenDeathWorker(String timeParameters, boolean isWhiteTurn) {
            String[] timeTokens = timeParameters.split(" +");

            for (int i = 0; i < timeTokens.length; i += 2) {
                switch (timeTokens[i]) {
                    case WHITE_TIME:
                        whiteLeftMillis = Integer.parseInt(timeTokens[i + 1]);
                        break;
                    case BLACK_TIME:
                        blackLeftMillis = Integer.parseInt(timeTokens[i + 1]);
                        break;
                    case WHITE_INCREMENT:
                        whiteIncrementMillis = Integer.parseInt(timeTokens[i + 1]);
                        break;
                    case BLACK_INCREMENT:
                        blackIncrementMillis = Integer.parseInt(timeTokens[i + 1]);
                        break;
                }
            }
        }

        public void doIterativeDeepening() {
            // TODO this is veery basic iterative deepening
            String bestMove = "";
            long before = System.currentTimeMillis();
            long millisForMove = calcMillisForNextMove();
            long elapsedMillis = 0;
            for (int depth = 1; depth < Config.MAX_SEARCH_DEPTH; depth++) {
                Scorer.Result score = Scorer.startMiniMax(state, depth);

                elapsedMillis = System.currentTimeMillis() - before;
                System.out.println(info(score.nodesEvaluated, score.pv,
                        elapsedMillis, depth, score.pvUpdates,
                        Utils.calcNodesPerSecond(score.nodesEvaluated, elapsedMillis)));
                // terminal nodes will not throw npex because gui will not ask
                bestMove = score.pv.moves[0];
                if (elapsedMillis > millisForMove) {
                    break;
                }
            }
            System.out.println("bestmove " + bestMove);
        }

        private long calcMillisForNextMove() {
            // TODO improve it - this is very basic and probably not very smart
            return (whiteTurn ? whiteLeftMillis : blackLeftMillis) / Config.EXPECTED_TURNS_LEFT;
        }
    }
}
