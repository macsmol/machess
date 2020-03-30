package machess;

import java.math.BigInteger;

public class Utils {
	public static String checkCountsToString(short number) {
		StringBuilder sb = new StringBuilder();

		boolean checkedByBlackKing = (State.SquareFormat.CHECK_BY_BLACK_KING & number) != 0;
		sb.append(checkedByBlackKing ? 'B' : '.');
		boolean checkedByWhiteKing = (State.SquareFormat.CHECK_BY_WHITE_KING & number) != 0;
		sb.append(checkedByWhiteKing ? 'W' : '.');

		number = (short)(number >>> 8);

		sb.append(String.format("%02X", number).replace('0','.'));
		return sb.toString();
	}

    static int calcMovesPerSecond(long movesEvaluated, long elapsedNanos) {
        elapsedNanos++;
        BigInteger movesPerSec = BigInteger.valueOf(1000_000_000)
                .multiply(BigInteger.valueOf(movesEvaluated))
                .divide(BigInteger.valueOf(elapsedNanos));
        return movesPerSec.intValue();
    }
}
