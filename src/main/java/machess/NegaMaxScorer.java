package machess;


public class NegaMaxScorer {
	private static final int WIN = 1_000_000;
	private static final int LOSE = -1_000_000;
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

	private static final int WHITE2MOVE = 1;
	private static final int BLACK2MOVE = -1;

	public static int evaluate(State state) {
		int materialScore = evaluateMaterialScore(state);
		System.out.println("	materialScore: " + materialScore);

		int checkedSquaresScore = evaluateCheckedSquaresScore(state);
		System.out.println("	checkedSquaresScore: " + checkedSquaresScore);

		int who2Move = state.test(State.WHITE_TURN) ? WHITE2MOVE : BLACK2MOVE;
		System.out.println("	who2Move: " + who2Move);
		return  (materialScore + checkedSquaresScore) * who2Move;
	}

	private static int evaluateMaterialScore(State state) {
		int whiteMaterialScore = evaluateMaterialScore(state.squaresWithWhites, state.whitesCount, state);
		int blackMaterialScore = evaluateMaterialScore(state.squaresWithBlacks, state.blacksCount, state);

		System.out.println("		whiteMaterialScore: " + whiteMaterialScore);
		System.out.println("		blackMaterialScore: " + blackMaterialScore);
		return whiteMaterialScore - blackMaterialScore;
	}

	private static int evaluateMaterialScore(Square[] piecesOfOneColor, int piecesCount, State state) {
		int score = 0;
		for (int i = piecesCount - 1; i >= 1; i--) {
			Square square = piecesOfOneColor[i];

			Content piece = state.getContent(square);
			score += getMaterialScore(piece) * getSafetyFactor(piece.isWhite, square, state);
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
		System.out.println("		squaresCheckedByWhite: " + squaresCheckedByWhite);
		System.out.println("		squaresCheckedByBlack: " + squaresCheckedByBlack);
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
}
