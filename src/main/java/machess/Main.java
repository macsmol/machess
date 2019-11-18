package machess;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();

		System.out.println();
		System.out.println();
		System.out.println(newGame);

		newGame.generateMoves();

//		newGame.debugIsPiecesOn();
	}
}
