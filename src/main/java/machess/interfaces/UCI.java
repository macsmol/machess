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

    public static final String SCORE = "score";
    public static final String MATE_IN = "mate";
    public static final String CENTIPAWNS = "cp";

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
            ex.printStackTrace();
        }
    }

    private void go(String input) {
        if (input.startsWith(DEPTH)) {
            int depth = Integer.parseInt(input.substring(DEPTH.length()).trim());

            long before = System.currentTimeMillis();
            Scorer.Result result = Scorer.startMiniMax(state, depth);
            long elapsedMillis = System.currentTimeMillis() - before;

            System.out.println(info(result.nodesEvaluated, result.pv,
                    elapsedMillis, depth, result.pvUpdates,
                    Utils.calcNodesPerSecond(result.nodesEvaluated, elapsedMillis),
                    formatScore(result.score, state.test(State.WHITE_TURN))));

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

    private static String info(int nodesEvaluated, Scorer.PrincipalVariation pvLine, long elapsedMillis, int depth,
                               int pvUpdates, int nodesPerSecond, String scoreString) {
        return spaces(UCI.INFO,
                UCI.NODES, Integer.toString(nodesEvaluated),
                UCI.PV, pvLine.toString(),
                UCI.TIME, Long.toString(elapsedMillis),
                UCI.DEPTH, Integer.toString(depth),
                UCI.NPS, Integer.toString(nodesPerSecond),
                UCI.SCORE, scoreString,
                "pvUpdates", Integer.toString(pvUpdates)
        );
    }

    public static String formatScore(int score, boolean isWhiteTurn) {
        if (Scorer.scoreCloseToWinning(score)) {
            return UCI.MATE_IN + " " + fullMovesToMate(score, isWhiteTurn);
        }
        return UCI.CENTIPAWNS + " " + score;
    }

    static int fullMovesToMate(int actualScore, boolean isWhiteTurn) {
        int movesToMate = 0;

        int actualScoreAbsolute = Math.abs(actualScore);
        int successiveMatingScores = Scorer.MAXIMIZING_WIN;
        while (successiveMatingScores > actualScoreAbsolute) {
            successiveMatingScores = Scorer.discourageLaterWin(Scorer.discourageLaterWin(successiveMatingScores));
            movesToMate++;
        }
        // negative values when engine is loosing
        int signum = actualScore > 0 == isWhiteTurn ? 1 : -1;
        return movesToMate * signum;
    }

    private class SuddenDeathWorker {

        private int whiteLeftMillis;
        private int blackLeftMillis;
        private int whiteIncrementMillis = 0;
        private int blackIncrementMillis = 0;
        private boolean whiteTurn;

        public SuddenDeathWorker(String timeParameters, boolean whiteTurn) {
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
            this.whiteTurn = whiteTurn;
        }

        public void doIterativeDeepening() {
            // TODO this is veery basic iterative deepening
            String bestMove = "";
            long millisForMove = calcMillisForNextMove();
            long before = System.currentTimeMillis();

            for (int depth = 1; depth < Config.MAX_SEARCH_DEPTH; depth++) {
                Scorer.Result result = Scorer.startMiniMax(state, depth);

                long elapsedMillis = System.currentTimeMillis() - before;
                System.out.println(info(result.nodesEvaluated, result.pv,
                        elapsedMillis, depth, result.pvUpdates,
                        Utils.calcNodesPerSecond(result.nodesEvaluated, elapsedMillis),
                        formatScore(result.score, state.test(State.WHITE_TURN))));

                bestMove = result.pv.moves[0];
                if (elapsedMillis > millisForMove) {
                    break;
                }
                if (result.oneLegalMove) {
                    break;
                }
                if (Scorer.scoreCloseToWinning(result.score)) {
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
