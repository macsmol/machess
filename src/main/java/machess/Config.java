package machess;

public class Config {
	/**
	 * Don't expect to be searching this far
	 */
	public static final int MAX_SEARCH_DEPTH = getProperty("maxSearchDepth", 40);

	/**
	 * Capacity per piece type and color. Engine will fail when too many pieces of the same type exist at once.
	 * Increase this all the way up to 9 for some theoretical positions with many promotions like this:
	 * R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1
 	 */
	public static final int PIECE_LIST_CAPACITY = getProperty("pieceListCapacity", 5);

	public static final int NODES_LOGGING_PERIOD = getProperty("nodesLoggingPeriod", 500_000);

	public static final String DEBUG_LINE_KEY = "debugLine";

	public static String debugLine() {
		return getProperty(DEBUG_LINE_KEY, null);
	}

	/**
	 * Move generation will fail if given position has more immediate children than this number
	 */
	public static int MOVES_LIST_CAPACITY =  getProperty("movesListCapacity", 60);

	/**
	 * Used in calculation of time devoted to next move
	 */
	public static int EXPECTED_FULL_MOVES_TO_BE_PLAYED = getProperty("expectedFullMovesToBePlayed", 25);

	private static String getProperty(String key, String defaultValue) {
		String value = System.getProperty(key);
		return value != null ? value : defaultValue;
	}

	private static int getProperty(String key, int defaultValue) {
		String value = System.getProperty(key);
		int intVal = defaultValue;
		if (value != null) {
			try {
				intVal = Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				System.err.println("Property " + key + " is not a parsable int. Falling back to default: " + defaultValue);
			}
		}
		return intVal;
	}
}
