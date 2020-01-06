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

	private static final float FACTOR_DEFENDED_PIECE 		= 1.0f;
	private static final float FACTOR_LOOSE_PIECE 			= 0.9f;
	private static final float FACTOR_ATTACKED_AND_DEFENDED = 0.6f;
	private static final float FACTOR_HANGING_PIECE 		= 0.3f;

	private static final int CHECKED_SQUARE_SCORE = 5;

	private static int movesEvaluatedInPly = 0;
	private static long totalMovesEvaluated = 0;
	private static long totalNanosElapsed = 0;


	public static MoveScore minMax(State rootState) {
		boolean maximizing = rootState.test(State.WHITE_TURN);
		movesEvaluatedInPly = 0 ;
		long before = System.nanoTime();
		List<State> moves = rootState.generateLegalMoves();
		int resultScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		int indexOfResultScore = -1;
		if (moves.isEmpty()) {
			System.out.println("Total seconds elapsed: " + totalNanosElapsed / 1000_000_000
					+ "; Moves/sec: " + Utils.calcMovesPerSecond(totalMovesEvaluated, totalNanosElapsed));
			return new MoveScore(terminalNodeScore(rootState), -1);
		}
		for (int i = 0; i < moves.size(); i++) {
			int depth = Config.SEARCH_DEPTH - 1;
			depth -=  maximizing ? Config.WHITE_PLY_HANDICAP : Config.BLACK_PLY_HANDICAP;

			int currScore = minMax(moves.get(i), depth);
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
		long after = System.nanoTime();
		MoveScore bestMove = new MoveScore(resultScore, indexOfResultScore);
		long elapsedNanos = after - before;
		totalMovesEvaluated += movesEvaluatedInPly;
		totalNanosElapsed += elapsedNanos;

		System.out.println(bestMove + "; millis elapsed: " + elapsedNanos / 1000_000 + "; Moves/sec: " + Utils.calcMovesPerSecond(movesEvaluatedInPly, elapsedNanos));
		return bestMove;
	}

	private static int minMax(State state, int depth) {
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
			int currScore = discourageLaterWin(minMax(move, depth - 1));
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
		if (state.countLegalMoves() == 0) {
			return terminalNodeScore(state);
		}
		movesEvaluatedInPly++;
		int materialScore = evaluateMaterialScore(state);
		int checkedSquaresScore = evaluateCheckedSquaresScore(state);

		return materialScore + checkedSquaresScore;
	}

	private static int evaluateMaterialScore(State state) {
		int whiteMaterialScore = evaluateMaterialScore(state.squaresWithWhites, state.whitesCount, state);
		int blackMaterialScore = evaluateMaterialScore(state.squaresWithBlacks, state.blacksCount, state);

		return whiteMaterialScore - blackMaterialScore;
	}

	private static int evaluateMaterialScore(Square[] piecesOfOneColor, int piecesCount, State state) {
		int score = 0;
		for (int i = piecesCount - 1; i >= 1; i--) {
			Square square = piecesOfOneColor[i];

			Content piece = state.getContent(square);
			score += getMaterialScore(piece)
//					* getSafetyFactor(piece.isWhite, square, state)
			;
		}
		return score;
	}

	private static int getMaterialScore(Content piece) {
		switch (piece) {
		case WHITE_PAWN:
		case BLACK_PAWN:
			return MATERIAL_PAWN;
		case WHITE_KNIGHT:
		case BLACK_KNIGHT:
			return MATERIAL_KNIGHT;
		case WHITE_BISHOP:
		case BLACK_BISHOP:
			return MATERIAL_BISHOP;
		case WHITE_ROOK:
		case BLACK_ROOK:
			return MATERIAL_ROOK;
		case WHITE_QUEEN:
		case BLACK_QUEEN:
			return MATERIAL_QUEEN;
		default:
			assert false : "Wrong Content for material score: " + piece;
			return 0;
		}
	}

	/**
	 * TODO this factor seems to have detrimental effect on play. Remove it? improve it?
	 // multiply material score by appropriate factor
	 * This model seems too simplistic. Will have to implement SEE..?
	 * https://www.chessprogramming.org/Loose_Piece
	 * https://www.chessprogramming.org/Hanging_Piece
	 * https://www.chessprogramming.org/Static_Exchange_Evaluation
	 */
	private static float getSafetyFactor(boolean isPieceWhite, Square square, State state) {
		boolean isCheckedByWhite = state.isSquareCheckedBy(square, State.WHITE);
		boolean isCheckedByBlack = state.isSquareCheckedBy(square, State.BLACK);

		boolean checkedByFriend = isPieceWhite ? isCheckedByWhite : isCheckedByBlack;
		boolean checkedByEnemy = isPieceWhite ? isCheckedByBlack : isCheckedByWhite;

		if (checkedByFriend) {
			return checkedByEnemy ? FACTOR_ATTACKED_AND_DEFENDED : FACTOR_DEFENDED_PIECE;
		} else {
			return checkedByEnemy ? FACTOR_HANGING_PIECE : FACTOR_LOOSE_PIECE;
		}
	}

	private static int evaluateCheckedSquaresScore(State state) {
		int squaresCheckedByWhite = countSquaresCheckedBy(State.WHITE, state);
		int squaresCheckedByBlack = countSquaresCheckedBy(State.BLACK, state);
		return (squaresCheckedByWhite - squaresCheckedByBlack) * CHECKED_SQUARE_SCORE;
	}

	private static int countSquaresCheckedBy(boolean checksByWhite, State state) {
		int checksCount = 0;
		for (Square square : Square.values()) {
			if (state.isSquareCheckedBy(square, checksByWhite)) {
				checksCount++;
			}
		}
		return checksCount;
	}

	private static int terminalNodeScore(State state) {
		movesEvaluatedInPly++;
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
}
