package machess;

public class Config {
	public static final boolean DEBUG_FIELD_IN_CHECK_FLAGS = true;
	public static final boolean DEBUG_PINNED_PIECES = true;

	public static final int DEFAULT_MOVES_LIST_CAPACITY = 60;

	/**
	 * Used in calculation of time devoted to next move
	 */
	public static final int EXPECTED_FULLMOVES_LEFT = 25;

	/**
	 * Don't expect to be searching this far
	 */
	public static final int MAX_SEARCH_DEPTH = 40;

	/**
	 * moveGenerationTest() fail if this is below 5.
	 * Increase this up to 9 for some theoretical positions with many promotions like this:
	 * R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1
 	 */
	public static final int PIECE_LIST_CAPACITY = 5;

	public static final int LOG_NODES_EVALUATED_DELAY = 500000;

	public static final String DEBUG_LINE = "e2e4";
}
