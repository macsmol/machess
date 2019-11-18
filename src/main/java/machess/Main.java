package machess;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();

		System.out.println();
		System.out.println();
		System.out.println(newGame);

		State s1 = newGame.fromUnsafeMove(Field.E2, Field.E8); // xD
		System.out.println(s1);
		State s2 = s1.fromUnsafeMove(Field.E8, Field.E4); // :D
		System.out.println(s2);

//		newGame.debugIsPiecesOn();
	}
}
