package machess;

import java.util.Arrays;

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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n |a |b |c |d |e |f |g |h |\n");
		sb.append("===========================\n");
		for (byte rank = Rank._8; rank >= Rank._1; rank--) {
			sb.append(rank + 1).append("|");
			for (byte file = File.A; file <= File.H; file++) {
				PinnedTo pinnedWhite = pinnedWhites[Field.fromLegalInts(file, rank).ordinal()];
				PinnedTo pinnedBlack = pinnedBlacks[Field.fromLegalInts(file, rank).ordinal()];
				sb.append(toString(pinnedWhite, pinnedBlack)).append("|");
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	private String toString(PinnedTo pinnedWhite, PinnedTo pinnedBlack) {
		StringBuilder sb = new StringBuilder();
		sb.append(pinnedBlack != null ? pinnedBlack.symbol : '.');
		sb.append(pinnedWhite != null ? pinnedWhite.symbol : '.');
		return sb.toString();
	}

	/**
	 * Describes the line to which movement is limited
	 */
	enum PinnedTo {
		FILE('|'),          // |
		RANK('-'),          // -
		DIAGONAL('/'),      // /
		ANTIDIAGONAL('\\');	// \

		private char symbol;

		PinnedTo(char symbol) {
			this.symbol = symbol;
		}
	}
}
