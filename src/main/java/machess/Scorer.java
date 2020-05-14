package machess;


import machess.interfaces.UCI;

import java.util.*;

import static machess.Utils.spaces;

public class Scorer {
	private static final int MAXIMIZING_WIN = 1_000_000;
	private static final int MINIMIZING_WIN = -1_000_000;
	private static final int DRAW = 0;

	private static final int MATERIAL_PAWN 		= 100;
	private static final int MATERIAL_KNIGHT 	= 300;
	private static final int MATERIAL_BISHOP 	= 300;
	private static final int MATERIAL_ROOK		= 500;
	private static final int MATERIAL_QUEEN		= 900;

	private static final int LEGAL_MOVE_SCORE = 5;

	private static int movesEvaluatedInPly = 0;
	private static int checkMatesFound = 0;
	private static long totalMovesEvaluated = 0;
	private static long totalNanosElapsed = 0;
	private static int pvUpdates = 0;

	public static MoveScore startMiniMax(State rootState, int depth) {
		boolean maximizing = rootState.test(State.WHITE_TURN);
		movesEvaluatedInPly = 0;
		checkMatesFound = 0;
		PrincipalVariation pvLine = new PrincipalVariation();
		PrincipalVariation pvSubLine = new PrincipalVariation();
		long before = System.nanoTime();
		List<State> moves = rootState.generateLegalMoves();

		int resultScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		int indexOfResultScore = -1;
		if (moves.isEmpty()) {
			System.out.println("Total milliseconds elapsed: " + totalNanosElapsed / 1000_000
					+ "; Moves/sec: " + Utils.calcMovesPerSecond(totalMovesEvaluated, totalNanosElapsed));
			return new MoveScore(terminalNodeScore(rootState), -1);
		}

		depth -=  maximizing ? Config.WHITE_PLY_HANDICAP : Config.BLACK_PLY_HANDICAP;
		for (int i = 0; i < moves.size(); i++) {
			int currScore;

			try {
				currScore = miniMax(moves.get(i), depth - 1, pvSubLine);
			} catch (Throwable ae) {
				System.out.println("----------------------FAILED ASSERTION!-------------------------------------");
				System.out.println("DEPTH: " + depth + " ROOT STATE: " + rootState);
				throw ae;
			}

			if (maximizing) {
				if (currScore > resultScore) {
					pvLine.updateSubline(pvSubLine, moves.get(i));
					System.out.println(spaces(UCI.INFO, UCI.PV, pvLine.toString()));
					resultScore = currScore;
					indexOfResultScore = i;
				}
			} else {
				if (currScore < resultScore) {
					pvLine.updateSubline(pvSubLine, moves.get(i));
					System.out.println(spaces(UCI.INFO, UCI.PV, pvLine.toString()));
					resultScore = currScore;
					indexOfResultScore = i;
				}
			}
		}
		System.out.println();
		long after = System.nanoTime();
		MoveScore bestMove = new MoveScore(resultScore, indexOfResultScore);
		long elapsedNanos = after - before;
		totalMovesEvaluated += movesEvaluatedInPly;
		totalNanosElapsed += elapsedNanos;

		System.out.println(info(movesEvaluatedInPly, pvLine,
				elapsedNanos / 1000_000, depth, pvUpdates,
				Utils.calcMovesPerSecond(movesEvaluatedInPly, elapsedNanos)));
		return bestMove;
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
			return terminalNodeScore(state);
		}
		for (State move : moves) {
			int currScore;
			try {
				currScore = discourageLaterWin(miniMax(move, depth - 1, pvSubLine));
			} catch (Throwable ae) {
				System.out.println("----------------------FAILED ASSERTION!-------------------------------------");
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

	/**
	 * call this on child nodes to encourage choosing earlier wins
	 */
	private static int discourageLaterWin(int score) {
		return score * 1023 / 1024;
	}

	public static class MoveScore {
		public final int score;
		public final int moveIndex;

		public MoveScore(int score, int moveIndex) {
			this.score = score;
			this.moveIndex = moveIndex;
		}

		@Override
		public String toString() {
			return "MoveScore{" +
					"score=" + score +
					", moveIndex=" + moveIndex +
					'}';
		}
	}

	public static int evaluate(State state) {
		movesEvaluatedInPly++;
		int legalMoves = state.countLegalMoves();
		if (legalMoves == 0) {
			int terminalNodeScore = terminalNodeScore(state);
			if (terminalNodeScore != DRAW) {
				checkMatesFound++;
			}
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

				// todo updateSubline()
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

	private static String info(int nodesEvaluated, PrincipalVariation pvLine, long elapsedMillis, int depth, int pvUpdates, int nodesPerSecond) {
		return spaces(UCI.INFO,
				UCI.NODES, Integer.toString(nodesEvaluated),
				UCI.PV, pvLine.toString(),
				UCI.TIME, Long.toString(elapsedMillis),
				UCI.DEPTH, Integer.toString(depth),
				UCI.NPS, Integer.toString(nodesPerSecond),
				"pvUpdates", Integer.toString(pvUpdates));
	}

	private static class PrincipalVariation {
		private String [] moves = new String[Config.MAX_SEARCH_DEPTH];
		private int movesCount = 0;

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
