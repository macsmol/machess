package machess;

import java.util.Arrays;
import java.util.Comparator;

import static machess.Square.*;

class PieceLists {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.append("White: ")
                .append("king:").append(whiteKing).append(' ')
                .append(toString("Pawns:", whitePawns, whitePawnsCount))
                .append(toString("Knights:", whiteKnights, whiteKnightsCount))
                .append(toString("Bishops:", whiteBishops, whiteBishopsCount))
                .append(toString("Rooks:", whiteRooks, whiteRooksCount))
                .append(toString("Queens:", whiteQueens, whiteQueensCount))
                .append('\n')
                .append("Black: ")
                .append("king:").append(blackKing).append(' ')
                .append(toString("Pawns:", blackPawns, blackPawnsCount))
                .append(toString("Knights:", blackKnights, blackKnightsCount))
                .append(toString("Bishops:", blackBishops, blackBishopsCount))
                .append(toString("Rooks:", blackRooks, blackRooksCount))
                .append(toString("Queens:", blackQueens, blackQueensCount))
                .append('\n').toString();
    }

    private StringBuilder toString(String header, Square[] pieceList, int piecesCount) {
        StringBuilder sb = new StringBuilder(header);
        sb.append('(').append(piecesCount).append(")[");
        for (int i = 0; i < piecesCount; i++) {
            sb.append(pieceList[i]).append(',');
        }
        return sb.append("] ");
    }

    private Square whiteKing;
    private Square blackKing;

    final Square[] whitePawns;
    byte whitePawnsCount;
    final Square[] blackPawns;
    byte blackPawnsCount;
    
    final Square[] whiteKnights;
    byte whiteKnightsCount;
    final Square[] blackKnights;
    byte blackKnightsCount;
    
    final Square[] whiteBishops;
    byte whiteBishopsCount;
    final Square[] blackBishops;
    byte blackBishopsCount;
    
    final Square[] whiteRooks;
    byte whiteRooksCount;
    final Square[] blackRooks;
    byte blackRooksCount;
    
    final Square[] whiteQueens;
    byte whiteQueensCount;
    final Square[] blackQueens;
    byte blackQueensCount;

    private PieceLists(Square whiteKing, Square blackKing, 
               Square[] whitePawns, byte whitePawnsCount, Square[] blackPawns, byte blackPawnsCount,
               Square[] whiteKnights, byte whiteKnightsCount, Square[] blackKnights, byte blackKnightsCount,
               Square[] whiteBishops, byte whiteBishopsCount, Square[] blackBishops, byte blackBishopsCount, 
               Square[] whiteRooks, byte whiteRooksCount, Square[] blackRooks, byte blackRooksCount,
               Square[] whiteQueens, byte whiteQueensCount, Square[] blackQueens, byte blackQueensCount) {
        this.whiteKing = whiteKing;
        this.blackKing = blackKing;
        this.whitePawns = whitePawns;
        this.whitePawnsCount = whitePawnsCount;
        this.blackPawns = blackPawns;
        this.blackPawnsCount = blackPawnsCount;
        this.whiteKnights = whiteKnights;
        this.whiteKnightsCount = whiteKnightsCount;
        this.blackKnights = blackKnights;
        this.blackKnightsCount = blackKnightsCount;
        this.whiteBishops = whiteBishops;
        this.whiteBishopsCount = whiteBishopsCount;
        this.blackBishops = blackBishops;
        this.blackBishopsCount = blackBishopsCount;
        this.whiteRooks = whiteRooks;
        this.whiteRooksCount = whiteRooksCount;
        this.blackRooks = blackRooks;
        this.blackRooksCount = blackRooksCount;
        this.whiteQueens = whiteQueens;
        this.whiteQueensCount = whiteQueensCount;
        this.blackQueens = blackQueens;
        this.blackQueensCount = blackQueensCount;
    }

    public PieceLists() {
        whiteKing = Square.E1;
        blackKing = Square.E8;

        whitePawns = new Square[]{
                Square.A2, Square.B2, Square.C2, Square.D2, Square.E2, Square.F2, Square.G2, Square.H2
        };
        whitePawnsCount = 8;

        blackPawns = new Square[]{
                Square.A7, Square.B7, Square.C7, Square.D7, Square.E7, Square.F7, Square.G7, Square.H7
        };
        blackPawnsCount = 8;

        whiteKnights = new Square[Config.PIECE_LIST_CAPACITY];
        whiteKnightsCount = 2;
        whiteKnights[0] = B1;
        whiteKnights[1] = G1;
        blackKnights = new Square[Config.PIECE_LIST_CAPACITY];
        blackKnightsCount = 2;
        blackKnights[0] = B8;
        blackKnights[1] = G8;

        whiteBishops = new Square[Config.PIECE_LIST_CAPACITY];
        whiteBishopsCount = 2;
        whiteBishops[0] = C1;
        whiteBishops[1] = F1;
        blackBishops = new Square[Config.PIECE_LIST_CAPACITY];
        blackBishopsCount = 2;
        blackBishops[0] = C8;
        blackBishops[1] = F8;

        whiteRooks = new Square[Config.PIECE_LIST_CAPACITY];
        whiteRooksCount = 2;
        whiteRooks[0] = A1;
        whiteRooks[1] = H1;
        blackRooks = new Square[Config.PIECE_LIST_CAPACITY];
        blackRooksCount = 2;
        blackRooks[0] = A8;
        blackRooks[1] = H8;

        whiteQueens = new Square[Config.PIECE_LIST_CAPACITY];
        whiteQueensCount = 1;
        whiteQueens[0] = D1;
        blackQueens = new Square[Config.PIECE_LIST_CAPACITY];
        blackQueensCount = 1;
        blackQueens[0] = D8;
    }

    public void sortOccupiedSquares() {
        if (!Config.SORT_OCCUPIED_SQUARES) {
            return;
        }
        Comparator<Square> comparator = Comparator.comparingInt(Enum::ordinal);

        Arrays.sort(whitePawns, 0, whitePawnsCount, comparator);
        Arrays.sort(blackPawns, 0, blackPawnsCount, comparator);
        Arrays.sort(whiteKnights, 0, whiteKnightsCount, comparator);
        Arrays.sort(blackKnights, 0, blackKnightsCount, comparator);
        Arrays.sort(whiteBishops, 0, whiteBishopsCount, comparator);
        Arrays.sort(blackBishops, 0, blackBishopsCount, comparator);
        Arrays.sort(whiteRooks, 0, whiteRooksCount, comparator);
        Arrays.sort(blackRooks, 0, blackRooksCount, comparator);
        Arrays.sort(whiteQueens, 0, whiteQueensCount, comparator);
        Arrays.sort(blackQueens, 0, blackQueensCount, comparator);
    }
    
    @Override
    public PieceLists clone() {
        return new PieceLists(whiteKing, blackKing,
                whitePawns.clone(), whitePawnsCount, blackPawns.clone(), blackPawnsCount,
                whiteKnights.clone(), whiteKnightsCount, blackKnights.clone(), blackKnightsCount,
                whiteBishops.clone(), whiteBishopsCount, blackBishops.clone(), blackBishopsCount,
                whiteRooks.clone(), whiteRooksCount, blackRooks.clone(), blackRooksCount,
                whiteQueens.clone(), whiteQueensCount, blackQueens.clone(), blackQueensCount);
    }

    void move(Content piece, Square from, Square to) {
        Square[] movingPieces = null;
        int movingPiecesCount = 0;
        switch (piece) {
            case WHITE_PAWN:
                movingPieces = whitePawns;
                movingPiecesCount = whitePawnsCount;
                break;
            case BLACK_PAWN:
                movingPieces = blackPawns;
                movingPiecesCount = blackPawnsCount;
                break;
            case WHITE_KNIGHT:
                movingPieces = whiteKnights;
                movingPiecesCount = whiteKnightsCount;
                break;
            case BLACK_KNIGHT:
                movingPieces = blackKnights;
                movingPiecesCount = blackKnightsCount;
                break;
            case WHITE_BISHOP:
                movingPieces = whiteBishops;
                movingPiecesCount = whiteBishopsCount;
                break;
            case BLACK_BISHOP:
                movingPieces = blackBishops;
                movingPiecesCount = blackBishopsCount;
                break;
            case WHITE_ROOK:
                movingPieces = whiteRooks;
                movingPiecesCount = whiteRooksCount;
                break;
            case BLACK_ROOK:
                movingPieces = blackRooks;
                movingPiecesCount = blackRooksCount;
                break;
            case WHITE_QUEEN:
                movingPieces = whiteQueens;
                movingPiecesCount = whiteQueensCount;
                break;
            case BLACK_QUEEN:
                movingPieces = blackQueens;
                movingPiecesCount = blackQueensCount;
                break;
            case WHITE_KING:
                whiteKing = to;
                return;
            case BLACK_KING:
                blackKing = to;
                return;
            default:
                assert false : "Invalid piece to move: " + piece;
                return;
        }

        for (int i = 0; i < movingPiecesCount; i++) {
            if (movingPieces[i] == from) {
                movingPieces[i] = to;
                break;
            }
        }
    }

    void kill(Content piece, Square killedOn) {
        Square[] listWithKilledPiece = null;
        int piecesCount = -1;
        switch(piece) {
            case BLACK_PAWN:
                listWithKilledPiece = blackPawns;
                piecesCount = blackPawnsCount--;
                break;
            case BLACK_KNIGHT:
                listWithKilledPiece = blackKnights;
                piecesCount = blackKnightsCount--;
                break;
            case BLACK_BISHOP:
                listWithKilledPiece = blackBishops;
                piecesCount = blackBishopsCount--;
                break;
            case BLACK_ROOK:
                listWithKilledPiece = blackRooks;
                piecesCount = blackRooksCount--;
                break;
            case BLACK_QUEEN:
                listWithKilledPiece = blackQueens;
                piecesCount = blackQueensCount--;
                break;
            case WHITE_PAWN:
                listWithKilledPiece = whitePawns;
                piecesCount = whitePawnsCount--;
                break;
            case WHITE_KNIGHT:
                listWithKilledPiece = whiteKnights;
                piecesCount = whiteKnightsCount--;
                break;
            case WHITE_BISHOP:
                listWithKilledPiece = whiteBishops;
                piecesCount = whiteBishopsCount--;
                break;
            case WHITE_ROOK:
                listWithKilledPiece = whiteRooks;
                piecesCount = whiteRooksCount--;
                break;
            case WHITE_QUEEN:
                listWithKilledPiece = whiteQueens;
                piecesCount = whiteQueensCount--;
                break;
            default:
                assert false : "Invalid piece to take: " + piece;
        }

        for (int i = 0; i < piecesCount; i++) {
            if (listWithKilledPiece[i] == killedOn) {
                listWithKilledPiece[i] = listWithKilledPiece[piecesCount - 1];
                return;
            }
        }
        assert false : "Didn't find piece to kill on piece list " + piece + " " + killedOn;
    }
    

    public Square getWhiteKing() {
        return whiteKing;
    }

    public Square getBlackKing() {
        return blackKing;
    }
}
