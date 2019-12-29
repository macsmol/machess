package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		System.out.println("new game \n" + newGame);
		State game = newGame
				.fromLegalMove(Square.F2, Square.F4)
				.fromLegalMove(Square.E7, Square.E5)
				.fromLegalMove(Square.G1, Square.F3)
				.fromLegalMove(Square.D8, Square.H4)
;


		System.out.println("game :"+ game + "\n");

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
