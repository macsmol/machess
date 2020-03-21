package machess;

import machess.fen.FEN;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        State game = FEN.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");

        System.out.println("new game \n" + game);

        for (int ply = 1; ply < 150; ply++) {
            Scorer.MoveScore bestMove = Scorer.miniMax(game);

            if (bestMove.moveIndex == -1) {
                String winMessage = bestMove.score > 0 ? "white win!" : "black win!";
                System.out.println("Game over: " + winMessage);
                break;
            }
            game = game.chooseMove(bestMove.moveIndex);
            System.out.println(game);
        }
    }

    private static State testEnPassantPinWhiteKing() {
        return new State()
                .fromPseudoLegalMove(Square.E1, Square.E5)
                .fromPseudoLegalMove(Square.A8, Square.A5)
                .fromPseudoLegalMove(Square.B2, Square.B5)
                .fromPseudoLegalPawnDoublePush(Square.C7, Square.C5, Square.C6)
                ;
    }

    private static State testEnPassantPinBlackKing() {
        return new State()
                .fromPseudoLegalMove(Square.A1, Square.A4)
                .fromPseudoLegalMove(Square.E8, Square.D4)
                .fromPseudoLegalMove(Square.H2, Square.H3) // white padding
                .fromPseudoLegalMove(Square.C7, Square.C4)
                .fromPseudoLegalPawnDoublePush(Square.B2, Square.B4, Square.B3)
                ;
    }

    private static State testEnPassantPin2BlackKing() {
        return new State()
                .fromPseudoLegalMove(Square.H2, Square.H3) // white padding
                .fromPseudoLegalMove(Square.E8, Square.E4)

                .fromPseudoLegalMove(Square.D1, Square.B4)
                .fromPseudoLegalMove(Square.D7, Square.D4)

                .fromPseudoLegalMove(Square.A1, Square.A4)
                .fromPseudoLegalMove(Square.H7, Square.H6) // black padding

                .fromPseudoLegalPawnDoublePush(Square.C2, Square.C4, Square.C3)
                ;
    }

    private static State testPins() {
        return new State()
                .fromPseudoLegalMove(Square.C1, Square.E3)
                .fromPseudoLegalMove(Square.E8, Square.E6)

                .fromPseudoLegalMove(Square.H2, Square.H3)
                .fromPseudoLegalMove(Square.E7, Square.E5)

                .fromPseudoLegalMove(Square.D1, Square.B3)
                .fromPseudoLegalMove(Square.D7, Square.D5);
    }
}
