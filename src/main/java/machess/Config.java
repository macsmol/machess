package machess;

public class Config {
	public static final boolean DEBUG_FIELD_IN_CHECK_FLAGS = true;
	public static final boolean DEBUG_PINNED_PIECES = true;

	public static final int DEFAULT_MOVES_CAPACITY = 200;
	/**
	 * As explained in:
		https://www.chessprogramming.org/Encoding_Moves#Move_Index
	 *
	 */
	public static final int MAX_THEORETICAL_NUMBER_OF_MOVES = 218;

	/**
	 * how many arrays for plies are allocated. Just make it bigger than how far the program can see.
	 */
	public static int SEARCH_DEPTH_CAPACITY = 40;

	public static final int MAX_SEARCH_DEPTH = 3;

	/**
	 * Decrement maximizing player search depth by this number
	 */
	public static final int WHITE_PLY_HANDICAP = 0;
	/**
	 * Decrement minimizing player search depth by this number
	 */
	public static final int BLACK_PLY_HANDICAP = 1;
}
