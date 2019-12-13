package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();

		State game = newGame
				// pieces pinned to king
				.fromLegalMove(Field.C2, Field.C3)
				.fromLegalMove(Field.D8, Field.E6)

				.fromLegalMove(Field.A1, Field.E5)
				.fromLegalMove(Field.A7, Field.A6)

				.fromLegalMove(Field.E1, Field.E3)
				.fromLegalMove(Field.C8, Field.G5)

				.fromLegalMove(Field.B2, Field.F4)
				.fromLegalMove(Field.H8, Field.A3)

				.fromLegalMove(Field.G1, Field.G3)
				.fromLegalMove(Field.A8, Field.H3);


				//TODO cut paste this to some test case


		System.out.println("game :"+ game + "\n");

//
//
//
		long before = System.currentTimeMillis();
		List<State> legalMovesThirdPly = game.generateLegalMoves();
		System.out.println(" elapsed ms: " + (System.currentTimeMillis() - before));

		System.out.println(" generated moves count: " + legalMovesThirdPly.size());
		for (State thirdPly : legalMovesThirdPly) {
			System.out.println(thirdPly);
//			List<State> legalMovesFourthPly = thirdPly.generateLegalMoves();
//			for (State fourhPly : legalMovesFourthPly) {
//				System.out.println(fourhPly);
//			}
		}
	}
}
