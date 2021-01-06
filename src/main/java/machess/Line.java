package machess;

import java.util.StringJoiner;

/**
 * Represents sequence of moves in LAN format eg.
 * e2e4 e7e6
 */
public class Line {
    public String [] moves = new String[Config.MAX_SEARCH_DEPTH];
    public int movesCount = 0;
    public int movesMatched = 0;

    public void updateSubline(Line newSubLine, State move) {
        moves[0] = Lan.toStringLastMove(move);

        System.arraycopy(newSubLine.moves, 0, moves, 1, newSubLine.movesCount);
        movesCount = newSubLine.movesCount + 1;
    }

    private  Line() {}

    private Line(String movesLan) {
        String [] movesSplit = movesLan.split(" +");
        this.moves = movesSplit;
        this.movesCount = movesSplit.length;
    }

    public static Line empty() {
        return new Line();
    }

    /**
     *
     * @param movesLan eg. "e2e4 e7e5"
     */
    public static Line of(String movesLan) {
        return movesLan != null ? new Line(movesLan) : null;
    }

    public boolean isMoveMatched(State move, int ply) {
        if (ply - 1 == movesMatched // so that we match the move only at desired level
                && movesMatched < moves.length && Lan.toStringLastMove(move).equals(moves[movesMatched])) {
            movesMatched++;
            return true;
        }
        return false;
    }

    public boolean isLineMatched() {
        if (movesCount == movesMatched) {
            // once int overflows it could return false positives
            movesMatched++;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringJoiner sb = new StringJoiner(" ");
        for (int i = 0; i < movesCount; i++) {
            sb.add(moves[i]);
        }
        return sb.toString();
    }
}
