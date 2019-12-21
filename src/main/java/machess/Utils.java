package machess;

import com.sun.javafx.binding.StringFormatter;

public class Utils {
	public static String checkCountsToString(short number) {
		StringBuilder sb = new StringBuilder();
		boolean isNoKing = (State.NO_KINGS_FLAG & number) != 0;
		sb.append(isNoKing ? '1' : '0');
		number = (short)(number >>> 8);

		sb.append(String.format("_%02X", number));
		return sb.toString();
	}
}
