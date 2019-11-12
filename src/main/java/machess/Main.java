package machess;

public class Main {
	public static void main(String[] args) {
		MasterState8Bit newGame = new MasterState8Bit();
		System.out.println(newGame.toString());

		System.out.println();
		System.out.println();
		BoardState.from(newGame);
		System.out.println(BoardState.debugString());
	}
}
