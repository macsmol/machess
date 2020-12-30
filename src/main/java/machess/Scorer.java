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

	private static volatile boolean interrupt;

	public static Result startAlphaBeta(State rootState, int depth, Instant finishTime, Line leftmostLine, Line debugLine) {
		interrupt = false;
		nodesEvaluatedInPly = 0;
		Line pvLine = Line.empty();
		Line pvSubLine = Line.empty();
		List<State> moves = rootState.generateLegalMoves();
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;

		final boolean maximizing = rootState.test(State.WHITE_TURN);
		int resultScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

		if (moves.isEmpty()) {
			return new Result(terminalNodeScore(rootState, 0), pvLine, nodesEvaluatedInPly, false);
		}

		reorderMoves(moves, leftmostLine, 1);
		for (State move : moves) {
			int currScore;

			try {
				currScore = alphaBeta(move, depth - 1, alpha, beta, leftmostLine, pvSubLine, finishTime, debugLine, 1);
			} catch (Throwable error) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("ROOT STATE: " + rootState);
				throw error;
			}

			if (maximizing) {
				if (currScore > resultScore) {
					pvLine.updateSubline(pvSubLine, move);
					System.out.println(spaces(UCI.INFO, UCI.PV, pvLine.toString(), UCI.SCORE, UCI.formatScore(currScore,true)));
					resultScore = currScore;
				}
				alpha = Math.max(currScore, alpha);
			} else {
				if (currScore < resultScore) {
					pvLine.updateSubline(pvSubLine, move);
					System.out.println(spaces(UCI.INFO, UCI.PV, pvLine.toString(), UCI.SCORE, UCI.formatScore(currScore,false)));
					resultScore = currScore;
				}
				beta = Math.min(currScore, beta);
			}
			if (alpha >= beta) {
				break;
			}
			if (Instant.now().isAfter(finishTime)) {
				// TODO return partial results after passing best PVs from previous iterative deepening iterations
				return new Result(0, null, nodesEvaluatedInPly, false);
			}
			if (nextMoveWins(currScore)) {
				break;
			}
		}
		return new Result(resultScore, pvLine, nodesEvaluatedInPly, moves.size() == 1);
	}

	public static void terminate() {
		interrupt = true;
	}

	/**
	 *
	 * @param leftmostLine - line to be examined first (obtained from previous ID)
	 * @param principalVariation - principal variation line - https://www.chessprogramming.org/Principal_Variation
	 *                           propagated up to the root node
	 * @param debugLine - nullable line that when matched should display additional info about scores of it's children.
	 * @param ply - same as depth but counts up. In other words ply distance from the root node
	 * @return score
	 */
	private static int alphaBeta(State state, int depth, int alpha, int beta, Line leftmostLine, Line principalVariation,
								 Instant finishTime, Line debugLine, int ply) {
		boolean debugChildrenScores = false;
		if (debugLine != null) {
			debugLine.isMoveMatched(state, ply);
			if (debugLine.isLineMatched()) {
				System.out.println("\tFound debug line: " + debugLine);
				System.out.println("alpha: " + alpha + " beta: " + beta);
				System.out.println("State is: " + state);
				debugChildrenScores = true;
			}
		}
		boolean maximizingTurn = state.test(State.WHITE_TURN);
		if (depth <= 0) {
			principalVariation.movesCount = 0;
			return evaluate(state, ply);
		}

		int resultScore = maximizingTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		Line pvSubLine = Line.empty();

		List<State> moves = state.generateLegalMoves();

		if (moves.isEmpty()) {
			principalVariation.movesCount = 0;
			return terminalNodeScore(state, ply);
		}
		reorderMoves(moves, leftmostLine, ply + 1);
		for (State move : moves) {
			int currScore;
			try {
				currScore = alphaBeta(move, depth - 1, alpha, beta, leftmostLine, pvSubLine, finishTime, debugLine, ply + 1);
			} catch (Throwable error) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("PLY: " + ply + " STATE: " + state);
				throw error;
			}
			if (debugChildrenScores) {
				System.out.println("\t" + Lan.toStringLastMove(move) + ": " + currScore);
			}
			if (maximizingTurn) {
				if (currScore > resultScore) {
					principalVariation.updateSubline(pvSubLine, move);
					resultScore = currScore;
				}
				alpha = Math.max(currScore, alpha);
			} else {
				if (currScore < resultScore) {
					principalVariation.updateSubline(pvSubLine, move);
					resultScore = currScore;
				}
				beta = Math.min(currScore, beta);
			}
			if (alpha >= beta) {
				break;
			}
			// TODO if (Utils.nanoNow().isAfter(finishTime)) {
			if (Instant.now().isAfter(finishTime)) {
				break;
			}
			if (interrupt) {
				break;
			}
		}
		return resultScore;
	}

	private static void reorderMoves(List<State> moves, Line leftmostLine, int ply) {
		for (int i = 0; i < moves.size(); i++) {
			State move = moves.get(i);
			if (leftmostLine.isMoveMatched(move, ply)) {
				State tmp = moves.get(0);
				moves.set(0, move);
				moves.set(i, tmp);
				break;
			}
		}
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
		final int onePly = 1;
		return Math.abs(score) == MAXIMIZING_WIN - onePly;
	}

	public static class Result {
		public final int score;
		public final Line pv;
		public final int nodesEvaluated;

		// skip iterative deepening in this case
		public final boolean oneLegalMove;

		public Result(int score, Line pvLine, int nodesEvaluated,
					  boolean oneLegalMove) {
			this.score = score;
			this.pv = pvLine;
			this.nodesEvaluated = nodesEvaluated;
			this.oneLegalMove = oneLegalMove;
		}
	}

	public static int evaluate(State state, int ply) {
		nodesEvaluatedInPly++;
		if (nodesEvaluatedInPly % Config.NODES_LOGGING_PERIOD == 0) {
			System.out.println(spaces(UCI.INFO, UCI.NODES, Integer.toString(nodesEvaluatedInPly)));
		}
		int legalMoves = state.countLegalMoves();
		if (legalMoves == 0) {
			return terminalNodeScore(state, ply);
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

	private static int terminalNodeScore(State state, int ply) {
		boolean maximizingTurn = state.test(State.WHITE_TURN);
		if (maximizingTurn) {
			if (state.isKingInCheck()) {
				return MINIMIZING_WIN + ply;
			} else {
				return DRAW;
			}
		} else {
			if (state.isKingInCheck()) {
				return MAXIMIZING_WIN - ply;
			} else {
				return DRAW;
			}
		}
	}
}
