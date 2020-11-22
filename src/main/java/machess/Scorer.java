package machess;


import machess.interfaces.UCI;

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

	public static final int SCORE_CLOSE_TO_WIN = 2 * (9 * MATERIAL_QUEEN +  2 * MATERIAL_ROOK +
			2 * MATERIAL_BISHOP + 2 * MATERIAL_KNIGHT);

	private static final int LEGAL_MOVE_SCORE = 5;

	private static int nodesEvaluatedInPly = 0;
	private static int pvUpdates = 0;

	private static volatile boolean interrupt;

	public static Result startMiniMax(State rootState, int depth) {
		interrupt = false;
		nodesEvaluatedInPly = 0;
		pvUpdates = 0;
		PrincipalVariation pvLine = new PrincipalVariation();
		PrincipalVariation pvSubLine = new PrincipalVariation();
		List<State> moves = rootState.generateLegalMoves();

		boolean maximizing = rootState.test(State.WHITE_TURN);
		int resultScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

		if (moves.isEmpty()) {
			return new Result(terminalNodeScore(rootState), pvLine, nodesEvaluatedInPly, pvUpdates, false);
		}

		for (State move : moves) {
			int currScore;

			try {
				currScore = discourageLaterWin(miniMax(move, depth - 1, pvSubLine));
			} catch (Throwable ae) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("DEPTH: " + depth + " ROOT STATE: " + rootState);
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
	 * @param pvLine - principal variation line - https://www.chessprogramming.org/Principal_Variation
	 * @return score
	 */
	private static int miniMax(State state, int depth, PrincipalVariation pvLine) {
		boolean maximizingTurn = state.test(State.WHITE_TURN);
		if (depth <= 0) {
			pvLine.movesCount = 0;
			return evaluate(state);
		}

		int resultScore = maximizingTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		PrincipalVariation pvSubLine = new PrincipalVariation();

		List<State> moves = state.generateLegalMoves();

		if (moves.isEmpty()) {
			pvLine.movesCount = 0;
			return terminalNodeScore(state);
		}
		for (State move : moves) {
			int currScore;
			try {
				currScore = discourageLaterWin(miniMax(move, depth - 1, pvSubLine));
			} catch (Throwable ae) {
				System.out.println("----------------------ERROR!-------------------------------------");
				System.out.println("DEPTH: " + depth + " STATE: " + state);
				throw ae;
			}
			if (maximizingTurn) {
				if (currScore > resultScore) {
					pvLine.updateSubline(pvSubLine, move);
					resultScore = currScore;
				}
			} else {
				if (currScore < resultScore) {
					pvLine.updateSubline(pvSubLine, move);
					resultScore = currScore;
				}
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
			System.out.println(Lan.printLastMove(child) + " " + movesCount);
		}
	}

	public static boolean scoreCloseToWinning(int score) {
		return Math.abs(score) > Scorer.SCORE_CLOSE_TO_WIN;
	}

	private static boolean nextMoveWins(int score) {
		return Math.abs(score) >= discourageLaterWin(MAXIMIZING_WIN);
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
		public final PrincipalVariation pv;
		public final int nodesEvaluated;

		public final int pvUpdates;

		// skip iterative deepening in this case
		public final boolean oneLegalMove;

		public Result(int score, PrincipalVariation pvLine, int nodesEvaluated, int pvUpdates,
					  boolean oneLegalMove) {
			this.score = score;
			this.pv = pvLine;
			this.nodesEvaluated = nodesEvaluated;
			this.pvUpdates = pvUpdates;
			this.oneLegalMove = oneLegalMove;
		}

		@Override
		public String toString() {
			return "Result{" +
					"score=" + score +
					", pv=" + pv +
					", oneLegalMove=" + oneLegalMove +
					", pvUpdates=" + pvUpdates +
					'}';
		}
	}

	public static int evaluate(State state) {
		nodesEvaluatedInPly++;
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


	public static class PrincipalVariation {
		public String [] moves = new String[Config.MAX_SEARCH_DEPTH];
		public int movesCount = 0;

		private void updateSubline(PrincipalVariation newSubLine, State move) {
			pvUpdates++;
			moves[0] = Lan.printLastMove(move);

			System.arraycopy(newSubLine.moves, 0, moves, 1, newSubLine.movesCount);
			movesCount = newSubLine.movesCount + 1;
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
