package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		State sicilianDefense = newGame
				.fromUnsafeMove(Field.E2, Field.E4)
				.fromUnsafeMove(Field.C7, Field.C5);
		System.out.println(sicilianDefense);


		long before = System.currentTimeMillis();
		List<State> legalMoves = sicilianDefense.generateMoves();
		System.out.println(" elapsed ms: " + (System.currentTimeMillis() - before));

		System.out.println(" generated moves count: " + legalMoves.size());
		for (State state : legalMoves) {
			System.out.println(state);
		}
	}
}
