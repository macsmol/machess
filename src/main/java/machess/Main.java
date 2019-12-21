package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		System.out.println("new game \n" + newGame);
		State game = newGame
				// pieces pinned to king
				.fromLegalMove(Square.C2, Square.C3)
				.fromLegalMove(Square.D8, Square.E6)

				.fromLegalMove(Square.A1, Square.E5)
				.fromLegalMove(Square.A7, Square.A6)

				.fromLegalMove(Square.E1, Square.E3)
				.fromLegalMove(Square.C8, Square.G5)

				.fromLegalMove(Square.B2, Square.F4)
				.fromLegalMove(Square.H8, Square.A3)

				.fromLegalMove(Square.G1, Square.G4)
				.fromLegalMove(Square.A8, Square.H3);


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
