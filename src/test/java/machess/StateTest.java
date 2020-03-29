package machess;

import machess.interfaces.FEN;
import org.junit.Test;

import static org.junit.Assert.*;

public class StateTest {

    @Test
    public void perfts() {
        testPosition(FEN.parse("n1n5/PPPk4/8/8/8/8/4Kppp/5N1N b - - 0 1"), new long[] {1 ,24, 496, 9483, 182838, 3605103});
    }

    private void testPosition(State position, long[] expectedLegalMoves) {
        for (int depth = 0; depth < expectedLegalMoves.length; depth++) {
            long actualLegalMoves = Scorer.perft(position, depth);
            if(expectedLegalMoves[depth] != actualLegalMoves) {
                System.err.println("Wrong perft for depth: " + depth + " and position: " + position);
                Scorer.perftDivide(position, depth);
                fail();
            }
        }
    }
}