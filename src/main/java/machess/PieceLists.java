package machess;

import machess.board0x88.Square0x88;

import java.util.ArrayList;
import java.util.List;

public class PieceLists {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.append("White: ")
                .append("king:").append(Square0x88.toString(whiteKing)).append(' ')
                .append(toString("Pawns:", whitePawns, whitePawnsCount))
                .append(toString("Knights:", whiteKnights, whiteKnightsCount))
                .append(toString("Bishops:", whiteBishops, whiteBishopsCount))
                .append(toString("Rooks:", whiteRooks, whiteRooksCount))
                .append(toString("Queens:", whiteQueens, whiteQueensCount))
                .append('\n')
                .append("Black: ")
                .append("king:").append(Square0x88.toString(blackKing)).append(' ')
                .append(toString("Pawns:", blackPawns, blackPawnsCount))
                .append(toString("Knights:", blackKnights, blackKnightsCount))
                .append(toString("Bishops:", blackBishops, blackBishopsCount))
                .append(toString("Rooks:", blackRooks, blackRooksCount))
                .append(toString("Queens:", blackQueens, blackQueensCount))
                .append('\n').toString();
    }

    private StringBuilder toString(String header, byte[] pieceList, int piecesCount) {
        StringBuilder sb = new StringBuilder(header);
        sb.append('(').append(piecesCount).append(")[");
        for (int i = 0; i < piecesCount; i++) {
            sb.append(Square0x88.toString(pieceList[i])).append(',');
        }
        return sb.append("] ");
    }

    private byte whiteKing;
    private byte blackKing;

    final byte[] whitePawns;
    // TODO remove count and capacity for all pieces. No need for it as the arrays are copied for every makeMove() anyway.
    byte whitePawnsCount;
    final byte[] blackPawns;
    byte blackPawnsCount;
    
    final byte[] whiteKnights;
    byte whiteKnightsCount;
    final byte[] blackKnights;
    byte blackKnightsCount;
    
    final byte[] whiteBishops;
    byte whiteBishopsCount;
    final byte[] blackBishops;
    byte blackBishopsCount;
    
    final byte[] whiteRooks;
    byte whiteRooksCount;
    final byte[] blackRooks;
    byte blackRooksCount;
    
    final byte[] whiteQueens;
    byte whiteQueensCount;
    final byte[] blackQueens;
    byte blackQueensCount;

    private PieceLists(byte whiteKing, byte blackKing,
               byte[] whitePawns, byte whitePawnsCount, byte[] blackPawns, byte blackPawnsCount,
               byte[] whiteKnights, byte whiteKnightsCount, byte[] blackKnights, byte blackKnightsCount,
               byte[] whiteBishops, byte whiteBishopsCount, byte[] blackBishops, byte blackBishopsCount,
               byte[] whiteRooks, byte whiteRooksCount, byte[] blackRooks, byte blackRooksCount,
               byte[] whiteQueens, byte whiteQueensCount, byte[] blackQueens, byte blackQueensCount) {
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
        whiteKing = Square0x88.E1;
        blackKing = Square0x88.E8;

        whitePawns = new byte[] {
                Square0x88.A2, Square0x88.B2, Square0x88.C2, Square0x88.D2, Square0x88.E2, Square0x88.F2, Square0x88.G2, Square0x88.H2
        };
        whitePawnsCount = 8;

        blackPawns = new byte[] {
                Square0x88.A7, Square0x88.B7, Square0x88.C7, Square0x88.D7, Square0x88.E7, Square0x88.F7, Square0x88.G7, Square0x88.H7
        };
        blackPawnsCount = 8;

        whiteKnights = new byte[Config.PIECE_LIST_CAPACITY];
        whiteKnightsCount = 2;
        whiteKnights[0] = Square0x88.B1;
        whiteKnights[1] = Square0x88.G1;
        blackKnights = new byte[Config.PIECE_LIST_CAPACITY];
        blackKnightsCount = 2;
        blackKnights[0] = Square0x88.B8;
        blackKnights[1] = Square0x88.G8;

        whiteBishops = new byte[Config.PIECE_LIST_CAPACITY];
        whiteBishopsCount = 2;
        whiteBishops[0] = Square0x88.C1;
        whiteBishops[1] = Square0x88.F1;
        blackBishops = new byte[Config.PIECE_LIST_CAPACITY];
        blackBishopsCount = 2;
        blackBishops[0] = Square0x88.C8;
        blackBishops[1] = Square0x88.F8;

        whiteRooks = new byte[Config.PIECE_LIST_CAPACITY];
        whiteRooksCount = 2;
        whiteRooks[0] = Square0x88.A1;
        whiteRooks[1] = Square0x88.H1;
        blackRooks = new byte[Config.PIECE_LIST_CAPACITY];
        blackRooksCount = 2;
        blackRooks[0] = Square0x88.A8;
        blackRooks[1] = Square0x88.H8;

        whiteQueens = new byte[Config.PIECE_LIST_CAPACITY];
        whiteQueensCount = 1;
        whiteQueens[0] = Square0x88.D1;
        blackQueens = new byte[Config.PIECE_LIST_CAPACITY];
        blackQueensCount = 1;
        blackQueens[0] = Square0x88.D8;
    }

    private PieceLists(byte whiteKing, byte blackKing,
                       List<Byte> whitePawns, List<Byte> blackPawns,
                       List<Byte> whiteKnights, List<Byte> blackKnights,
                       List<Byte> whiteBishops, List<Byte> blackBishops,
                       List<Byte> whiteRooks, List<Byte> blackRooks,
                       List<Byte> whiteQueens, List<Byte> blackQueens
                       ) {
        this.whiteKing = whiteKing;
        this.blackKing = blackKing;
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

    private byte[] toArray(List<Byte> list, int capacity) {
        byte[] array = new byte[capacity];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private int capacity(int piecesCount, int pawnsCount) {
        return Math.min(piecesCount + pawnsCount, Config.PIECE_LIST_CAPACITY);
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

    void move(Content piece, byte from, byte to) {
        byte[] movingPieces = null;
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

    void kill(Content piece, byte killedOn) {
        byte[] listWithKilledPiece = null;
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

    public void promote(byte where, Content toWhat) {
        byte[] pieces = toWhat.isWhite ? whitePawns : blackPawns;
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

    public byte getWhiteKing() {
        return whiteKing;
    }

    public byte getBlackKing() {
        return blackKing;
    }
    
    public static class Builder {
        private byte whiteKing = Square0x88.NULL;
        private byte blackKing = Square0x88.NULL;
        private List<Byte> whitePawns = new ArrayList<>();
        private List<Byte> blackPawns = new ArrayList<>();
        private List<Byte> whiteKnights = new ArrayList<>();
        private List<Byte> blackKnights = new ArrayList<>();
        private List<Byte> whiteBishops = new ArrayList<>();
        private List<Byte> blackBishops = new ArrayList<>();
        private List<Byte> whiteRooks = new ArrayList<>();
        private List<Byte> blackRooks = new ArrayList<>();
        private List<Byte> whiteQueens = new ArrayList<>();
        private List<Byte> blackQueens = new ArrayList<>();

        public void addWhitePawn(byte piece) {
            whitePawns.add(piece);
        }
        public void addBlackPawn(byte piece) {
            blackPawns.add(piece);
        }
        public void addWhiteKnight(byte piece) {
            whiteKnights.add(piece);
        }
        public void addBlackKnight(byte piece) {
            blackKnights.add(piece);
        }
        public void addWhiteBishop(byte piece) {
            whiteBishops.add(piece);
        }
        public void addBlackBishop(byte piece) {
            blackBishops.add(piece);
        }
        public void addWhiteRook(byte piece) {
            whiteRooks.add(piece);
        }
        public void addBlackRook(byte piece) {
            blackRooks.add(piece);
        }
        public void addWhiteQueen(byte piece) {
            whiteQueens.add(piece);
        }
        public void addBlackQueen(byte piece) {
            blackQueens.add(piece);
        }
        public void setWhiteKing(byte piece) {
            if (whiteKing != Square0x88.NULL) {
                throw new IllegalStateException("Duplicate white king: "+ whiteKing +" " + piece);
            }
            whiteKing = piece;
        }
        
        public void setBlackKing(byte piece) {
            if (blackKing != Square0x88.NULL) {
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
