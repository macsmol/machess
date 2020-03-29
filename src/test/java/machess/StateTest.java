package machess;

import machess.interfaces.FEN;
import org.junit.Test;

import static org.junit.Assert.*;

public class StateTest {

    @Test
    public void perfts() {
        testPosition("n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1", new long[] {1 ,24, 496, 9483, 182838, 3605103});
    }

    private void testPosition(State position, int depth, long expectedLegalMoves) {
        long actualLegalMoves = Scorer.perft(position, depth);
        if (expectedLegalMoves != actualLegalMoves) {
            System.out.println("Wrong perft(" + depth + "). Expected: " + expectedLegalMoves+ ", actual: "+ actualLegalMoves +". Position: " + position);
            Scorer.perftDivide(position, depth);
            fail();
        }
    }

    private void testPosition(String fen, long[] expectedLegalMoves) {
        State position = FEN.parse(fen);
        System.out.println("Testing: " + fen);
        for (int depth = 0; depth < expectedLegalMoves.length; depth++) {
            long actualLegalMoves = Scorer.perft(position, depth);
            if (expectedLegalMoves[depth] != actualLegalMoves) {
                System.out.println("Wrong perft(" + depth + "). Expected: " + expectedLegalMoves[depth] + ", actual: "+ actualLegalMoves +". Position: " + position);
                Scorer.perftDivide(position, depth);
                fail();
            }
            System.out.println("Perft(" + depth + ") ok");
        }
    }
}