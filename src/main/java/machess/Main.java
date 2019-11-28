package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		System.out.println(newGame);

		State ply3 = newGame
				//king vs king
				.fromLegalMove(Field.E1, Field.C4)
				.fromLegalMove(Field.E8, Field.E6)
				.fromLegalMove(Field.F1, Field.F3);
				//test blocking king by normal piece
//				.fromLegalMove(Field.C2, Field.C3)
//				.fromLegalMove(Field.E7, Field.E5)
//				.fromLegalMove(Field.C3, Field.C4)
//				.fromLegalMove(Field.F8, Field.C5);
		System.out.println("ply3:"+ ply3);
//
//
//
		long before = System.currentTimeMillis();
		List<State> legalMovesThirdPly = ply3.generateLegalMoves();
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
