package machess;

import machess.board0x88.Square0x88;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class Utils {

	private static final NanoClock NANO_CLOCK = new NanoClock();

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

	public static long calcNodesPerSecond(long movesEvaluated, long elapsedNanos) {
		elapsedNanos++;
		BigInteger movesPerSec = BigInteger.valueOf(1000_000_000)
				.multiply(BigInteger.valueOf(movesEvaluated))
				.divide(BigInteger.valueOf(elapsedNanos));
		return movesPerSec.longValueExact();
	}

	public static Instant nanoNow() {
		return NANO_CLOCK.instant();
	}

	/**
	 * Insertion sort of pawns by their file
	 * @param pawns - pawn locations given as 0x88 squares
	 */
	static void sortByFiles(byte[] pawns, int pawnsCount)	{
		for (int i = 1; i < pawnsCount; ++i) {
			byte key = pawns[i];
			int j = i - 1;

            /* Move elements of arr[0..i-1], that are
               greater than key, to one position ahead
               of their current position */
			while (j >= 0 && Square0x88.getFile(pawns[j]) > Square0x88.getFile(key)) {
				pawns[j + 1] = pawns[j];
				j = j - 1;
			}
			pawns[j + 1] = key;
		}
	}

	public static String spaces(CharSequence... tokens) {
		return String.join(" ", tokens);
	}

	private static class NanoClock extends Clock {
		private final Clock clock;

		private final long initialNanos;

		private final Instant initialInstant;

		public NanoClock() {
			this(Clock.systemUTC());
		}

		public NanoClock(final Clock clock) {
			this.clock = clock;
			initialInstant = clock.instant();
			initialNanos = getSystemNanos();
		}

		@Override
		public ZoneId getZone() {
			return clock.getZone();
		}

		@Override
		public Instant instant() {
			return initialInstant.plusNanos(getSystemNanos() - initialNanos);
		}

		@Override
		public Clock withZone(final ZoneId zone) {
			return new NanoClock(clock.withZone(zone));
		}

		private long getSystemNanos() {
			return System.nanoTime();
		}
	}
}
