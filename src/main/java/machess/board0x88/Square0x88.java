package machess.board0x88;

/**
 * https://www.chessprogramming.org/0x88
 */
public interface Square0x88 {
    byte A8 = 0x70; byte B8 = 0x71; byte C8 = 0x72; byte D8 = 0x73; byte E8 = 0x74; byte F8 = 0x75; byte G8 = 0x76; byte H8 = 0x77;
    byte A7 = 0x60; byte B7 = 0x61; byte C7 = 0x62; byte D7 = 0x63; byte E7 = 0x64; byte F7 = 0x65; byte G7 = 0x66; byte H7 = 0x67;
    byte A6 = 0x50; byte B6 = 0x51; byte C6 = 0x52; byte D6 = 0x53; byte E6 = 0x54; byte F6 = 0x55; byte G6 = 0x56; byte H6 = 0x57;
    byte A5 = 0x40; byte B5 = 0x41; byte C5 = 0x42; byte D5 = 0x43; byte E5 = 0x44; byte F5 = 0x45; byte G5 = 0x46; byte H5 = 0x47;
    byte A4 = 0x30; byte B4 = 0x31; byte C4 = 0x32; byte D4 = 0x33; byte E4 = 0x34; byte F4 = 0x35; byte G4 = 0x36; byte H4 = 0x37;
    byte A3 = 0x20; byte B3 = 0x21; byte C3 = 0x22; byte D3 = 0x23; byte E3 = 0x24; byte F3 = 0x25; byte G3 = 0x26; byte H3 = 0x27;
    byte A2 = 0x10; byte B2 = 0x11; byte C2 = 0x12; byte D2 = 0x13; byte E2 = 0x14; byte F2 = 0x15; byte G2 = 0x16; byte H2 = 0x17;
    byte A1 = 0x00; byte B1 = 0x01; byte C1 = 0x02; byte D1 = 0x03; byte E1 = 0x04; byte F1 = 0x05; byte G1 = 0x06; byte H1 = 0x07;


    static byte from07(int file07, int rank07) {
        return (byte)(16 * rank07 + file07);
    }

    /**
     * @return file in 0-7 range
     */
    static byte getFile(byte square0x88) {
        return (byte)(square0x88 & 7);
    }

    /**
     * @return rank in 0-7 range
     */
    static byte getRank(byte square0x88) {
        return (byte)(square0x88 >> 4);
    }
}
