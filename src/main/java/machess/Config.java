package machess;

public class Config {
	public static final boolean DEBUG_FIELD_IN_CHECK_FLAGS = true;
	public static final boolean DEBUG_PINNED_PIECES = true;

	public static final int DEFAULT_MOVES_LIST_CAPACITY = 60;

	public static final int SEARCH_DEPTH = 4;

	/**
	 * Decrement maximizing player search depth by this number
	 */
	public static final int WHITE_PLY_HANDICAP = 0;
	/**
	 * Decrement minimizing player search depth by this number
	 */
	public static final int BLACK_PLY_HANDICAP = 1;

	/**
	 * moveGenerationTest() fail if this is below 5.
 	 */
	public static final int PIECE_LIST_CAPACITY = 5;
}
