package machess;

import machess.board0x88.Direction;
import machess.board0x88.Square0x88;
import machess.board8x8.File;
import machess.board8x8.Rank;
import machess.board8x8.Square;

import java.util.ArrayList;
import java.util.List;

import static machess.board0x88.Square0x88.*;

/**
 * Game state seen as game board rather than a list of figures.
 * All is static in order to avoid allocations.
 */
public class State {
	public static final boolean WHITE = true;
	public static final boolean BLACK = false;

	/**
	 * Bit layout of each square is
	 * bbbbwwww--BWcppp
	 *
	 * p - piece type
	 * c - is piece white flag
	 * W - check by white king
	 * B - check by black king
	 * w - checks by white count
	 * b - checks by black count
	 */
	public static final class SquareFormat {
		static final short PIECE_TYPE_MASK 		= 0x07;
		static final short IS_WHITE_PIECE_FLAG 	= 0x08;

		static final short CHECK_BY_WHITE_KING 	= 0x10;
		static final short CHECK_BY_BLACK_KING 	= 0x20;

		// mask to get checks count by black or white. Four MSBits are checks by black, next 4 bits are checks by white.
		static final short CHECKS_COUNT_MASK 	= 0x0F;
		static final byte CHECKS_BY_WHITE_BIT_OFFSET = 8;
		static final byte CHECKS_BY_BLACK_BIT_OFFSET = 12;
	}

	// flags
	private byte flags;
	public static final int WHITE_TURN					= 0x01;
	public static final int WHITE_KS_CASTLE_POSSIBLE	= 0x02;
	public static final int WHITE_QS_CASTLE_POSSIBLE 	= 0x04;
	public static final int BLACK_KS_CASTLE_POSSIBLE 	= 0x08;
	public static final int BLACK_QS_CASTLE_POSSIBLE 	= 0x10;

	/**
	 * https://www.chessprogramming.org/0x88
	 */
	final short[] board0x88;

	final PieceLists pieces;
	/**
	 * If not null it means there is a possibility to en-passant on this square
	 */
	private final byte enPassantSquare;

	// TODO 50 move draw rule
	private final byte halfmoveClock;

	// not really necessary - only for debug purposes
	private final int fullMoveCounter;

	/**
	 * Board with absolutely pinned pieces. It's indexed by Square.ordinal()
	 */
	private final Pin[] pinnedPieces;

	// for printing move
	byte from;
	byte to;
	Content promotion;

	/**
	 * new game
	 */
	public State() {
		flags = WHITE_TURN |
				WHITE_QS_CASTLE_POSSIBLE | WHITE_KS_CASTLE_POSSIBLE |
				BLACK_QS_CASTLE_POSSIBLE | BLACK_KS_CASTLE_POSSIBLE;
		board0x88 = new short[128];
		for (int file = File.A; file <= File.H; file++) {
			board0x88[Square0x88.from07(file, Rank._2)] = Content.WHITE_PAWN.asByte;
			board0x88[Square0x88.from07(file, Rank._7)] = Content.BLACK_PAWN.asByte;

			switch (file) {
				case File.A:
				case File.H:
					board0x88[Square0x88.from07(file, Rank._1)] = Content.WHITE_ROOK.asByte;
					board0x88[Square0x88.from07(file, Rank._8)] = Content.BLACK_ROOK.asByte;
					break;
				case File.B:
				case File.G:
					board0x88[Square0x88.from07(file, Rank._1)] = Content.WHITE_KNIGHT.asByte;
					board0x88[Square0x88.from07(file, Rank._8)] = Content.BLACK_KNIGHT.asByte;
					break;
				case File.C:
				case File.F:
					board0x88[Square0x88.from07(file, Rank._1)] = Content.WHITE_BISHOP.asByte;
					board0x88[Square0x88.from07(file, Rank._8)] = Content.BLACK_BISHOP.asByte;
					break;
				case File.D:
					board0x88[Square0x88.from07(file, Rank._1)] = Content.WHITE_QUEEN.asByte;
					board0x88[Square0x88.from07(file, Rank._8)] = Content.BLACK_QUEEN.asByte;
					break;
				case File.E:
					board0x88[Square0x88.from07(file, Rank._1)] = Content.WHITE_KING.asByte;
					board0x88[Square0x88.from07(file, Rank._8)] = Content.BLACK_KING.asByte;
					break;
			}
		}
		pieces = new PieceLists();
		enPassantSquare = NULL;
		halfmoveClock = 0;
		fullMoveCounter = 1;

		pinnedPieces = new Pin[Square.values().length];
		initChecksAroundKings();
	}

	public State(short[] board0x88, PieceLists pieces, byte flags,
				 byte enPassantSquare, byte halfmoveClock, int fullMoveCounter, byte from, byte to) {
		this.board0x88 = board0x88;
		this.pieces = pieces;
		this.flags = flags;
		this.enPassantSquare = enPassantSquare;
		this.halfmoveClock = halfmoveClock;
		this.fullMoveCounter = fullMoveCounter;
		this.from = from;
		this.to = to;

		resetSquaresInCheck();
		initChecksAroundKings();

		pinnedPieces = new Pin[Square.values().length];
		initPinnedPieces();
	}

	State fromPseudoLegalPawnDoublePush(byte from, byte to, byte enPassantSquare) {
		assert inBounds(enPassantSquare);
		if (!isEnPassantLegal(to)) {
			enPassantSquare = NULL;
		}
		return fromPseudoLegalMove(from, to, null, enPassantSquare, NULL);
	}

	/**
	 * En passant can sometimes be illegal due to an absolute pin, eg.
	 * 8/8/8/8/RPpk4/8/8/4K3 b - b3 0 1
	 * Program detects such situation before double-push is made
	 */
	private boolean isEnPassantLegal(byte doublePushTo) {
		byte king = getRank(doublePushTo) == Rank._4 ? pieces.getBlackKing() : pieces.getWhiteKing();
		if (getRank(king) != getRank(doublePushTo)) {
			return true;
		}
		byte[] rooks = getRank(doublePushTo) == Rank._4 ? pieces.whiteRooks : pieces.blackRooks;
		int rooksCount = getRank(doublePushTo) == Rank._4 ? pieces.whiteRooksCount : pieces.blackRooksCount;
		if (!isEnPassantLegal(king, rooks, rooksCount)) {
			return false;
		}
		byte[] queens = getRank(doublePushTo) == Rank._4 ? pieces.whiteQueens : pieces.blackQueens;
		int queensCount = getRank(doublePushTo) == Rank._4 ? pieces.whiteQueensCount : pieces.blackQueensCount;
		return isEnPassantLegal(king, queens, queensCount);
	}

	private boolean isEnPassantLegal(byte king, byte[] rooklikes, int rooklikesCount) {
		for (int i = 0; i < rooklikesCount; i++) {
			if (getRank(rooklikes[i]) != getRank(king)) {
				continue;
			}
			int fileFrom = Math.min(getFile(king), getFile(rooklikes[i])) + 1;
			int fileTo = Math.max(getFile(king), getFile(rooklikes[i]));
			int piecesBetweenCount = 0;
			for (int file = fileFrom; file < fileTo; file++) {
				if (getContent(file, getRank(king)) != Content.EMPTY) {
					piecesBetweenCount++;
				}
			}
			if (piecesBetweenCount == 1) {
				return false;
			}
		}
		return true;
	}

	State fromPseudoLegalMoveWithPromotion(byte from, byte to, Content promotion) {
		assert promotion != null;
		State nextState = fromPseudoLegalMove(from, to, promotion, NULL, NULL);
		nextState.promotion = promotion;
		return nextState;
	}

	State fromLegalQueensideCastling(byte kingFrom, byte kingTo) {
		byte rookToCastle = Square0x88.from07(File.A,  getRank(kingFrom));
		return fromPseudoLegalMove(kingFrom, kingTo, null, NULL, rookToCastle);
	}

	State fromLegalKingsideCastling(byte kingFrom, byte kingTo) {
		byte rookToCastle = Square0x88.from07(File.H, getRank(kingFrom));
		return fromPseudoLegalMove(kingFrom, kingTo, null, NULL, rookToCastle);
	}

	/**
	 * Generates new BoardState based on move. Typical move without special events.
	 */
	State fromPseudoLegalMove(byte from, byte to) {
		return fromPseudoLegalMove(from, to, null, NULL, NULL);
	}

	/**
	 * Generates new BoardState based on move. It does not verify game rules - assumes input is a legal move.
	 * This is the root method - it covers all cases. All 'overload' methods should call this one.
	 */
	private State fromPseudoLegalMove(byte from, byte to, Content promotion, byte futureEnPassantSquare,
									  byte rookCastleFrom) {
		assert from != to : from + "->" + to + " is no move";
		assert inBounds(from) : "invalid from square: " + from;
		assert inBounds(to) : "invalid to square: " + to;
		short[] board0x88Copy = board0x88.clone();
		PieceLists piecesCopy = pieces.clone();

		//  update boardCopy
		Content movedPiece = Content.fromShort(board0x88Copy[from]);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		assert movedPiece.isWhite == test(WHITE_TURN) : "Moved " + movedPiece + " on " + (test(WHITE_TURN) ? "white" : "black") + " turn";
		board0x88Copy[from] = Content.EMPTY.asByte;

		Content takenPiece = Content.fromShort(board0x88Copy[to]);
		assert takenPiece != Content.BLACK_KING && takenPiece != Content.WHITE_KING : from + "->" + to + " is taking king";
		board0x88Copy[to] = movedPiece.asByte;

		piecesCopy.move(movedPiece, from, to);

		byte squareWithPawnTakenEnPassant = NULL;
		if (enPassantSquare == to) {
			if (movedPiece == Content.WHITE_PAWN) {
				squareWithPawnTakenEnPassant = Direction.move(to, Direction.S);
				takenPiece = Content.fromShort(board0x88Copy[squareWithPawnTakenEnPassant]);
				board0x88Copy[squareWithPawnTakenEnPassant] = Content.EMPTY.asByte;
			} else if (movedPiece == Content.BLACK_PAWN) {
				squareWithPawnTakenEnPassant = Direction.move(to, Direction.N);
				takenPiece = Content.fromShort(board0x88Copy[squareWithPawnTakenEnPassant]);
				board0x88Copy[squareWithPawnTakenEnPassant] = Content.EMPTY.asByte;
			}
		} else if (rookCastleFrom != NULL) {
			board0x88Copy[rookCastleFrom] = Content.EMPTY.asByte;
			Content rook = test(WHITE_TURN) ? Content.WHITE_ROOK : Content.BLACK_ROOK;
			byte rookDestination;
			if (getFile(rookCastleFrom) == File.A) {
				rookDestination = test(WHITE_TURN) ? Square0x88.D1 : Square0x88.D8;
				board0x88Copy[rookDestination] = rook.asByte;
			} else {
				rookDestination = test(WHITE_TURN) ? Square0x88.F1 : Square0x88.F8;
				board0x88Copy[rookDestination] = rook.asByte;
			}
			// update pieces lists
			piecesCopy.move(rook, rookCastleFrom, rookDestination);
		} else if (promotion != null) {
			board0x88Copy[to] = promotion.asByte;
			piecesCopy.promote(to, promotion);
		}

		if (takenPiece != Content.EMPTY) {
			assert movedPiece.isWhite != takenPiece.isWhite : from + "->" + to + " is a friendly take";
			piecesCopy.kill(takenPiece, squareWithPawnTakenEnPassant != NULL ? squareWithPawnTakenEnPassant : to);
		}

		int flagsCopy = flags ^ WHITE_TURN;
		if (from == Square0x88.E1) {
			flagsCopy &= ~(WHITE_KS_CASTLE_POSSIBLE | WHITE_QS_CASTLE_POSSIBLE);
		} else if (from == Square0x88.E8) {
			flagsCopy &= ~(BLACK_KS_CASTLE_POSSIBLE | BLACK_QS_CASTLE_POSSIBLE);
		} else if (from == Square0x88.A1) {
			flagsCopy &= ~WHITE_QS_CASTLE_POSSIBLE;
		} else if (from == Square0x88.H1) {
			flagsCopy &= ~WHITE_KS_CASTLE_POSSIBLE;
		} else if (from == Square0x88.A8) {
			flagsCopy &= ~BLACK_QS_CASTLE_POSSIBLE;
		} else if (from == Square0x88.H8) {
			flagsCopy &= ~BLACK_KS_CASTLE_POSSIBLE;
		}
		if (to == A1) {
			flagsCopy &= ~WHITE_QS_CASTLE_POSSIBLE;
		} else if (to == H1) {
			flagsCopy &= ~WHITE_KS_CASTLE_POSSIBLE;
		} else if (to == A8) {
			flagsCopy &= ~BLACK_QS_CASTLE_POSSIBLE;
		} else if (to == H8) {
			flagsCopy &= ~BLACK_KS_CASTLE_POSSIBLE;
		}

		int newFullMoveClock = test(WHITE_TURN) ? fullMoveCounter : fullMoveCounter + 1;
		return new State(board0x88Copy, piecesCopy, (byte) flagsCopy, futureEnPassantSquare, (byte)0, newFullMoveClock,
				from, to);
	}

	public boolean test(int flagMask) {
		return (flags & flagMask) != 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: ").append(test(WHITE_TURN) ? "WHITE" : "BLACK");

		sb.append(test(WHITE_KS_CASTLE_POSSIBLE) ? "; WHITE_KS_CASTLE_POSSIBLE" : "");
		sb.append(test(WHITE_QS_CASTLE_POSSIBLE) ? "; WHITE_QS_CASTLE_POSSIBLE" : "");
		sb.append(test(BLACK_KS_CASTLE_POSSIBLE) ? "; BLACK_KS_CASTLE_POSSIBLE" : "");
		sb.append(test(BLACK_QS_CASTLE_POSSIBLE) ? "; BLACK_QS_CASTLE_POSSIBLE" : "");
		sb.append("\n | a  | b  | c  | d  | e  | f  | g  | h  |");
		sb.append(Config.DEBUG_FIELD_IN_CHECK_FLAGS	? "| a  | b  | c  | d  | e  | f  | g  | h  |" : "");
		sb.append(Config.DEBUG_PINNED_PIECES 		? "| a  | b  | c  | d  | e  | f  | g  | h  |" : "");
		sb.append('\n');
		sb.append("==========================================");
		sb.append(Config.DEBUG_FIELD_IN_CHECK_FLAGS	? "=========================================" : "");
		sb.append(Config.DEBUG_PINNED_PIECES 		? "=========================================" : "");
		sb.append('\n');
		for (byte rank = Rank._8; rank >= Rank._1; rank--) {
			StringBuilder sbCheckFlags = 	new StringBuilder(Config.DEBUG_FIELD_IN_CHECK_FLAGS 	? "|" : "");
			StringBuilder sbPins = 			new StringBuilder(Config.DEBUG_PINNED_PIECES 			? "|" : "");
			sb.append(rank + 1).append("|");
			for (byte file = File.A; file <= File.H; file++) {
				byte square = Square0x88.from07(file, rank);
				Content content = getContent(file, rank);
				sb.append(content.symbol);
				if (square == to || square == from) {
					sb.append(" <");
				} else {
					sb.append(" |");
				}

				if (Config.DEBUG_FIELD_IN_CHECK_FLAGS) {
					short contentAsShort = board0x88[Square0x88.from07(file,rank)];
					sbCheckFlags.append(Utils.checkCountsToString(contentAsShort)).append('|');
				}
				if (Config.DEBUG_PINNED_PIECES) {
					Pin pinType = pinnedPieces[Square0x88.to8x8Square(square)];
					sbPins.append(" ").append(pinType != null ? pinType.symbol : ' ').append("  |");
				}
			}
			sb.append(sbCheckFlags).append(sbPins)
					.append("\n-+----+----+----+----+----+----+----+----+")
					.append(Config.DEBUG_FIELD_IN_CHECK_FLAGS 	? "+----+----+----+----+----+----+----+----+" : "")
					.append(Config.DEBUG_PINNED_PIECES 			? "+----+----+----+----+----+----+----+----+" : "")
					.append('\n');
		}
		sb.append(pieces);
		sb.append("enPassantSquare: ").append(Square0x88.toString(enPassantSquare)).append('\n');
		sb.append("fullmoveClock: ").append(fullMoveCounter).append('\n');
		return sb.toString();
	}

	private Content getContent(int file, int rank) {
		return getContent(Square0x88.from07(file, rank));
	}

	Content getContent(byte square0x88) {
		return Content.fromShort(board0x88[square0x88]);
	}

	private boolean isPromotingSquare(byte square0x88) {
		return test(WHITE_TURN) ? getRank(square0x88) == Rank.WHITE_PROMOTION_RANK : getRank(square0x88) == Rank.BLACK_PROMOTION_RANK;
	}

	private boolean isInitialSquareOfPawn(byte square0x88) {
		return test(WHITE_TURN) ? getRank(square0x88) == Rank.WHITE_PAWN_INITIAL_RANK : getRank(square0x88) == Rank.BLACK_PAWN_INITIAL_RANK;
	}

	/**
	 * Tells if Square square is occupied by a piece of color that's currently taking turn.
	 */
	boolean isSameColorPieceOn(byte square0x88) {
		return test(WHITE_TURN) ? isWhitePieceOn(square0x88) : isBlackPieceOn(square0x88);
	}

	/**
	 * Tells if Square square is occupied by a piece of color that's currently taking turn.
	 */
	boolean isOppositeColorPieceOn(byte square0x88) {
		return test(WHITE_TURN) ? isBlackPieceOn(square0x88) : isWhitePieceOn(square0x88);
	}

	private boolean isWhitePieceOn(byte square0x88) {
		short contentAsShort = board0x88[square0x88];
		return (contentAsShort & SquareFormat.IS_WHITE_PIECE_FLAG) != 0 && (contentAsShort & SquareFormat.PIECE_TYPE_MASK) != 0;
	}

	private boolean isBlackPieceOn(byte square0x88) {
		short contentAsShort = board0x88[square0x88];
		return (contentAsShort & SquareFormat.IS_WHITE_PIECE_FLAG) == 0 && (contentAsShort & SquareFormat.PIECE_TYPE_MASK) != 0;
	}

	private void resetSquaresInCheck() {
		for (int i = 0; i < board0x88.length; i++) {
			short contentAsShort = board0x88[i];
			board0x88[i] = (byte) (contentAsShort & (SquareFormat.PIECE_TYPE_MASK | SquareFormat.IS_WHITE_PIECE_FLAG));
		}
	}

	private void initChecksAroundKings() {
		initChecksAroundKing(BLACK);
		initChecksAroundKing(WHITE);
		initSquaresInCheckByKings();
	}

	private void initPinnedPieces() {
		byte whiteKing = pieces.getWhiteKing();
		byte blackKing = pieces.getBlackKing();
		assert getContent(whiteKing) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing) == Content.BLACK_KING : "Corrupted black king position";

		for (Square sq : Square.values()) {
			pinnedPieces[sq.ordinal()] = null;
		}

		initPinnedPieces(whiteKing, WHITE);
		initPinnedPieces(blackKing, BLACK);
	}

	private void initPinnedPieces(byte king, boolean isPinnedToWhiteKing) {
		initPinsByBishops(king, isPinnedToWhiteKing);
		initPinsByRooks(king, isPinnedToWhiteKing);
		initPinsByQueens(king, isPinnedToWhiteKing);
	}

	// TODO could use some smarter 0x88 arithmetic for this?
	private void initPinsByBishops(byte king, boolean isPinnedToWhiteKing) {

		byte[] bishops = isPinnedToWhiteKing ? pieces.blackBishops : pieces.whiteBishops;
		int bishopsCount = isPinnedToWhiteKing ? pieces.blackBishopsCount : pieces.whiteBishopsCount;

		for (int i = 0; i < bishopsCount; i++) {
			int deltaRank = getRank(bishops[i]) - getRank(king);
			int deltaFile = getFile(bishops[i]) - getFile(king);

			if (Math.abs(deltaFile) == Math.abs(deltaRank)) {
				initPinByBishop(king, isPinnedToWhiteKing, bishops[i]);
			}
		}
	}
	
	private void initPinsByRooks(byte king0x88, boolean isPinnedToWhiteKing) {
		byte[] rooks = isPinnedToWhiteKing ? pieces.blackRooks : pieces.whiteRooks;
		int rooksCount = isPinnedToWhiteKing ? pieces.blackRooksCount : pieces.whiteRooksCount;

		for (int i = 0; i < rooksCount; i++) {
			if (getRank(king0x88) == getRank(rooks[i])) {
				initRankPin(king0x88, isPinnedToWhiteKing, rooks[i]);
			} else if (getFile(king0x88) == getFile(rooks[i])) {
				initFilePin(king0x88, isPinnedToWhiteKing, rooks[i]);
			}
		}
	}

	private void initPinsByQueens(byte king, boolean isPinnedToWhiteKing) {
		byte[] queens = isPinnedToWhiteKing ? pieces.blackQueens : pieces.whiteQueens;
		int queensCount = isPinnedToWhiteKing ? pieces.blackQueensCount : pieces.whiteQueensCount;

		for (int i = 0; i < queensCount; i++) {
			int deltaRank = getRank(queens[i]) - getRank(king);
			int deltaFile = getFile(queens[i]) - getFile(king);

			if (Math.abs(deltaFile) == Math.abs(deltaRank)) {
				initPinByBishop(king, isPinnedToWhiteKing, queens[i]);
			} else if (getRank(king) == getRank(queens[i])) {
				initRankPin(king, isPinnedToWhiteKing, queens[i]);
			} else if (getFile(king) == getFile(queens[i])) {
				initFilePin(king, isPinnedToWhiteKing, queens[i]);
			}
		}
	}

	private void initPinByBishop(byte king0x88, boolean isKingWhite, byte bishoplike0x88) {
		if (getFile(bishoplike0x88) - getFile(king0x88) > 0) {
			if (getRank(bishoplike0x88) - getRank(king0x88) > 0) {
				initPin(king0x88, isKingWhite, bishoplike0x88, Direction.NE);
			} else {
				initPin(king0x88, isKingWhite, bishoplike0x88, Direction.SE);
			}
		} else {
			if (getRank(bishoplike0x88) - getRank(king0x88) > 0) {
				initPin(king0x88, isKingWhite, bishoplike0x88, Direction.NW);
			} else {
				initPin(king0x88, isKingWhite, bishoplike0x88, Direction.SW);
			}
		}
	}

	private void initRankPin(byte king0x88, boolean isKingWhite, byte rooklike0x88) {
		byte direction = getFile(rooklike0x88) - getFile(king0x88) > 0 ? Direction.E : Direction.W;
		initPin(king0x88, isKingWhite, rooklike0x88, direction);
	}

	private void initFilePin(byte king0x88, boolean isKingWhite, byte rooklike0x88) {
		byte direction = getRank(rooklike0x88) - getRank(king0x88) > 0 ? Direction.N : Direction.S;
		initPin(king0x88, isKingWhite, rooklike0x88, direction);
	}

	private void initPin(byte king, boolean isKingWhite, byte slidingPieceSquare, byte direction) {
		int candidate8x8index = -1;
		byte testedSquare = Direction.move(king, direction);
		while (true) {
			if (testedSquare == slidingPieceSquare) {
				if (candidate8x8index != -1) {
					pinnedPieces[candidate8x8index] = Pin.fromDirection(direction);
				}
				return;
			}
			Content content = getContent(testedSquare);
			if (content != Content.EMPTY) {
				if (content.isWhite != isKingWhite) {
					// obstructed by piece of same color as sliding piece
					return;
				}
				if (candidate8x8index == -1) {
					candidate8x8index = Square0x88.to8x8Square(testedSquare);
				} else {
					// found second piece obstructing ray
					return;
				}
			}
			testedSquare = Direction.move(testedSquare, direction);
		}
	}

	// TODO can it be improved with 'smart' 0x88 arithmetic?
	private void initChecksAroundKing(boolean isCheckedByWhite) {
		byte king = isCheckedByWhite ? pieces.getBlackKing() : pieces.getWhiteKing();
		boolean castlingImpossible;
		if (isCheckedByWhite) {
			castlingImpossible = !(test(BLACK_KS_CASTLE_POSSIBLE) || test(BLACK_QS_CASTLE_POSSIBLE));
		} else {
			castlingImpossible = !(test(WHITE_KS_CASTLE_POSSIBLE) || test(WHITE_QS_CASTLE_POSSIBLE));
		}

		if (isCheckedByWhite) {
			initChecksByPawns(king, castlingImpossible, isCheckedByWhite, pieces.whitePawns, pieces.whitePawnsCount);
			initChecksByKnights(king, castlingImpossible, isCheckedByWhite, pieces.whiteKnights, pieces.whiteKnightsCount);
			initChecksByBishops(king, isCheckedByWhite, pieces.whiteBishops, pieces.whiteBishopsCount);
			initChecksByRooks(king, isCheckedByWhite, pieces.whiteRooks, pieces.whiteRooksCount);
			initChecksByQueens(king, isCheckedByWhite, pieces.whiteQueens, pieces.whiteQueensCount);
		} else {
			initChecksByPawns(king, castlingImpossible, isCheckedByWhite, pieces.blackPawns, pieces.blackPawnsCount);
			initChecksByKnights(king, castlingImpossible, isCheckedByWhite, pieces.blackKnights, pieces.blackKnightsCount);
			initChecksByBishops(king, isCheckedByWhite, pieces.blackBishops, pieces.blackBishopsCount);
			initChecksByRooks(king, isCheckedByWhite, pieces.blackRooks, pieces.blackRooksCount);
			initChecksByQueens(king, isCheckedByWhite, pieces.blackQueens, pieces.blackQueensCount);
		}
	}

	private void initChecksByPawns(byte king, boolean castlingImpossible, boolean isCheckedByWhite,
								   byte[] pawns, byte pawnsCount) {
		if (pawnsCount == 0) {
			return;
		}

		Utils.sortByFiles(pawns, pawnsCount);

		byte minKingSafeDistance = (byte)(castlingImpossible ? 2 : 3);
		for (int i = pawnsCount / 2; i >= 0; i--) {
			if (getFile(pawns[i]) < getFile(king) - minKingSafeDistance) {
				break;
			}
			initSquaresInCheckByPawn(pawns[i], isCheckedByWhite);
		}
		for (int i = pawnsCount / 2 + 1; i < pawnsCount; i++) {
			if (getFile(pawns[i]) > getFile(king) + minKingSafeDistance) {
				break;
			}
			initSquaresInCheckByPawn(pawns[i], isCheckedByWhite);
		}
	}

	private void initChecksByKnights(byte king, boolean castlingImpossible, boolean isCheckedByWhite, byte[] knights,
			byte knightsCount) {
		byte minKingSafeDistance = (byte)(castlingImpossible ? 4 : 5);
		for (int i = 0; i < knightsCount; i++) {
			if (Math.abs(getFile(knights[i]) - getFile(king)) < minKingSafeDistance && Math.abs(getRank(knights[i]) - getRank(king)) < minKingSafeDistance) {
				initSquaresInCheckByKnight(knights[i],isCheckedByWhite);
			}
		}
	}

	private void initChecksByBishops(byte king0x88, boolean isCheckedByWhite, byte[] bishops, byte bishopsCount) {
		for (int i = 0; i < bishopsCount; i++) {
			byte deltaRank = (byte)(getRank(king0x88) - getRank(bishops[i]));

			if (deltaRank > 0) {
				// king above
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.NW);
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.NE);
			} else if (deltaRank < 0) {
				//king below
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.SW);
				initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.SE);
			} else {
				byte deltaFile = (byte) (getFile(king0x88) - getFile(bishops[i]));
				if (deltaFile > 0) { // king to the right
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.NE);
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.SE);
				} else {
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.NW);
					initCheckFlagsBySlidingPiece(bishops[i], isCheckedByWhite, Direction.SW);
				}
			}
		}
	}

	private void initChecksByRooks(byte king0x88, boolean isCheckedByWhite, byte[] rooks, byte rooksCount) {
		for (int i = 0; i < rooksCount; i++) {
			initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, Direction.N);
			initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, Direction.S);

			byte deltaRank = (byte)(getRank(king0x88) - getRank(rooks[i]));
			if (isCloseEnoughToRank(deltaRank)) {
				initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, Direction.E);
				initCheckFlagsBySlidingPiece(rooks[i], isCheckedByWhite, Direction.W);
			}
		}
	}

	private boolean isCloseEnoughToRank(byte deltaRank) {
		return Math.abs(deltaRank) <= 1;
	}

	private void initChecksByQueens(byte king, boolean isCheckedByWhite, byte[] queens, byte queensCount) {
		for (int i = 0; i < queensCount; i++) {
			byte deltaRank = (byte) (getRank(king) - getRank(queens[i]));
			if (deltaRank > 1) { // king at least two ranks above
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.NE);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.N);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.NW);
			} else if (deltaRank < -1) { // king at least two ranks below
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.SE);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.S);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.SW);
			} else if (deltaRank > 0) { // king just above
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.NE);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.N);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.NW);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.E);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.W);
			} else if (deltaRank < 0) { // king just below
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.SE);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.S);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.SW);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.E);
				initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.W);
			} else { // same rank
				byte deltaFile = (byte) (getFile(king) - getFile(queens[i]));
				if (deltaFile > 0) { // king to the right
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.N);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.NE);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.E);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.SE);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.S);
				} else { // king to the left
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.N);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.NW);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.W);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.SW);
					initCheckFlagsBySlidingPiece(queens[i], isCheckedByWhite, Direction.S);
				}
			}
		}
	}

	private void initSquaresInCheckByKnight(byte knightSquare0x88, boolean isCheckedByWhite) {
		byte to = Direction.move(knightSquare0x88,Direction.NNE);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Direction.move(knightSquare0x88,Direction.NEE);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Direction.move(knightSquare0x88,Direction.SEE);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Direction.move(knightSquare0x88,Direction.SSE);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Direction.move(knightSquare0x88,Direction.SSW);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Direction.move(knightSquare0x88,Direction.SWW);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Direction.move(knightSquare0x88,Direction.NWW);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
		to = Direction.move(knightSquare0x88,Direction.NNW);
		if (inBounds(to)) {
			incrementChecksOnSquare(to, isCheckedByWhite);
		}
	}

	private void initSquaresInCheckByKings() {
		byte whiteKing0x88 = pieces.getWhiteKing();
		byte blackKing0x88 = pieces.getBlackKing();
		assert getContent(whiteKing0x88) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing0x88) == Content.BLACK_KING : "Corrupted black king position";
		assert Math.abs(getRank(blackKing0x88) - getRank(whiteKing0x88)) > 1
				|| Math.abs(getFile(blackKing0x88) - getFile(whiteKing0x88)) > 1 : "Kings to close. w: " + whiteKing0x88 + ", b: " + blackKing0x88;

		initSquareInCheckByKing(whiteKing0x88, WHITE);
		initSquareInCheckByKing(blackKing0x88, BLACK);
	}

	private void initSquareInCheckByKing(byte king0x88, boolean isKingWhite) {
		short checkFlag = isKingWhite ? SquareFormat.CHECK_BY_WHITE_KING : SquareFormat.CHECK_BY_BLACK_KING;
		byte to = Direction.move(king0x88, Direction.E);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
		to = Direction.move(king0x88, Direction.NE);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
		to = Direction.move(king0x88, Direction.N);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
		to = Direction.move(king0x88, Direction.NW);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
		to = Direction.move(king0x88, Direction.W);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
		to = Direction.move(king0x88, Direction.SW);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
		to = Direction.move(king0x88, Direction.S);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
		to = Direction.move(king0x88, Direction.SE);
		if (inBounds(to)) {
			setFlag(to, checkFlag);
		}
	}

	private void initCheckFlagsBySlidingPiece(byte from, boolean isCheckedByWhite, byte direction) {
		byte squareUnderCheck = Direction.move(from, direction);
		while (inBounds(squareUnderCheck)) {
			incrementChecksOnSquare(squareUnderCheck, isCheckedByWhite);
			if (isSquareBlockingSlidingPiece(squareUnderCheck, isCheckedByWhite)) {
				break;
			}
			squareUnderCheck = Direction.move(squareUnderCheck, direction);
		}
	}

	private boolean isSquareBlockingSlidingPiece(byte square0x88, boolean isSlidingPieceWhite) {
		byte contentAsByte = (byte)(board0x88[square0x88]
				& (SquareFormat.PIECE_TYPE_MASK | SquareFormat.IS_WHITE_PIECE_FLAG));
		if (contentAsByte == Content.EMPTY.asByte) {
			return false;
		}
		byte enemyKing = isSlidingPieceWhite ? Content.BLACK_KING.asByte : Content.WHITE_KING.asByte;
		return contentAsByte != enemyKing;
	}

	private void initSquaresInCheckByPawn(byte from, boolean isCheckedByWhite) {
		byte queensideCheck = Direction.move(from, isCheckedByWhite ? Direction.NW : Direction.SW);
		if (inBounds(queensideCheck)) {
			incrementChecksOnSquare(queensideCheck, isCheckedByWhite);
		}
		byte kingsideCheck = Direction.move(from, isCheckedByWhite ? Direction.NE : Direction.SE);
		if (inBounds(kingsideCheck)) {
			incrementChecksOnSquare(kingsideCheck, isCheckedByWhite);
		}
	}

	private void incrementChecksOnSquare(byte square0x88, boolean isCheckedByWhite) {
		short contentAsShort = board0x88[square0x88];
		byte bitOffset = isCheckedByWhite ? SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET : SquareFormat.CHECKS_BY_BLACK_BIT_OFFSET;
		short checksCount = (short) ((contentAsShort >>> bitOffset) & SquareFormat.CHECKS_COUNT_MASK);
		checksCount++;
		checksCount <<= bitOffset;

		short resettingMask = (short) ~(SquareFormat.CHECKS_COUNT_MASK << bitOffset);
		contentAsShort = (short) (contentAsShort & resettingMask);

		board0x88[square0x88] = (short) (contentAsShort | checksCount);
	}

	private void setFlag(byte square0x88, short flag) {
		short contentAsShort = board0x88[square0x88];
		board0x88[square0x88] = (short) (contentAsShort | flag);
	}

	boolean isSquareCheckedBy(byte square0x88, boolean testChecksByWhite) {
		return getChecksCount(square0x88, testChecksByWhite) > 0;
	}

	private byte getChecksCount(byte square, boolean checksByWhite) {
		byte bitOffset = checksByWhite ? SquareFormat.CHECKS_BY_WHITE_BIT_OFFSET : SquareFormat.CHECKS_BY_BLACK_BIT_OFFSET;
		return (byte) ((board0x88[square] >> bitOffset) & SquareFormat.CHECKS_COUNT_MASK);
	}

	private boolean canKingWalkOnSquare(byte square0x88, boolean isKingWhite) {
		short checkedByKingFlag = isKingWhite ? SquareFormat.CHECK_BY_BLACK_KING : SquareFormat.CHECK_BY_WHITE_KING;
		return !isSquareCheckedBy(square0x88, !isKingWhite) && (board0x88[square0x88] & checkedByKingFlag) == 0;
	}

	public int countLegalMoves() {
		if (isKingInCheck()) {
			byte checkedKing = test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing();
			return generateLegalMovesWhenKingInCheck(null, checkedKing);
		}
		return generatePseudoLegalMoves(null);
	}

	public int countOtherSideLegalMoves() {
		makeNullMove();
		int legalMoves = countLegalMoves();
		makeNullMove();
		return legalMoves;
	}

	private void makeNullMove() {
		flags ^= WHITE_TURN;
	}

	public List<State> generateLegalMoves() {
		assert isLegal() : "King is still left in check after previous move!\n" + this;
		List<State> moves = new ArrayList<>(Config.DEFAULT_MOVES_LIST_CAPACITY);
		try {

			if (isKingInCheck()) {
				byte checkedKing = test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing();
				generateLegalMovesWhenKingInCheck(moves, checkedKing);
				return moves;
			}

			generatePseudoLegalMoves(moves);
		} catch (AssertionError ae) {
			System.out.println("------------------FAILED ASSERTION IN MOVE GENERATION----------------------------");
			System.out.println(" STATE: " + this);
			throw ae;
		}
		return moves;
	}

	boolean isKingInCheck() {
		return isSquareCheckedBy(test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing(), !test(WHITE_TURN));
	}

	/**
	 * 	 Generates pseudo-legal moves. Takes into consideration absolute pins. Moves generated by this method are legal
	 * 	 * 	 provided that the king of the side taking turn was not in check.
	 * @param ouputMoves - leave this null to skip generation and just count
	 * @return number of output moves
	 */
	private int generatePseudoLegalMoves(List<State> ouputMoves) {
		int movesCount = 0;
		byte[] piecesOfOneType = test(WHITE_TURN) ? pieces.whitePawns : pieces.blackPawns;
		byte piecesCount = test(WHITE_TURN) ? pieces.whitePawnsCount : pieces.blackPawnsCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalPawnMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteKnights : pieces.blackKnights;
		piecesCount = test(WHITE_TURN) ? pieces.whiteKnightsCount : pieces.blackKnightsCount;
		for (byte i = 0; i < piecesCount; i++) {
				movesCount += generatePseudoLegalKnightMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteBishops : pieces.blackBishops;
		piecesCount = test(WHITE_TURN) ? pieces.whiteBishopsCount : pieces.blackBishopsCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalBishopMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteRooks : pieces.blackRooks;
		piecesCount = test(WHITE_TURN) ? pieces.whiteRooksCount : pieces.blackRooksCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalRookMoves(piecesOfOneType[i], ouputMoves);
		}
		piecesOfOneType = test(WHITE_TURN) ? pieces.whiteQueens : pieces.blackQueens;
		piecesCount = test(WHITE_TURN) ? pieces.whiteQueensCount : pieces.blackQueensCount;
		for (byte i = 0; i < piecesCount; i++) {
			movesCount += generatePseudoLegalQueenMoves(piecesOfOneType[i], ouputMoves);
		}
		movesCount += generateLegalKingMoves(test(WHITE_TURN) ? pieces.getWhiteKing() : pieces.getBlackKing(),
				ouputMoves);
		return movesCount;
	}

	private int generateLegalKingMoves(byte from, List<State> outputMoves) {
		int movesCount = 0;
		boolean isWhiteTurn = test(WHITE_TURN);
		byte to = Direction.move(from, Direction.NE);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.E);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.SE);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.S);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.SW);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.W);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.NW);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.N);
		if (inBounds(to) && !isSameColorPieceOn(to) && canKingWalkOnSquare(to, isWhiteTurn)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}

		int qsCastlePossible = isWhiteTurn ? WHITE_QS_CASTLE_POSSIBLE : BLACK_QS_CASTLE_POSSIBLE;
		int ksCastlePossible = isWhiteTurn ? WHITE_KS_CASTLE_POSSIBLE : BLACK_KS_CASTLE_POSSIBLE;

		if (!isSquareCheckedBy(from, !isWhiteTurn)) {
			byte kingQsTo = isWhiteTurn ? Square0x88.C1 : Square0x88.C8;
			byte kingKsTo = isWhiteTurn ? Square0x88.G1 : Square0x88.G8;

			if (test(qsCastlePossible) && squaresOkForQsCastling(isWhiteTurn)) {
				if (outputMoves != null) {
					outputMoves.add(fromLegalQueensideCastling(from, kingQsTo));
				} else {
					movesCount++;
				}
			}
			if (test(ksCastlePossible) && squaresOkForKsCastling(isWhiteTurn)) {
				if (outputMoves != null) {
					outputMoves.add(fromLegalKingsideCastling(from, kingKsTo));
				} else {
					movesCount++;
				}
			}
		}
		return movesCount;
	}

	private boolean squaresOkForQsCastling(boolean isWhiteKingCastling) {
		for (int file = File.D; file >= File.C; file--) {
			byte square = Square0x88.from07(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(square) != Content.EMPTY || !canKingWalkOnSquare(square, isWhiteKingCastling)) {
				return false;
			}
		}
		return getContent(isWhiteKingCastling ? Square0x88.B1 : Square0x88.B8) == Content.EMPTY;
	}

	private boolean squaresOkForKsCastling(boolean isWhiteKingCastling) {
		for (int file = File.F; file <= File.G; file++) {
			byte square = Square0x88.from07(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(square) != Content.EMPTY || !canKingWalkOnSquare(square, isWhiteKingCastling)) {
				return false;
			}
		}
		return true;
	}

	private int generatePseudoLegalQueenMoves(byte from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generatePseudoLegalRookMoves(from, outputMoves);
		movesCount += generatePseudoLegalBishopMoves(from, outputMoves);
		return movesCount;
	}

	private int generatePseudoLegalRookMoves(byte from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.N);
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.E);
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.S);
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.W);
		return movesCount;
	}

	private int generatePseudoLegalBishopMoves(byte from, List<State> outputMoves) {
		int movesCount = 0;
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.NE);
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.SE);
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.SW);
		movesCount += generateSlidingPieceMoves(from, outputMoves, Direction.NW);
		return movesCount;
	}

	private int generateSlidingPieceMoves(byte from0x88, List<State> outputMoves, byte direction) {
		int movesCount = 0;
		if (pieceIsFreeToMove(Square0x88.to8x8Square(from0x88), Pin.fromDirection(direction))) {
			byte to = Direction.move(from0x88, direction);
			while (inBounds(to)) {
				if (isSameColorPieceOn(to)) {
					break;
				}
				movesCount = createOrCountMove(from0x88, to, outputMoves, movesCount);
				if (isOppositeColorPieceOn(to)) {
					break;
				}
				to = Direction.move(to, direction);
			}
		}
		return movesCount;
	}

	private int generatePseudoLegalPawnMoves(byte from0x88, List<State> outputMoves) {
		int movesCount = 0;
		byte pawnDisplacement = test(WHITE_TURN) ? Direction.N : Direction.S;
		int pawnDoubleDisplacement = test(WHITE_TURN) ? 2 * Direction.N : 2 * Direction.S;
		byte pawnQsTake = test(WHITE_TURN) ? Direction.NW : Direction.SW;
		byte pawnKsTake = test(WHITE_TURN) ? Direction.NE : Direction.SE;

		byte to0x88 = Direction.move(from0x88, pawnDisplacement);
		// head-on move
		if (getContent(to0x88) == Content.EMPTY && pieceIsFreeToMove(Square0x88.to8x8Square(from0x88), Pin.FILE)) {
			if (isPromotingSquare(to0x88)) {
				movesCount += generatePromotionMoves(from0x88, to0x88, outputMoves);
			} else {
				movesCount = createOrCountMove(from0x88, to0x88, outputMoves, movesCount);
				to0x88 = Direction.move(from0x88, (byte) pawnDoubleDisplacement);
				if (isInitialSquareOfPawn(from0x88) && getContent(to0x88) == Content.EMPTY) {
					if (outputMoves != null) {
						outputMoves.add(fromPseudoLegalPawnDoublePush(from0x88, to0x88, (byte)(from0x88 + pawnDisplacement)));
					} else {
						movesCount++;
					}
				}
			}
		}
		// move with take to the queen-side
		to0x88 = Direction.move(from0x88, pawnQsTake);

		if (Square0x88.inBounds(to0x88) && (isOppositeColorPieceOn(to0x88) || to0x88 == enPassantSquare)
				&& pieceIsFreeToMove(Square0x88.to8x8Square(from0x88), Pin.fromDirection(pawnQsTake))) {
			if (isPromotingSquare(to0x88)) {
				movesCount += generatePromotionMoves(from0x88, to0x88, outputMoves);
			} else {
				movesCount = createOrCountMove(from0x88, to0x88, outputMoves, movesCount);
			}
		}
		// move with take to the king side
		to0x88 = Direction.move(from0x88, pawnKsTake);
		if (Square0x88.inBounds(to0x88) && (isOppositeColorPieceOn(to0x88) || to0x88 == enPassantSquare)
				&& pieceIsFreeToMove(Square0x88.to8x8Square(from0x88), Pin.fromDirection(pawnKsTake))) {
			if (isPromotingSquare(to0x88)) {
				movesCount += generatePromotionMoves(from0x88, to0x88, outputMoves);
			} else {
				movesCount = createOrCountMove(from0x88, to0x88, outputMoves, movesCount);
			}
		}
		return movesCount;
	}

	private int generatePromotionMoves(byte from, byte to, List<State> outputMoves) {
		if (outputMoves == null) {
			return 4;
		}
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_QUEEN : Content.BLACK_QUEEN));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_ROOK : Content.BLACK_ROOK));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_BISHOP : Content.BLACK_BISHOP));
		outputMoves.add(fromPseudoLegalMoveWithPromotion(from, to, test(WHITE_TURN) ? Content.WHITE_KNIGHT : Content.BLACK_KNIGHT));
		return 4;
	}

	private int generatePseudoLegalKnightMoves(byte from, List<State> outputMoves) {
		if (!pieceIsFreeToMove(Square0x88.to8x8Square(from), null)) {
			return 0;
		}
		int movesCount = 0;
		byte to = Direction.move(from, Direction.NNE);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.NEE);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.SEE);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.SSE);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.SSW);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.SWW);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.NWW);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}
		to = Direction.move(from, Direction.NNW);
		if (inBounds(to) && !isSameColorPieceOn(to)) {
			movesCount = createOrCountMove(from, to, outputMoves, movesCount);
		}

		return movesCount;
	}

	private int createOrCountMove(byte from, byte to, List<State> outputMoves, int movesCount) {
		if (outputMoves != null) {
			outputMoves.add(fromPseudoLegalMove(from, to));
			return 0;
		}
		return movesCount + 1;
	}

	/**
	 * Returns true if piece at pieceLocation8x8 can move along the movementDirection line (is not absolutely pinned).
	 * @param movementDirection - leave this null in case of knight at pieceLocation8x8
	 */
	private boolean pieceIsFreeToMove(int pieceLocation8x8, Pin movementDirection) {
		Pin pin = pinnedPieces[pieceLocation8x8];
		return pin == null || pin == movementDirection;
	}

	private int generateLegalMovesWhenKingInCheck(List<State> outputMoves, byte checkedKing) {
		int movesCount = 0;
		if (getChecksCount(checkedKing, !test(WHITE_TURN)) < 2) {
			List<State> pseudoLegalMoves = new ArrayList<>(Config.DEFAULT_MOVES_LIST_CAPACITY);
			generatePseudoLegalMoves(pseudoLegalMoves);
			for (State pseudoLegalState : pseudoLegalMoves) {
				if (pseudoLegalState.isLegal()) {
					if (outputMoves != null) {
						outputMoves.add(pseudoLegalState);
					} else {
						movesCount++;
					}
				}
			}
		} else {
			movesCount = generateLegalKingMoves(checkedKing, outputMoves);
		}
		return movesCount;
	}

	/**
	 * Is king left in check after move
	 */
	private boolean isLegal() {
		boolean isWhiteTurn = test(WHITE_TURN);
		byte king = isWhiteTurn ? pieces.getBlackKing() : pieces.getWhiteKing();
		return !isSquareCheckedBy(king, isWhiteTurn);
	}
}
