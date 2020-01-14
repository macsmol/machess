package machess;


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



	/**
	 * moves at ach ply
	 * [ply][moveAsInt]
	 */
	private static int[][] MOVES_SEARCHED;

	/**
	 * move counts per ply
	 */
	private static int[] MOVE_COUNTS;

	static {
		MOVES_SEARCHED = new int[Config.SEARCH_DEPTH_CAPACITY][];
		for (int i = 0; i < Config.SEARCH_DEPTH_CAPACITY; i++) {
			MOVES_SEARCHED[i] = new int[Config.DEFAULT_MOVES_CAPACITY];
		}
		MOVE_COUNTS = new int[Config.SEARCH_DEPTH_CAPACITY];
	}


	public static MoveScore miniMax(State rootState) {
		boolean maximizing = rootState.test(State.WHITE_TURN);
		movesEvaluatedInPly = 0 ;
		long before = System.nanoTime();
		MOVE_COUNTS[0] = rootState.generateLegalMoves(MOVES_SEARCHED[0]);
		int resultScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		if (MOVE_COUNTS[0] == 0) {
			System.out.println("Total seconds elapsed: " + totalNanosElapsed / 1000_000_000
					+ "; Moves/sec: " + Utils.calcMovesPerSecond(totalMovesEvaluated, totalNanosElapsed));
			return new MoveScore(terminalNodeScore(rootState), Move.NULL);
		}
		int bestMove = Move.NULL;
		for (int i = 0; i < MOVE_COUNTS[0]; i++) {
			int move = MOVES_SEARCHED[0][i];
			rootState.makePseudoLegalMove(move);
			int currScore = miniMax(rootState, 1);
			rootState.unmakePseudoLegalMove(move);
			if (maximizing) {
				if (currScore > resultScore) {
					resultScore = currScore;
					bestMove = move;
				}
			} else {
				if (currScore < resultScore) {
					resultScore = currScore;
					bestMove = move;
				}
			}
		}
		System.out.println("Moves evaluated: " + movesEvaluatedInPly);
		long after = System.nanoTime();
		MoveScore bestMoveAndScore = new MoveScore(resultScore, bestMove);
		long elapsedNanos = after - before;
		totalMovesEvaluated += movesEvaluatedInPly;
		totalNanosElapsed += elapsedNanos;

		System.out.println(bestMove + "; millis elapsed: " + elapsedNanos / 1000_000 + "; Moves/sec: " + Utils.calcMovesPerSecond(movesEvaluatedInPly, elapsedNanos));
		return bestMoveAndScore;
	}

	private static int miniMax(State state, int depth) {
		boolean maximizingTurn = state.test(State.WHITE_TURN);
		int maxSearchDepth = Config.MAX_SEARCH_DEPTH;
		maxSearchDepth -= maximizingTurn ? Config.WHITE_PLY_HANDICAP : Config.BLACK_PLY_HANDICAP;
		if (depth >= maxSearchDepth) {
			return evaluate(state);
		}
		int resultScore = maximizingTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;

		MOVE_COUNTS[depth] = state.generateLegalMoves(MOVES_SEARCHED[depth]);
		if (MOVE_COUNTS[depth] == 0) {
			return terminalNodeScore(state);
		}
		for (int i = 0; i < MOVE_COUNTS[depth]; i++) {
			int move = MOVES_SEARCHED[depth][i];
			state.makePseudoLegalMove(move);
			int currScore = discourageLaterWin(miniMax(state, depth + 1));
			state.unmakePseudoLegalMove(move);
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
		public final int moveAsInt;

		public MoveScore(int score, int moveAsInt) {
			this.score = score;
			this.moveAsInt = moveAsInt;
		}

		@Override
		public String toString() {
			return "MoveScore{" +
					"score=" + score +
					", moveAsInt=" + moveAsInt +
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
