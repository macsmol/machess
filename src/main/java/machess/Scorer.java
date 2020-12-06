package machess;


import machess.interfaces.UCI;

import java.time.Instant;
import java.util.*;

import static machess.Utils.spaces;

public class Scorer {
	public static final int MAXIMIZING_WIN = 1_000_000;
	public static final int MINIMIZING_WIN = -1_000_000;
	private static final int DRAW = 0;

	private static final int MATERIAL_PAWN 		= 100;
	private static final int MATERIAL_KNIGHT 	= 300;
	private static final int MATERIAL_BISHOP 	= 300;
	private static final int MATERIAL_ROOK		= 500;
	private static final int MATERIAL_QUEEN		= 900;

	//  Score more than overwhelming difference in material
	public static final int SCORE_CLOSE_TO_WIN = 2 * (9 * MATERIAL_QUEEN +  2 * MATERIAL_ROOK +
			2 * MATERIAL_BISHOP + 2 * MATERIAL_KNIGHT);

	private static final int LEGAL_MOVE_SCORE = 5;

	private static int nodesEvaluatedInPly = 0;
	private static int pvUpdates = 0;

	private static volatile boolean interrupt;

	public static Result startMiniMax(State rootState, int depth, Instant finishTime) {
		return startMiniMax(rootState, depth, finishTime, null);
	}

	public static Result startMiniMax(State rootState, int depth, Instant finishTime, Line debugLine) {
		interrupt = false;
		nodesEvaluatedInPly = 0;
		pvUpdates = 0;
		Line pvLine = new Line();
		Line pvSubLine = new Line();
		List<State> moves = rootState.generateLegalMoves();

		boolean maximizing = rootState.test(State.WHITE_TURN);
		int resultScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

		if (moves.isEmpty()) {
			return new Result(terminalNodeScore(rootState), pvLine, nodesEvaluatedInPly, pvUpdates, false);
		}

		for (State move : moves) {
			int currScore;

			try {
				currScore = discourageLaterWin(miniMax(move, depth - 1, pvSubLine, finishTime, debugLine, 1));
			} catch (Throwable ae) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("ROOT STATE: " + rootState);
				throw ae;
			}

			if (maximizing) {
				if (currScore > resultScore) {
					pvLine.updateSubline(pvSubLine, move);
					System.out.println(spaces(UCI.INFO, UCI.PV, pvLine.toString()));
					resultScore = currScore;
				}
			} else {
				if (currScore < resultScore) {
					pvLine.updateSubline(pvSubLine, move);
					System.out.println(spaces(UCI.INFO, UCI.PV, pvLine.toString()));
					resultScore = currScore;
				}
			}
			if (Instant.now().isAfter(finishTime)) {
				// TODO return partial results after passing best PVs from previous iterative deepening iterations
				return new Result(0, null, nodesEvaluatedInPly, pvUpdates, false);
			}
			if (nextMoveWins(currScore)) {
				break;
			}
		}
		return new Result(resultScore, pvLine, nodesEvaluatedInPly, pvUpdates, moves.size() == 1);
	}

	public static void terminate() {
		interrupt = true;
	}

	/**
	 *
	 * @param principalVariation - principal variation line - https://www.chessprogramming.org/Principal_Variation
	 *                           propagated up to the root node
	 * @param debugLine - nullable line that when matched should display additional info about scores of it's children.
	 * @param ply - same as depth but counts up. In other words ply distance from the root node
	 * @return score
	 */
	private static int miniMax(State state, int depth, Line principalVariation, Instant finishTime, Line debugLine, int ply) {
		boolean debugChildrenScores = false;
		if (debugLine != null) {
			debugLine.isMoveMatched(state, ply);
			if (debugLine.isLineMatched()) {
				System.out.println("\tFound debug line: " + debugLine);
				System.out.println("State is: " + state);
				debugChildrenScores = true;
			}
		}
		boolean maximizingTurn = state.test(State.WHITE_TURN);
		if (depth <= 0) {
			principalVariation.movesCount = 0;
			return evaluate(state);
		}

		int resultScore = maximizingTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		Line pvSubLine = new Line();

		List<State> moves = state.generateLegalMoves();

		if (moves.isEmpty()) {
			principalVariation.movesCount = 0;
			return terminalNodeScore(state);
		}
		for (State move : moves) {
			int currScore;
			try {
				currScore = discourageLaterWin(miniMax(move, depth - 1, pvSubLine, finishTime, debugLine, ply + 1));
			} catch (Throwable ae) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("PLY: " + ply + " STATE: " + state);
				throw ae;
			}
			if (debugChildrenScores) {
				System.out.println("\t" + Lan.toStringLastMove(move) + ": " + normalize(currScore, ply));
			}
			if (maximizingTurn) {
				if (currScore > resultScore) {
					principalVariation.updateSubline(pvSubLine, move);
					resultScore = currScore;
				}
			} else {
				if (currScore < resultScore) {
					principalVariation.updateSubline(pvSubLine, move);
					resultScore = currScore;
				}
			}
			if (Instant.now().isAfter(finishTime)) {
				break;
			}
			if (interrupt) {
				break;
			}
		}
		return resultScore;
	}


	public static long perft(State state, int depth) {
		long movesCount = 0;
		if (depth == 0) {
			return 1;
		}
		for (State child : state.generateLegalMoves()) {
			movesCount += perft(child, depth - 1);
		}
		return movesCount;
	}

	public static void perftDivide(State state, int depth) {
		List<State> legalMoves = state.generateLegalMoves();
		if (depth < 1) {
			System.out.println("nothing to divide");
			return;
		}
		System.out.println("divide(" + depth + "):");
		for (State child : legalMoves) {
			long movesCount = perft(child, depth - 1);
			System.out.println(Lan.toStringLastMove(child) + " " + movesCount);
		}
	}

	public static boolean scoreCloseToWinning(int score) {
		return Math.abs(score) > Scorer.SCORE_CLOSE_TO_WIN;
	}

	private static boolean nextMoveWins(int score) {
		return Math.abs(score) >= discourageLaterWin(MAXIMIZING_WIN);
	}

	/**
	 * Because of discourageLaterWin() same positions occurring deeper in the search tree will have different scores.
	 * Call this on them to make them appear as seeb from the root position.
	 */
	private static int normalize(int score, int ply) {
		for (int i = 0; i < ply; i++) {
			score = discourageLaterWin(score);
		}
		return score;
	}

	/**
	 * Decreases the score.
	 * call this on child nodes to encourage choosing earlier wins.
	 */
	public static int discourageLaterWin(int score) {
		return score * 127 / 128;
	}

	public static class Result {
		public final int score;
		public final Line pv;
		public final int nodesEvaluated;

		public final int pvUpdates;

		// skip iterative deepening in this case
		public final boolean oneLegalMove;

		public Result(int score, Line pvLine, int nodesEvaluated, int pvUpdates,
					  boolean oneLegalMove) {
			this.score = score;
			this.pv = pvLine;
			this.nodesEvaluated = nodesEvaluated;
			this.pvUpdates = pvUpdates;
			this.oneLegalMove = oneLegalMove;
		}
	}

	public static int evaluate(State state) {
		nodesEvaluatedInPly++;
		if (nodesEvaluatedInPly % Config.LOG_NODES_EVALUATED_DELAY == 0) {
			System.out.println(spaces(UCI.INFO, UCI.NODES, Integer.toString(nodesEvaluatedInPly)));
		}
		int legalMoves = state.countLegalMoves();
		if (legalMoves == 0) {
			int terminalNodeScore = terminalNodeScore(state);
			return terminalNodeScore;
		}
		int materialScore = evaluateMaterialScore(state);
		int mobilityScore = mobilityScore(legalMoves, state);

		return materialScore + mobilityScore;
	}

	private static int mobilityScore(int currSideLegalMoves, State state) {
		int otherSideLegalMoves = state.countOtherSideLegalMoves();
		if (state.test(State.WHITE_TURN)) {
			return (currSideLegalMoves - otherSideLegalMoves) * LEGAL_MOVE_SCORE;
		}
		return (otherSideLegalMoves - currSideLegalMoves) * LEGAL_MOVE_SCORE;
	}

	private static int evaluateMaterialScore(State state) {
		int whiteMaterialScore = evaluateMaterialScore(state.pieces, State.WHITE);
		int blackMaterialScore = evaluateMaterialScore(state.pieces, State.BLACK);

		return whiteMaterialScore - blackMaterialScore;
	}

	private static int evaluateMaterialScore(PieceLists pieces, boolean whitePieces) {
		int score = 0;
		byte piecesCount = whitePieces ? pieces.whitePawnsCount : pieces.blackPawnsCount;
			score += MATERIAL_PAWN * piecesCount;
		piecesCount = whitePieces ? pieces.whiteKnightsCount : pieces.blackKnightsCount;
			score += MATERIAL_KNIGHT * piecesCount;
		piecesCount = whitePieces ? pieces.whiteBishopsCount : pieces.blackBishopsCount;
			score += MATERIAL_BISHOP * piecesCount;
		piecesCount = whitePieces ? pieces.whiteRooksCount : pieces.blackRooksCount;
			score += MATERIAL_ROOK * piecesCount;
		piecesCount = whitePieces ? pieces.whiteQueensCount : pieces.blackQueensCount;
			score += MATERIAL_QUEEN * piecesCount;

		return score;
	}

//	private static int evaluateMaterialScore(PieceLists pieces, boolean whitePieces) {
//		int score = 0;
//		// Squares array will be useful after introducing piece-square tables
//		Square [] piecesOfOneType = whitePieces ? pieces.whitePawns : pieces.blackPawns;
//		byte piecesCount = whitePieces ? pieces.whitePawnsCount : pieces.blackPawnsCount;
//		for (int i = 0; i < piecesCount; i++) {
//			score += MATERIAL_PAWN;
//		}
//		piecesOfOneType = whitePieces ? pieces.whiteKnights : pieces.blackKnights;
//		piecesCount = whitePieces ? pieces.whiteKnightsCount : pieces.blackKnightsCount;
//		for (int i = 0; i < piecesCount; i++) {
//			score += MATERIAL_KNIGHT;
//		}
//		piecesOfOneType = whitePieces ? pieces.whiteBishops : pieces.blackBishops;
//		piecesCount = whitePieces ? pieces.whiteBishopsCount : pieces.blackBishopsCount;
//		for (int i = 0; i < piecesCount; i++) {
//			score += MATERIAL_BISHOP;
//		}
//		piecesOfOneType = whitePieces ? pieces.whiteRooks : pieces.blackRooks;
//		piecesCount = whitePieces ? pieces.whiteRooksCount : pieces.blackRooksCount;
//		for (int i = 0; i < piecesCount; i++) {
//			score += MATERIAL_ROOK;
//		}
//		piecesOfOneType = whitePieces ? pieces.whiteQueens : pieces.blackQueens;
//		piecesCount = whitePieces ? pieces.whiteQueensCount : pieces.blackQueensCount;
//		for (int i = 0; i < piecesCount; i++) {
//			score += MATERIAL_QUEEN;
//		}
//
//		return score;
//	}

	private static int terminalNodeScore(State state) {
		boolean maximizingTurn = state.test(State.WHITE_TURN);
		if (maximizingTurn) {
			if (state.isKingInCheck()) {
				return MINIMIZING_WIN;
			} else {
				return DRAW;
			}
		} else {
			if (state.isKingInCheck()) {
				return MAXIMIZING_WIN;
			} else {
				return DRAW;
			}
		}
	}


	/**
	 * Represents sequence of moves in LAN format eg.
	 * e2e4 e7e6
	 */
	public static class Line {
		public String [] moves = new String[Config.MAX_SEARCH_DEPTH];
		public int movesCount = 0;
		public int movesMatched = 0;

		private void updateSubline(Line newSubLine, State move) {
			pvUpdates++;
			moves[0] = Lan.toStringLastMove(move);

			System.arraycopy(newSubLine.moves, 0, moves, 1, newSubLine.movesCount);
			movesCount = newSubLine.movesCount + 1;
		}

		public Line () {
		}

		public Line (String moves) {
			String [] movesSplit = moves.split(" +");
			this.moves = movesSplit;
			this.movesCount = movesSplit.length;
		}

		public void isMoveMatched(State move, int ply) {
			if (ply - 1 == movesMatched // so that we match the move only at desired level
					&& movesMatched < moves.length && moves[movesMatched].equals(Lan.toStringLastMove(move))) {
				movesMatched++;
			}
		}

		public boolean isLineMatched() {
			if (movesCount == movesMatched) {
				// once int overflows it could return false positives
				movesMatched++;
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			StringJoiner sb = new StringJoiner(" ");
			for (int i = 0; i < movesCount; i++) {
				sb.add(moves[i]);
			}
			return sb.toString();
		}
	}
}
