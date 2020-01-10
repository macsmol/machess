package machess;

import java.math.BigInteger;

import static machess.State.*;

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

    public static String flagsToString(int flags) {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: ").append(test(flags, WHITE_TURN) ? "WHITE" : "BLACK");
		if (test(flags, WHITE_KING_MOVED)) {
			sb.append("; WHITE_KING_MOVED");
		}
		if (test(flags, WHITE_QS_ROOK_MOVED)) {
			sb.append("; WHITE_QS_ROOK_MOVED");
		}
		if (test(flags, WHITE_KS_ROOK_MOVED)) {
			sb.append("; WHITE_KS_ROOK_MOVED");
		}
		if (test(flags, BLACK_KING_MOVED)) {
			sb.append("; BLACK_KING_MOVED");
		}
		if (test(flags, BLACK_QS_ROOK_MOVED)) {
			sb.append("; BLACK_QS_ROOK_MOVED");
		}
		if (test(flags, BLACK_KS_ROOK_MOVED)) {
			sb.append("; BLACK_KS_ROOK_MOVED");
		}
		return sb.toString();
	}

	private static boolean test(int flags, int mask) {
		return (flags & mask) != 0;
	}

    public static String asHexWithTrailingZeros(int number) {
		return String.format(" %08X", number);
	}
}
