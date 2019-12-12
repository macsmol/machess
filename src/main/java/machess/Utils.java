package machess;

public class Utils {
	/**
	 * prints 7-4 of parameter number
	 */
	public static String checkFlagsToString(int number) {
		number >>>= 4;
		StringBuilder result = new StringBuilder();
		for(int i = 3; i >= 0 ; i--) {
			int mask = 1 << i;
			result.append((number & mask) != 0 ? "1" : ".");
		}
		return result.toString();
	}

	public static String toString(PinTo pinnedWhite, PinTo pinnedBlack) {
		StringBuilder sb = new StringBuilder();
		sb.append(pinnedBlack != null ? pinnedBlack.symbol : '.');
		sb.append(pinnedWhite != null ? pinnedWhite.symbol : '.');
		return sb.toString();
	}
}
