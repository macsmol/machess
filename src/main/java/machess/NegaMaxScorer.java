package machess;


public class NegaMaxScorer {
	private static final int WIN = Integer.MAX_VALUE;
	private static final int LOSE = Integer.MIN_VALUE;
	private static final int DRAW = 0;

	private static final int MATERIAL_PAWN 		= 100;
	private static final int MATERIAL_KNIGHT 	= 300;
	private static final int MATERIAL_BISHOP 	= 300;
	private static final int MATERIAL_ROOK		= 500;
	private static final int MATERIAL_QUEEN		= 900;
//	private static final int MATERIAL_KING 		= 20000; // this could give incentivize to cause absolute pins

	private static final float FACTOR_DEFENDED_PIECE 		= 1.0f;
	private static final float FACTOR_LOOSE_PIECE 			= 0.9f;
	private static final float FACTOR_ATTACKED_AND_DEFENDED = 0.6f;
	private static final float FACTOR_HANGING_PIECE 		= 0.3f;

	private static final int CHECKED_SQUARE_SCORE = 5;

	private int WHITE2MOVE = 1;
	private int BLACK2MOVE = -1;

	public int evaluate(State state) {
		int whiteMaterialScore = evaluateMaterialScore(state.squaresWithWhites, state.whitesCount, state.board);
		int blackMaterialScore = evaluateMaterialScore(state.squaresWithBlacks, state.blacksCount, state.board);

		int who2Move = state.test(State.WHITE_TURN) ? WHITE2MOVE : BLACK2MOVE;
		return (whiteMaterialScore - blackMaterialScore) * who2Move;
	}

	private int evaluateMaterialScore(Square[] pieces, int piecesCount, short[] board) {
		int score = 0;
		for (int i = piecesCount; i >= 1; i--) {
			Square square = pieces[i];

			Content piece = Utils.getContent(square, board);
			score += getMaterialScore(piece) * getSafetyFactor(piece, square, board);
		}
		return score;
	}

	private int getMaterialScore(Content piece) {
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
	static float getSafetyFactor(Content piece, Square square, short[] board) {
		byte checksByWhite = (byte)((board[square.ordinal()] >> State.SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET) & State.SquareFormat.CHECKS_COUNT_MASK);
		byte checksByBlack = (byte)((board[square.ordinal()] >> State.SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET) & State.SquareFormat.CHECKS_COUNT_MASK);

		byte selfChecks = piece.isWhite ? checksByWhite : checksByBlack;
		byte checksByEnemy = piece.isWhite ? checksByBlack : checksByWhite;

		if (selfChecks > 0) {
			return checksByEnemy > 0 ? FACTOR_ATTACKED_AND_DEFENDED : FACTOR_DEFENDED_PIECE;
		} else {
			return checksByEnemy > 0 ? FACTOR_HANGING_PIECE : FACTOR_LOOSE_PIECE;
		}
	}
}
