package machess;

/**
 * Contains info about pieces with limited movement because they protect the king.
 */
public class PinnedPieces {

	/**
	 * This is indexed byt Field.ordinal
	 */
	final PinnedTo[] pinnedWhites;

	/**
	 * This is indexed byt Field.ordinal
	 */
	final PinnedTo[] pinnedBlacks;

	PinnedPieces() {
		pinnedWhites = new PinnedTo[Field.values().length];
		pinnedBlacks = new PinnedTo[Field.values().length];
	}

	/**
	 * Describes the line to which movement is limited
	 */
	enum PinnedTo {
		FILE, // |
		RANK, // -
		DIAGONAL, // /
		ANTIDIAGONAL // \
	}
}
