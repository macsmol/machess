package machess;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		State game = new State();
		System.out.println("new game \n" + game);
		System.out.println(NegaMaxScorer.evaluate(game));

		game = game.fromLegalMove(Square.F2, Square.F4);
		System.out.println("game \n" + game);
		System.out.println(NegaMaxScorer.evaluate(game));


		game = game.fromLegalMove(Square.E7, Square.E5);
		System.out.println("game \n" + game);
		System.out.println(NegaMaxScorer.evaluate(game));

		game = game.fromLegalMove(Square.G1, Square.F3);
		System.out.println("game \n" + game);
		System.out.println(NegaMaxScorer.evaluate(game));

		game = game.fromLegalMove(Square.D8, Square.H4);
		System.out.println("game :"+ game);
		System.out.println(NegaMaxScorer.evaluate(game));

		game = game.fromLegalMove(Square.F3, Square.H4);
		System.out.println("game :"+ game);
		System.out.println(NegaMaxScorer.evaluate(game));
;
	}
}
