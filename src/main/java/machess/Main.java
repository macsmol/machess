package machess;

import machess.interfaces.UCI;

public class Main {
    public static void main(String[] args) {
        startUci();
    }

    private static void startUci() {
        UCI uci = new UCI();
        uci.startEngine();
    }
}
