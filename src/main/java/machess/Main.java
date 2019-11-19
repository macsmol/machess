package machess;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		System.out.println(newGame);

		State s1= newGame.fromUnsafeMove(Field.E2, Field.G7);
		System.out.println(s1);

		State s2= s1.fromUnsafeMove(Field.E7, Field.E5);
		System.out.println(s2);

		long before = System.currentTimeMillis();
		System.out.println(s2.generateMoves().size());
		System.out.println(" elapsed ms: " + (System.currentTimeMillis() - before));

	}
}
