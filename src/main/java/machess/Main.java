package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		System.out.println(newGame);

		State game = newGame
				//king vs king
				.fromLegalMove(Field.B1, Field.B3)
				.fromLegalMove(Field.B8, Field.B6)
				.fromLegalMove(Field.C1, Field.C3)
				.fromLegalMove(Field.C8, Field.C6)
				.fromLegalMove(Field.D1, Field.D3)
				.fromLegalMove(Field.D8, Field.D6)
				.fromLegalMove(Field.F1, Field.F3)
				.fromLegalMove(Field.F8, Field.F6)
				.fromLegalMove(Field.G1, Field.G3)

				.fromLegalMove(Field.G8, Field.A3);
		System.out.println("game :"+ game + "\n\n\n\n");
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
