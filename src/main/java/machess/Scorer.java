package machess;


import java.util.List;

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


	public static MoveScore miniMax(State rootState) {
		boolean maximizing = rootState.test(State.WHITE_TURN);
		movesEvaluatedInPly = 0;
		checkMatesFound = 0;
		long before = System.nanoTime();
		List<State> moves = rootState.generateLegalMoves();

		int resultScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		int indexOfResultScore = -1;
		if (moves.isEmpty()) {
			System.out.println("Total milliseconds elapsed: " + totalNanosElapsed / 1000_000
					+ "; Moves/sec: " + Utils.calcMovesPerSecond(totalMovesEvaluated, totalNanosElapsed));
			return new MoveScore(terminalNodeScore(rootState), -1);
		}

		int depth = Config.SEARCH_DEPTH;
		depth -=  maximizing ? Config.WHITE_PLY_HANDICAP : Config.BLACK_PLY_HANDICAP;
		for (int i = 0; i < moves.size(); i++) {
			int currScore;

			try {
				currScore = miniMax(moves.get(i), depth - 1);
			} catch (Throwable ae) {
				System.out.println("----------------------FAILED ASSERTION!-------------------------------------");
				System.out.println("DEPTH: " + depth + " ROOT STATE: " + rootState);
				throw ae;
			}

			if (maximizing) {
				if (currScore > resultScore) {
					resultScore = currScore;
					indexOfResultScore = i;
				}
			} else {
				if (currScore < resultScore) {
					resultScore = currScore;
					indexOfResultScore = i;
				}
			}
		}
		System.out.println("Moves evaluated: " + movesEvaluatedInPly);
		System.out.println("Checkmates found: " + checkMatesFound);
		long after = System.nanoTime();
		MoveScore bestMove = new MoveScore(resultScore, indexOfResultScore);
		long elapsedNanos = after - before;
		totalMovesEvaluated += movesEvaluatedInPly;
		totalNanosElapsed += elapsedNanos;

		System.out.println(bestMove + "; millis elapsed: " + elapsedNanos / 1000_000 + "; Moves/sec: " + Utils.calcMovesPerSecond(movesEvaluatedInPly, elapsedNanos));
		return bestMove;
	}

	private static int miniMax(State state, int depth) {
		boolean maximizingTurn = state.test(State.WHITE_TURN);
		if (depth <= 0) {
			return evaluate(state);
		}
		int resultScore = maximizingTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;

		List<State> moves = state.generateLegalMoves();


		if (moves.isEmpty()) {
			return terminalNodeScore(state);
		}
		for (State move : moves) {
			int currScore;

			try {
				currScore = discourageLaterWin(miniMax(move, depth - 1));
			} catch (Throwable ae) {
				System.out.println("----------------------FAILED ASSERTION!-------------------------------------");
				System.out.println("DEPTH: " + depth + " STATE: " + state);
				throw ae;
			}
			if (maximizingTurn) {
				if (currScore > resultScore) {
					resultScore = currScore;
				}
			} else {
				if (currScore < resultScore) {
					resultScore = currScore;
				}
			}
		}
		return resultScore;
	}

	/**
	 * call this on child nodes to encourage choosing earlier wins
	 * @param score
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
//		int mobilityScore = LEGAL_MOVE_SCORE * legalMoves;
		if (legalMoves == 0) {
			int terminalNodeScore = terminalNodeScore(state);
			if (terminalNodeScore != DRAW) {
				checkMatesFound++;
			}
			return terminalNodeScore;
		}
		int materialScore = evaluateMaterialScore(state);

		return materialScore;
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
				checkMatesFound++;
				return MINIMIZING_WIN;
			} else {
				return DRAW;
			}
		} else {
			if (state.isKingInCheck()) {
				checkMatesFound++;
				return MAXIMIZING_WIN;
			} else {
				return DRAW;
			}
		}
	}
}
