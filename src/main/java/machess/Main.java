package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State newGame = new State();
		System.out.println(newGame);

		State ply2 = newGame
				.fromTrustedMove(Field.E1, Field.A4)
				.fromTrustedMove(Field.E8, Field.A6);
		System.out.println("ply2:"+ ply2);
//
//		State kingsPawnGame = ply2
//				.fromTrustedMove(Field.E7, Field.E5);
//		System.out.println(kingsPawnGame);
//
//
//		long before = System.currentTimeMillis();
//		List<State> legalMovesThirdPly = kingsPawnGame.generateLegalMoves();
//		System.out.println(" elapsed ms: " + (System.currentTimeMillis() - before));
//
//		System.out.println(" generated moves count: " + legalMovesThirdPly.size());
//		for (State thirdPly : legalMovesThirdPly) {
//			System.out.println(thirdPly);
//			List<State> legalMovesFourthPly = thirdPly.generateLegalMoves();
//			for (State fourhPly : legalMovesFourthPly) {
//				System.out.println(fourhPly);
//			}
//		}
	}
}
