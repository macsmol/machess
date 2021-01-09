package machess;

import machess.interfaces.UCI;

public class Main {
    public static void main(String[] args) {
        UCI uci = new UCI();
        uci.startEngine();
    }
}
