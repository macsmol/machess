package machess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static machess.Square.*;

public class PieceLists {
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

    private PieceLists(Square whiteKing, Square blackKing,
                       List<Square> whitePawns, List<Square> blackPawns,
                       List<Square> whiteKnights, List<Square> blackKnights,
                       List<Square> whiteBishops, List<Square> blackBishops,
                       List<Square> whiteRooks, List<Square> blackRooks,
                       List<Square> whiteQueens, List<Square> blackQueens
                       ) {
        this.whiteKing = whiteKing;
        this.whiteKing = blackKing;
        this.whitePawns = toArray(whitePawns, whitePawns.size());
        whitePawnsCount = (byte)whitePawns.size();
        this.blackPawns = toArray(blackPawns, blackPawns.size());
        blackPawnsCount = (byte)blackPawns.size();

        this.whiteKnights = toArray(whiteKnights, capacity(whiteKnights.size(), whitePawnsCount));
        whiteKnightsCount = (byte)whiteKnights.size();
        this.blackKnights = toArray(blackKnights, capacity(blackKnights.size(), blackPawnsCount));
        blackKnightsCount = (byte)blackKnights.size();

        this.whiteBishops = toArray(whiteBishops, capacity(whiteBishops.size(), whitePawnsCount));
        whiteBishopsCount = (byte)whiteBishops.size();
        this.blackBishops = toArray(blackBishops, capacity(blackBishops.size(), blackPawnsCount));
        blackBishopsCount = (byte)blackBishops.size();
        
        this.whiteRooks = toArray(whiteRooks, capacity(whiteRooks.size(), whitePawnsCount));
        whiteRooksCount = (byte)whiteRooks.size();
        this.blackRooks = toArray(blackRooks, capacity(blackRooks.size(), blackPawnsCount));
        blackRooksCount = (byte)blackRooks.size();
        
        this.whiteQueens = toArray(whiteQueens, capacity(whiteQueens.size(), whitePawnsCount));
        whiteQueensCount = (byte)whiteQueens.size();
        this.blackQueens = toArray(blackQueens, capacity(blackQueens.size(), blackPawnsCount));
        blackQueensCount = (byte)blackQueens.size();
    }

    private Square[] toArray(List<Square> list, int capacity) {
        Square[] array = new Square[capacity];
        return list.toArray(array);
    }

    private int capacity(int piecesCount, int pawnsCount) {
        return Math.min(piecesCount + pawnsCount, Config.PIECE_LIST_CAPACITY);
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

    public void promote(Square where, Content toWhat) {
        Square[] pieces = toWhat.isWhite ? whitePawns : blackPawns;
        byte piecesCount = toWhat.isWhite ? whitePawnsCount-- : blackPawnsCount--;
        for (int i = 0; i < piecesCount; i++) {
            if (pieces[i] == where) {
               pieces[i] = pieces[piecesCount - 1];
            }
        }
        switch (toWhat) {
            case BLACK_KNIGHT:
                pieces = blackKnights;
                piecesCount = blackKnightsCount++;
                break;
            case BLACK_BISHOP:
                pieces = blackBishops;
                piecesCount = blackBishopsCount++;
                break;
            case BLACK_ROOK:
                pieces = blackRooks;
                piecesCount = blackRooksCount++;
                break;
            case BLACK_QUEEN:
                pieces = blackQueens;
                piecesCount = blackQueensCount++;
                break;
            case WHITE_KNIGHT:
                pieces = whiteKnights;
                piecesCount = whiteKnightsCount++;
                break;
            case WHITE_BISHOP:
                pieces = whiteBishops;
                piecesCount = whiteBishopsCount++;
                break;
            case WHITE_ROOK:
                pieces = whiteRooks;
                piecesCount = whiteRooksCount++;
                break;
            case WHITE_QUEEN:
                pieces = whiteQueens;
                piecesCount = whiteQueensCount++;
                break;
            default:
                assert false : "Invalid piece to promote to: " + toWhat;
        }
        pieces[piecesCount] = where;
    }

    public Square getWhiteKing() {
        return whiteKing;
    }

    public Square getBlackKing() {
        return blackKing;
    }
    
    public static class Builder {
        private Square whiteKing;
        private Square blackKing;
        private List<Square> whitePawns = new ArrayList<>();
        private List<Square> blackPawns = new ArrayList<>();
        private List<Square> whiteKnights = new ArrayList<>();
        private List<Square> blackKnights = new ArrayList<>();
        private List<Square> whiteBishops = new ArrayList<>();
        private List<Square> blackBishops = new ArrayList<>();
        private List<Square> whiteRooks = new ArrayList<>();
        private List<Square> blackRooks = new ArrayList<>();
        private List<Square> whiteQueens = new ArrayList<>();
        private List<Square> blackQueens = new ArrayList<>();

        public void addWhitePawn(Square piece) {
            whitePawns.add(piece);
        }
        public void addBlackPawn(Square piece) {
            blackPawns.add(piece);
        }
        public void addWhiteKnight(Square piece) {
            whiteKnights.add(piece);
        }
        public void addBlackKnight(Square piece) {
            blackKnights.add(piece);
        }
        public void addWhiteBishop(Square piece) {
            whiteBishops.add(piece);
        }
        public void addBlackBishop(Square piece) {
            blackBishops.add(piece);
        }
        public void addWhiteRook(Square piece) {
            whiteRooks.add(piece);
        }
        public void addBlackRook(Square piece) {
            blackRooks.add(piece);
        }
        public void addWhiteQueen(Square piece) {
            whiteQueens.add(piece);
        }
        public void addBlackQueen(Square piece) {
            blackQueens.add(piece);
        }
        public void setWhiteKing(Square piece) {
            if (whiteKing != null) {
                throw new IllegalStateException("Duplicate white king: "+ whiteKing +" " + piece);
            }
            whiteKing = piece;
        }
        
        public void setBlackKing(Square piece) {
            if (blackKing != null) {
                throw new IllegalStateException("Duplicate black king: "+ blackKing +" " + piece);
            }
            blackKing = piece;
        }
        
        public PieceLists build() {
            return new PieceLists(whiteKing,blackKing, whitePawns, blackPawns,
                    whiteKnights, blackKnights, whiteBishops, blackBishops,
                    whiteRooks, blackRooks, whiteQueens,blackQueens);
        }
    }
}
