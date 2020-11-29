package machess.interfaces;

import machess.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

import static machess.Utils.*;

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
    public static final String MOVESTOGO = "movestogo";


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
        SuddenDeathWorker worker = new SuddenDeathWorker(input, state.test(State.WHITE_TURN));
        worker.doIterativeDeepening();
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
        System.out.println("id machess 1.0-SNAPSHOT_22.11.2020");
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

        private int whiteLeftMillis = Integer.MAX_VALUE;
        private int blackLeftMillis = Integer.MAX_VALUE;
        private int whiteIncrementMillis = 0;
        private int blackIncrementMillis = 0;
        private int givenMovesToGo = -1;
        private int maxDepth = Config.MAX_SEARCH_DEPTH;
        private boolean whiteTurn;

        public SuddenDeathWorker(String timeParameters, boolean whiteTurn) {
            String[] tokens = timeParameters.split(" +");

            for (int i = 0; i < tokens.length; i += 2) {
                switch (tokens[i]) {
                    case WHITE_TIME:
                        whiteLeftMillis = Integer.parseInt(tokens[i + 1]);
                        break;
                    case BLACK_TIME:
                        blackLeftMillis = Integer.parseInt(tokens[i + 1]);
                        break;
                    case WHITE_INCREMENT:
                        whiteIncrementMillis = Integer.parseInt(tokens[i + 1]);
                        break;
                    case BLACK_INCREMENT:
                        blackIncrementMillis = Integer.parseInt(tokens[i + 1]);
                        break;
                    case MOVESTOGO:
                        givenMovesToGo = Integer.parseInt(tokens[i + 1]);
                        break;
                    case DEPTH:
                        maxDepth  = Math.min(Integer.parseInt(tokens[i + 1]), Config.MAX_SEARCH_DEPTH);
                        break;
                }
            }
            this.whiteTurn = whiteTurn;
        }

        public void doIterativeDeepening() {
            String bestMove = "";
            Instant before = Instant.now();
            Instant finishTime = before.plus(calcTimeForNextMove());

            for (int depth = 1; depth <= maxDepth; depth++) {
                Scorer.Result result = Scorer.startMiniMax(state, depth, finishTime);
                if (result.pv == null) { // when runs out of time returns null pv
                    break;
                }

                Duration elapsedTime = Duration.between(before, Instant.now());
                System.out.println(info(result.nodesEvaluated, result.pv,
                        elapsedTime.toMillis(), depth, result.pvUpdates,
                        calcNodesPerSecond(result.nodesEvaluated, elapsedTime.toNanos()),
                        formatScore(result.score, state.test(State.WHITE_TURN))));

                bestMove = result.pv.moves[0];
                if (Instant.now().isAfter(finishTime)) {
                    break;
                }
                if (result.oneLegalMove) {
                    break;
                }
                if (Scorer.scoreCloseToWinning(result.score)) { // mating line was found no need to go deeper
                    break;
                }
            }
            System.out.println("bestmove " + bestMove);
        }

        private Duration calcTimeForNextMove() {
            int fullMovesToGo = givenMovesToGo == -1 ? Config.EXPECTED_FULLMOVES_LEFT : givenMovesToGo;
            long millis = (whiteTurn ? whiteLeftMillis : blackLeftMillis) / fullMovesToGo;
            return Duration.of(millis, ChronoUnit.MILLIS);
        }
    }
}
