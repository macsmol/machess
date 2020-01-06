package machess;

import java.math.BigInteger;

public class Utils {
	public static String checkCountsToString(short number) {
		StringBuilder sb = new StringBuilder();
		boolean isNoKing = (State.SquareFormat.NO_KINGS_FLAG & number) != 0;
		sb.append(isNoKing ? '1' : '.');
		number = (short)(number >>> 8);

		sb.append(String.format(" %02X", number).replace('0','.'));
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
