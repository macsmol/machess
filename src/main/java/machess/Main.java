package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();

		PinnedPieces pins = new PinnedPieces();

		State game = newGame
				// pieces pinned to king
				.fromLegalMove(Field.C2, Field.C3)
				.fromLegalMove(Field.A8, Field.E6)
				.fromLegalMove(Field.A1, Field.E5)
				.fromLegalMove(Field.A7, Field.A6)
				.fromLegalMove(Field.E1, Field.E3)
				.fromLegalMove(Field.A6, Field.A5)
				.fromLegalMove(Field.H2, Field.H3)
				.fromLegalMove(Field.H8, Field.A3);

				//TODO cut paste this to some test case


		System.out.println("game :"+ game + "\n\n\n");

		game.updatePinnedPieces(pins);
		System.out.println(pins);
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
