package machess.board0x88;

/**
 * Directions on the 0x88 board (https://www.chessprogramming.org/0x88).
 * North is towards the 8th rank.
 * East is towards the H file.
 */
public final class Direction {
    public static final byte N = 0x10;
    public static final byte S = -N;
    public static final byte E = 0x01;
    public static final byte W = -E;

    public static final byte NE = 0x11;
    public static final byte SW = -NE;
    public static final byte NW = 0x0F;
    public static final byte SE = -NW;

    // knight directions below
    public static final byte NNE = 0x21;
    public static final byte SSW = -NNE;

    public static final byte NNW = 0x1F;
    public static final byte SSE = -NNW;

    public static final byte NEE = 0x12;
    public static final byte SWW = -NEE;

    public static final byte NWW = 0x0E;
    public static final byte SEE = -NWW;

    public static byte move(byte from0x88, byte direction) {
        return (byte) (from0x88 + direction);
    }
}
