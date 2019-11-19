package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		System.out.println(newGame);

		State s1= newGame.fromUnsafeMove(Field.F2, Field.F5);
		System.out.println(s1);

		State s2= s1.fromUnsafeMove(Field.E7, Field.E5);
//		State s2= s1.fromUnsafeMove(Field.E7, Field.E5,null, Field.E6);
		System.out.println(s2);

		long before = System.currentTimeMillis();
		List<State> legalMoves = s2.generateMoves();
		System.out.println(" elapsed ms: " + (System.currentTimeMillis() - before));
		for (State state : legalMoves) {
			System.out.println(state);
		}
	}
}
