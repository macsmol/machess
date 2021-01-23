package machess;


import machess.interfaces.UCI;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static machess.Utils.spaces;

public class Scorer {
	public static final int LOST = -1_000_000;
	private static final int DRAW = 0;

	/**
	 * Can't just use Integer.MIN_VALUE because it overflows in negamax after changing sign.
	 */
	private static final int MINUS_INFINITY = -Integer.MAX_VALUE;
	private static final int INFINITY = Integer.MAX_VALUE ;

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
		if (debugLine.movesCount > 0) {
			System.out.println("debug line " + debugLine);
		}
		interrupt = false;
		nodesEvaluatedInPly = 0;
		Line pvLine = Line.empty();
		Line pvSubLine = Line.empty();
		List<State> moves = rootState.generateLegalMoves();
		int alpha = MINUS_INFINITY;
		final int beta = INFINITY;


		if (moves.isEmpty()) {
			return new Result(terminalNodeScore(rootState, 0), pvLine, nodesEvaluatedInPly, false);
		}

		reorderMoves(moves, leftmostLine, 1);
		for (State move : moves) {
			int currScore;

			try {
				currScore = -alphaBeta(move, depth - 1, -beta, -alpha, leftmostLine, pvSubLine, finishTime, debugLine, 1);
			} catch (Throwable error) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("ROOT STATE: " + rootState);
				throw error;
			}
			if (currScore > alpha) {
				pvLine.updateSubline(pvSubLine, move);
				System.out.println(spaces(UCI.INFO, UCI.PV, pvLine.toString(), UCI.SCORE, UCI.formatScore(currScore)));
				alpha = currScore;
			}

			if (Utils.nanoNow().isAfter(finishTime)) {
				return new Result(0, null, nodesEvaluatedInPly, false);
			}
			if (nextMoveWins(currScore)) {
				break;
			}
		}
		return new Result(alpha, pvLine, nodesEvaluatedInPly, moves.size() == 1);
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
				System.out.println("\talpha: " + alpha + " beta: " + beta);
				System.out.println("\tState is: " + state);
				debugChildrenScores = true;
			}
		}
		if (depth <= 0) {
			return quiescence(state, alpha, beta, ply, principalVariation);
		}

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
				currScore = -alphaBeta(move, depth - 1, -beta, -alpha, leftmostLine, pvSubLine, finishTime, debugLine, ply + 1);
			} catch (Throwable error) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("PLY: " + ply + " STATE: " + state);
				throw error;
			}
			if (debugChildrenScores) {
				System.out.println("\t" + Lan.toStringLastMove(move) + ": " + currScore);
			}
			if (currScore >= beta) {
				return beta;
			}
			if (currScore > alpha) {
				principalVariation.updateSubline(pvSubLine, move);
				alpha = currScore;
			}
			if (Utils.nanoNow().isAfter(finishTime)) {
				break;
			}
			if (interrupt) {
				break;
			}
		}
		return alpha;
	}

	private static int quiescence(State state, int alpha, int beta, int ply, Line principalVariation) {
		int score = evaluate(state, ply);

		if (score >= beta) {
			return beta;
		}
		if (score > alpha) {
			principalVariation.movesCount = 0;
			alpha = score;
		}

		Line pvSubLine = Line.empty();
		List<State> moves = state.generateLegalMoves()
				.stream().filter(move -> move.takenPiece != Content.EMPTY).collect(Collectors.toList());

		for (State move : moves) {
			score = -quiescence(move, -beta, -alpha, ply + 1, pvSubLine);

			if (score >= beta) {
				return beta;
			}
			if (score > alpha) {
				principalVariation.updateSubline(pvSubLine, move);
				alpha = score;
			}
		}
		return alpha;
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

	public static boolean scoreCloseToMating(int score) {
		return Math.abs(score) > Scorer.SCORE_CLOSE_TO_WIN;
	}

	private static boolean nextMoveWins(int score) {
		final int onePly = 1;
		return score == -LOST - onePly;
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

		int score = materialScore + mobilityScore;
		// negamax requires score relative to the moving side
		return state.test(State.WHITE_TURN) ? score : -score;
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
		if (state.isKingInCheck()) {
			return LOST + ply;
		} else {
			return DRAW;
		}
	}
}
