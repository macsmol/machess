package machess;

public class Config {
	public static final boolean DEBUG_FIELD_IN_CHECK_FLAGS = true;
	public static final boolean DEBUG_PINNED_PIECES = true;

	public static final int DEFAULT_MOVES_LIST_CAPACITY = 60;

	// TODO conflict with pawn sorting?
	public static final boolean SORT_OCCUPIED_SQUARES = false;

	public static final int SEARCH_DEPTH = 5;

	/**
	 * Decrement maximizing player search depth by this number
	 */
	public static final int WHITE_PLY_HANDICAP = 0;
	/**
	 * Decrement minimizing player search depth by this number
	 */
	public static final int BLACK_PLY_HANDICAP = 0;

	public static final int PIECE_LIST_CAPACITY = 4;
}
