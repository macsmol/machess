package machess;

public class Utils {
	public static String checkCountsToString(short number) {
		StringBuilder sb = new StringBuilder();
		boolean isNoKing = (State.SquareFormat.NO_KINGS_FLAG & number) != 0;
		sb.append(isNoKing ? '1' : '0');
		number = (short)(number >>> 8);

		sb.append(String.format(" %02X", number).replace('0','_'));
		return sb.toString();
	}
}
