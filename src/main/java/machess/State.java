package machess;

import com.sun.istack.internal.Nullable;

import java.util.*;

/**
 * Game state seen as game board rather than a list of figures.
 * All is static in order to avoid allocations.
 */
public class State {
	public static final boolean WHITE = true;
	public static final boolean BLACK = false;
	
	static final byte PIECE_TYPE_MASK = 0x07;
	static final byte IS_WHITE_PIECE_FLAG = 0x08;

	// black king cannot walk onto a field with this flag set
	private static final byte IN_CHECK_BY_WHITE = 0x10;
	// white king cannot walk onto a field with this flag set
	private static final byte IN_CHECK_BY_BLACK = 0x20;
	// flag for fields adjacent to both kings at once - neither king can walk onto these fields
	private static final byte NO_KINGS_FLAG = 0x40;

	private static final int INITIAL_PLAYER_PIECES_COUNT = 16;

	// flags
	private final byte flags;
	private static final int WHITE_TURN =           0x01;
	private static final int WHITE_KING_MOVED =     0x02;
	private static final int BLACK_KING_MOVED =     0x04;
	private static final int WHITE_KS_ROOK_MOVED =  0x08;
	private static final int WHITE_QS_ROOK_MOVED =  0x10;
	private static final int BLACK_KS_ROOK_MOVED =  0x20;
	private static final int BLACK_QS_ROOK_MOVED =  0x40;

	/**
	 * one byte per field.
	 */
	private final byte[] board;

	// king, queen, rooks and knights, pawns
	private final Field[] fieldsWithWhites;
	private final byte whitesCount;

	private final Field[] fieldsWithBlacks;
	private final byte blacksCount;
	/**
	 * If not null it means there is a possibility to en-passant on this field
	 */
	private final Field enPassantField;
	// not really necessary - only for debug purposes
	private final int plyNumber;

	/**
	 * Board with absolutely pinned pieces. It's indexed by Field.ordinal()
	 */
	private final Pin[] pinnedPieces;

	/**
	 * new game
	 */
	State() {
		flags = WHITE_TURN;
		board = new byte[Field.values().length];
		for (int file = File.A; file <= File.H; file++) {
			board[Field.fromLegalInts(file, Rank._2).ordinal()] = Content.WHITE_PAWN.asByte;
			board[Field.fromLegalInts(file, Rank._7).ordinal()] = Content.BLACK_PAWN.asByte;

			switch (file) {
				case 0:
				case 7:
					board[Field.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_ROOK.asByte;
					board[Field.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_ROOK.asByte;
					break;
				case 1:
				case 6:
					board[Field.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_KNIGHT.asByte;
					board[Field.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_KNIGHT.asByte;
					break;
				case 2:
				case 5:
					board[Field.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_BISHOP.asByte;
					board[Field.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_BISHOP.asByte;
					break;
				case 3:
					board[Field.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_QUEEN.asByte;
					board[Field.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_QUEEN.asByte;
					break;
				case 4:
					board[Field.fromLegalInts(file, Rank._1).ordinal()] = Content.WHITE_KING.asByte;
					board[Field.fromLegalInts(file, Rank._8).ordinal()] = Content.BLACK_KING.asByte;
					break;
			}
		}
		fieldsWithWhites = new Field[]{
				Field.E1, Field.D1, Field.A1, Field.H1, Field.B1, Field.C1, Field.F1, Field.G1,
				Field.A2, Field.B2, Field.C2, Field.D2, Field.E2, Field.F2, Field.G2, Field.H2
		};
		whitesCount = INITIAL_PLAYER_PIECES_COUNT;

		fieldsWithBlacks = new Field[]{
				Field.E8, Field.D8, Field.A8, Field.H8, Field.B8, Field.C8, Field.F8, Field.G8,
				Field.A7, Field.B7, Field.C7, Field.D7, Field.E7, Field.F7, Field.G7, Field.H7
		};
		blacksCount = INITIAL_PLAYER_PIECES_COUNT;
		enPassantField = null;
		plyNumber = 1;

		pinnedPieces = new Pin[Field.values().length];
		initFieldsInCheck();
	}

	private State(byte[] board, Field[] fieldsWithWhites, byte whitesCount, Field[] fieldsWithBlacks, byte blacksCount,
	              byte flags, @Nullable Field enPassantField, int plyNumber) {
		this.board = board;
		this.fieldsWithWhites = fieldsWithWhites;
		this.fieldsWithBlacks = fieldsWithBlacks;
		this.whitesCount = whitesCount;
		this.blacksCount = blacksCount;
		this.flags = flags;
		this.enPassantField = enPassantField;
		this.plyNumber = plyNumber;
		resetFieldsInCheck();
		initFieldsInCheck();

		pinnedPieces = new Pin[Field.values().length];
		initPinnedPieces();
	}

	private State fromLegalPawnFirstMove(Field from, Field to, Field enPassantField) {
		assert enPassantField != null;
		return fromLegalMove(from, to, null, enPassantField, null);
	}

	private State fromLegalMoveWithPromotion(Field from, Field to, Content promotion) {
		assert promotion != null;
		return fromLegalMove(from, to, promotion, null, null);
	}

	private State fromLegalQueensideCastling(Field kingFrom, Field kingTo) {
		Field rookToCastle = Field.fromLegalInts(File.A, kingFrom.rank);
		return fromLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}

	private State fromLegalKingsideCastling(Field kingFrom, Field kingTo) {
		Field rookToCastle = Field.fromLegalInts(File.H, kingFrom.rank);
		return fromLegalMove(kingFrom, kingTo, null, null, rookToCastle);
	}

	/**
	 * Generates new BoardState based on move. Typical move without special events.
	 */
	State fromLegalMove(Field from, Field to) {
		return fromLegalMove(from, to, null, null, null);
	}

	/**
	 * Generates new BoardState based on move. It does not verify game rules - assumes input is a legal move.
	 * This is the root method - it covers all cases. All 'overload' methods should call this one.
	 */
	private State fromLegalMove(Field from, Field to, @Nullable Content promotion, @Nullable Field futureEnPassantField,
	                            @Nullable Field rookToCastle) {
		assert from != to : from + "->" + to + " is no move";
		byte[] boardCopy = board.clone();
		Field[] fieldsWithWhitesCopy = fieldsWithWhites.clone();
		Field[] fieldsWithBlacksCopy = fieldsWithBlacks.clone();

		//  update boardCopy
		Content movedPiece = Content.fromByte(boardCopy[from.ordinal()]);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		assert movedPiece.isWhite == test(WHITE_TURN) : "Moved " + movedPiece + " on " + (test(WHITE_TURN) ? "white" : "black" ) + " turn";
		boardCopy[from.ordinal()] = Content.EMPTY.asByte;

		Content takenPiece = Content.fromByte(boardCopy[to.ordinal()]);
		assert takenPiece != Content.BLACK_KING && takenPiece != Content.WHITE_KING : from + "->" + to + " is taking king";
		boardCopy[to.ordinal()] = promotion != null ? promotion.asByte : movedPiece.asByte;

		Field fieldWithPawnTakenEnPassant = null;
		if (enPassantField == to) {
			if (movedPiece == Content.WHITE_PAWN) {
				fieldWithPawnTakenEnPassant = Field.fromLegalInts(to.file, to.rank - 1);
				takenPiece = Content.fromByte(boardCopy[fieldWithPawnTakenEnPassant.ordinal()]);
				boardCopy[fieldWithPawnTakenEnPassant.ordinal()] = Content.EMPTY.asByte;
			} else if (movedPiece == Content.BLACK_PAWN) {
				fieldWithPawnTakenEnPassant = Field.fromLegalInts(to.file, to.rank + 1);
				takenPiece = Content.fromByte(boardCopy[fieldWithPawnTakenEnPassant.ordinal()]);
				boardCopy[fieldWithPawnTakenEnPassant.ordinal()] = Content.EMPTY.asByte;
			}
		} else if (rookToCastle != null) {
			boardCopy[rookToCastle.ordinal()] = Content.EMPTY.asByte;
			byte rookAsByte = test(WHITE_TURN) ? Content.WHITE_ROOK.asByte : Content.BLACK_ROOK.asByte;
			Field rookDestination;
			if (rookToCastle.file == 0) {
				rookDestination = test(WHITE_TURN) ? Field.D1 : Field.D8;
				boardCopy[rookDestination.ordinal()] = rookAsByte;
			} else {
				rookDestination = test(WHITE_TURN) ? Field.F1 : Field.F8;
				boardCopy[rookDestination.ordinal()] = rookAsByte;
			}
			// update pieces lists
			movePieceOnPiecesLists(fieldsWithWhitesCopy, fieldsWithBlacksCopy, rookToCastle, rookDestination);
		}

		// update pieces lists
		movePieceOnPiecesLists(fieldsWithWhitesCopy, fieldsWithBlacksCopy, from, to);

		Field[] takenPieces = test(WHITE_TURN) ? fieldsWithBlacksCopy : fieldsWithWhitesCopy;
		byte takenPiecesCount = test(WHITE_TURN) ? blacksCount : whitesCount;

		if (takenPiece != Content.EMPTY) {
			assert movedPiece.isWhite != takenPiece.isWhite : from + "->" + to + " is a friendly take";
			for (int i = 0; i < takenPiecesCount; i++) {
				if (takenPieces[i] == to || takenPieces[i] == fieldWithPawnTakenEnPassant) {
					// decrement size of alive pieces
					takenPiecesCount--;
					takenPieces[i] = takenPieces[takenPiecesCount];
					break;
				}
			}
		}

		int flagsCopy = flags ^ WHITE_TURN;
		if (from == Field.E1) {
			flagsCopy |= WHITE_KING_MOVED;
		} else if (from == Field.E8) {
			flagsCopy |= BLACK_KING_MOVED;
		} else if (from == Field.A1) {
			flagsCopy |= WHITE_QS_ROOK_MOVED;
		} else if (from == Field.H1) {
			flagsCopy |= WHITE_KS_ROOK_MOVED;
		} else if (from == Field.A8) {
			flagsCopy |= BLACK_QS_ROOK_MOVED;
		} else if (from == Field.H8) {
			flagsCopy |= BLACK_KS_ROOK_MOVED;
		}

		byte updatedWhitesCount = test(WHITE_TURN) ? whitesCount : takenPiecesCount;
		byte updatedBlacksCount = test(WHITE_TURN) ? takenPiecesCount : blacksCount;
		return new State(boardCopy, fieldsWithWhitesCopy, updatedWhitesCount, fieldsWithBlacksCopy, updatedBlacksCount,
				(byte)flagsCopy, futureEnPassantField, plyNumber + 1);
	}

	private void movePieceOnPiecesLists(Field[] fieldsWithWhites, Field[] fieldsWithBlacks, Field from, Field to) {
		Field[] movingPieces = test(WHITE_TURN) ? fieldsWithWhites : fieldsWithBlacks;
		byte movingPiecesCount = test(WHITE_TURN) ? whitesCount : blacksCount;

		for (int i = 0; i < movingPiecesCount; i++) {
			if (movingPieces[i] == from) {
				movingPieces[i] = to;
				break;
			}
		}
	}

	private boolean test(int flagMask) {
		return (flags & flagMask) != 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: ").append(test(WHITE_TURN) ? "WHITE" : "BLACK");
		sb.append(test(WHITE_KING_MOVED) ? "; WHITE_KING_MOVED" : "");
		sb.append(test(WHITE_QS_ROOK_MOVED) ? "; WHITE_QS_ROOK_MOVED" : "");
		sb.append(test(WHITE_KS_ROOK_MOVED) ? "; WHITE_KS_ROOK_MOVED" : "");
		sb.append(test(BLACK_KING_MOVED) ? "; BLACK_KING_MOVED" : "");
		sb.append(test(BLACK_QS_ROOK_MOVED) ? "; BLACK_QS_ROOK_MOVED" : "");
		sb.append(test(BLACK_KS_ROOK_MOVED) ? "; BLACK_KS_ROOK_MOVED" : "");
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
				Content content = getContent(file, rank);
				sb.append(content.symbol).append(" |");

				if (Config.DEBUG_FIELD_IN_CHECK_FLAGS) {
					byte contentAsByte = board[Field.fromLegalInts(file, rank).ordinal()];
					sbCheckFlags.append(Utils.checkFlagsToString(contentAsByte)).append('|');
				}
				if (Config.DEBUG_PINNED_PIECES) {
					Pin pinType = pinnedPieces[Field.fromLegalInts(file, rank).ordinal()];
					sbPins.append(" ").append(pinType != null ? pinType.symbol : ' ').append("  |");
				}
			}
			sb.append(sbCheckFlags).append(sbPins)
					.append("\n-+----+----+----+----+----+----+----+----+")
					.append(Config.DEBUG_FIELD_IN_CHECK_FLAGS 	? "+----+----+----+----+----+----+----+----+" : "")
					.append(Config.DEBUG_PINNED_PIECES 			? "+----+----+----+----+----+----+----+----+" : "")
					.append('\n');
		}
		sb.append("fieldsWithWhites: [");
		for (int i = 0; i < fieldsWithWhites.length; i++) {
			sb.append(fieldsWithWhites[i]).append(i == (whitesCount - 1) ? ";   " : ", ");
		}
		sb.append("] count: ").append(whitesCount).append('\n');
		sb.append("fieldsWithBlacks: [");
		for (int i = 0; i < fieldsWithBlacks.length; i++) {
			sb.append(fieldsWithBlacks[i]).append(i == (blacksCount - 1) ? ";   " : ", ");
		}
		sb.append("] count: ").append(blacksCount).append('\n');
		sb.append("enPassantField: ").append(enPassantField).append('\n');
		sb.append("plyNumber: ").append(plyNumber).append('\n');
		return sb.toString();
	}

	public Content getContent(int file, int rank) {
		return getContent(Field.fromLegalInts(file, rank));
	}

	public Content getContent(Field field) {
		return Content.fromByte(board[field.ordinal()]);
	}

	private boolean isPromotingField(Field field) {
		return test(WHITE_TURN) ? field.rank == Rank.WHITE_PROMOTION_RANK : field.rank == Rank.BLACK_PROMOTION_RANK;
	}

	private boolean isInitialFieldOfPawn(Field field) {
		return test(WHITE_TURN) ? field.rank == Rank.WHITE_PAWN_INITIAL_RANK : field.rank == Rank.BLACK_PAWN_INITIAL_RANK;
	}

	/**
	 * Tells if Field field is occupied by a piece of color that's currently taking turn.
	 */
	boolean isSameColorPieceOn(Field field) {
		return test(WHITE_TURN) ? isWhitePieceOn(field) : isBlackPieceOn(field);
	}

	/**
	 * Tells if Field field is occupied by a piece of color isWhite
	 */
	boolean isSameColorPieceOn(Field field, boolean isWhite) {
		return isWhite ? isWhitePieceOn(field) : isBlackPieceOn(field);
	}

	/**
	 * Tells if Field field is occupied by a piece of color that's currently taking turn.
	 */
	boolean isOppositeColorPieceOn(Field field) {
		return test(WHITE_TURN) ? isBlackPieceOn(field) : isWhitePieceOn(field);
	}

	/**
	 * Tells if Field field is occupied by a piece of color isWhite.
	 */
	boolean isOppositeColorPieceOn(Field field, boolean isWhite) {
		return isWhite ? isBlackPieceOn(field) : isWhitePieceOn(field);
	}

	private boolean isWhitePieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_PIECE_FLAG) != 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	private boolean isBlackPieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_PIECE_FLAG) == 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	private void resetFieldsInCheck() {
		for (int i = 0; i < board.length; i++) {
			byte contentAsByte = board[i];
			board[i] = (byte)(contentAsByte & (State.PIECE_TYPE_MASK | State.IS_WHITE_PIECE_FLAG));
		}
	}

	private void initFieldsInCheck() {
		initFieldsInCheck(BLACK);
		initFieldsInCheck(WHITE);
		initFieldsInCheckByKings();
	}

	private void initPinnedPieces() {
		Field whiteKing = fieldsWithWhites[0];
		Field blackKing = fieldsWithBlacks[0];
		assert getContent(whiteKing) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing) == Content.BLACK_KING : "Corrupted black king position";

		for (Field f : Field.values()) {
			pinnedPieces[f.ordinal()] = null;
		}

		initPinnedPieces(whiteKing, WHITE);
		initPinnedPieces(blackKing, BLACK);
	}

	private void initPinnedPieces(Field king, boolean isPinnedToWhiteKing) {
		initPinInOneDirection(king, isPinnedToWhiteKing, -1, -1);
		initPinInOneDirection(king, isPinnedToWhiteKing, -1, 0);
		initPinInOneDirection(king, isPinnedToWhiteKing, -1, 1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 0, -1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 0, 1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 1, -1);
		initPinInOneDirection(king, isPinnedToWhiteKing, 1, 0);
		initPinInOneDirection(king, isPinnedToWhiteKing, 1, 1);
	}

	private void initPinInOneDirection(Field king, boolean isPinnedToWhiteKing, int deltaFile, int deltaRank) {
		Field candidate = null;
		for (int i = 1; true ; i++) {
			Field field = Field.fromInts(king.file + i * deltaFile, king.rank + i * deltaRank);

			if (field == null || (isOppositeColorPieceOn(field, isPinnedToWhiteKing) && candidate == null)) {
				return;
			}
			if (isSameColorPieceOn(field, isPinnedToWhiteKing)) {
				if (candidate == null) {
					candidate = field;
					continue;
				} else {
					return;
				}
			}
			if (candidate != null) {
				Content afterCandidate = getContent(field);
				Content enemyQueen = isPinnedToWhiteKing ? Content.BLACK_QUEEN : Content.WHITE_QUEEN;
				Content enemySlider = Content.rookOrBishop(!isPinnedToWhiteKing, deltaFile, deltaRank);
				if (afterCandidate == enemyQueen || afterCandidate == enemySlider) {
					pinnedPieces[candidate.ordinal()] = Pin.fromDeltas(deltaFile, deltaRank);
					return;
				} else if (afterCandidate != Content.EMPTY) {
					return;
				}
			}
		}
	}

	private void initFieldsInCheck(boolean isCheckedByWhite) {
		int countOfPiecesTakingTurn = isCheckedByWhite ? whitesCount : blacksCount;
		Field[] fieldsWithPiecesTakingTurn = isCheckedByWhite ? fieldsWithWhites  : fieldsWithBlacks;

		// for every piece except king
		for (int i = countOfPiecesTakingTurn - 1; i > 0; i--) {
			Field currField = fieldsWithPiecesTakingTurn[i];
			Content piece = Content.fromByte(board[currField.ordinal()]);
			switch (piece) {
				case WHITE_PAWN:
				case BLACK_PAWN:
					initFieldsInCheckByPawn(currField, isCheckedByWhite);
					break;
				case WHITE_KNIGHT:
				case BLACK_KNIGHT:
					initFieldsInCheckByKnight(currField, isCheckedByWhite);
					break;
				case WHITE_BISHOP:
				case BLACK_BISHOP:
					initFieldsInCheckByBishop(currField, isCheckedByWhite);
					break;
				case WHITE_ROOK:
				case BLACK_ROOK:
					initFieldsInCheckByRook(currField, isCheckedByWhite);
					break;
				case WHITE_QUEEN:
				case BLACK_QUEEN:
					initFieldsInCheckByQueen(currField, isCheckedByWhite);
					break;
				default:
					assert false : "Thing on:" + fieldsWithPiecesTakingTurn[i] + " is unknown piece: " + piece;
			}
		}
	}

	private void initFieldsInCheckByKings() {
		Field whiteKing = fieldsWithWhites[0];
		Field blackKing = fieldsWithBlacks[0];
		assert getContent(whiteKing) == Content.WHITE_KING : "Corrupted white king position";
		assert getContent(blackKing) == Content.BLACK_KING : "Corrupted black king position";
		assert Math.abs(blackKing.rank - whiteKing.rank) > 1
				|| Math.abs(blackKing.file - whiteKing.file) > 1 : "Kings to close. w: " + whiteKing+ ", b: " + blackKing;

		Set<Field> whiteNeighbourhood = getKingNeighbourhood(whiteKing);
		Set<Field> blackNeighbourhood = getKingNeighbourhood(blackKing);

		Set<Field> noKingsZone = new HashSet<>(whiteNeighbourhood);
		if (noKingsZone.retainAll(blackNeighbourhood)) {
			for (Field field : noKingsZone) {
				setNoKingFlagOnField(field);
			}
			whiteNeighbourhood.removeAll(noKingsZone);
			blackNeighbourhood.removeAll(noKingsZone);
		}
		for (Field field : whiteNeighbourhood) {
			setCheckFlagOnFieldByKing(field, WHITE);
		}
		for (Field field : blackNeighbourhood) {
			setCheckFlagOnFieldByKing(field, BLACK);
		}
	}

	private void setCheckFlagOnFieldByKing(Field field, boolean isKingWhite) {
		if (!isFieldCheckedBy(field, !isKingWhite)) {
			setCheckFlagOnField(field, isKingWhite);
		}
	}

	private Set<Field> getKingNeighbourhood(Field king) {
		Set<Field> neighbourhood = new HashSet<>();
		Field to = Field.fromInts(king.file, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromInts(king.file + 1, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromInts(king.file + 1, king.rank);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromInts(king.file + 1, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromInts(king.file, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromInts(king.file - 1, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromInts(king.file - 1, king.rank);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromInts(king.file - 1, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		return neighbourhood;
	}

	private void initFieldsInCheckByQueen(Field queenField, boolean isCheckedByWhite) {
		initFieldsInCheckByBishop(queenField, isCheckedByWhite);
		initFieldsInCheckByRook(queenField, isCheckedByWhite);
	}

	private void initFieldsInCheckByRook(Field rookField, boolean isCheckedByWhite) {
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(rookField.file + i, rookField.rank);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(rookField.file - i, rookField.rank);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(rookField.file, rookField.rank + i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(rookField.file, rookField.rank - i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
	}

	private void initFieldsInCheckByBishop(Field bishopField, boolean isCheckedByWhite) {
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(bishopField.file + i, bishopField.rank + i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(bishopField.file + i, bishopField.rank - i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(bishopField.file - i, bishopField.rank + i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromInts(bishopField.file - i, bishopField.rank - i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
	}

	private boolean setCheckFlagForSlidingPiece(Field underCheck, boolean isCheckedByWhite) {
		if (underCheck == null) {
			return true;
		} else if (isSameColorPieceOn(underCheck, isCheckedByWhite)) {
			setCheckFlagOnField(underCheck, isCheckedByWhite);
			return true;
		}
		setCheckFlagOnField(underCheck, isCheckedByWhite);
		return isOppositeColorPieceOn(underCheck, isCheckedByWhite);
	}

	private void initFieldsInCheckByPawn(Field from, boolean isCheckedByWhite) {
		int pawnDisplacement = isCheckedByWhite ? 1 : -1;
		// check to the queen side
		Field to = Field.fromInts(from.file - 1, from.rank + pawnDisplacement);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		// check to the king side
		to = Field.fromInts(from.file + 1, from.rank + pawnDisplacement);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
	}

	private void initFieldsInCheckByKnight(Field knightField, boolean isCheckedByWhite) {
		Field to = Field.fromInts(knightField.file + 1, knightField.rank + 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromInts(knightField.file + 1, knightField.rank - 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromInts(knightField.file - 1, knightField.rank + 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromInts(knightField.file - 1, knightField.rank - 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromInts(knightField.file + 2, knightField.rank + 1);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromInts(knightField.file + 2, knightField.rank - 1);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromInts(knightField.file - 2, knightField.rank + 1);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromInts(knightField.file - 2, knightField.rank - 1);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
	}
	
	private void setCheckFlagOnField(Field field, boolean isCheckedByWhite) {
		int checkFlag = isCheckedByWhite ? IN_CHECK_BY_WHITE : IN_CHECK_BY_BLACK;
		byte contentAsByte = board[field.ordinal()];
		board[field.ordinal()] = (byte)(contentAsByte | checkFlag);
	}

	private void setNoKingFlagOnField(Field field) {
		byte contentAsByte = board[field.ordinal()];
		board[field.ordinal()] = (byte)(contentAsByte | NO_KINGS_FLAG);
	}

	private boolean isFieldCheckedBy(Field field, boolean testChecksByWhite) {
		int checkFlag = testChecksByWhite ? IN_CHECK_BY_WHITE : IN_CHECK_BY_BLACK;
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & checkFlag) != 0;
	}

	private boolean isFieldOkForKing(Field field, boolean isKingWhite) {
		return !isFieldCheckedBy(field, !isKingWhite) && (board[field.ordinal()] & NO_KINGS_FLAG) == 0;
	}

	public List<State> generateLegalMoves() {
		// TODO generate legal moves: pinned pieces movement, generation when king under check
		List<State> moves = new ArrayList<>();

		int countOfPiecesTakingTurn = test(WHITE_TURN) ? whitesCount : blacksCount;
		Field[] fieldsWithPiecesTakingTurn = test(WHITE_TURN) ? fieldsWithWhites  : fieldsWithBlacks;

		for (int i = 0; i < countOfPiecesTakingTurn; i++) {
			Field currField = fieldsWithPiecesTakingTurn[i];
			Content piece = Content.fromByte(board[currField.ordinal()]);
			switch (piece) {
				case WHITE_PAWN:
				case BLACK_PAWN:
					generateLegalPawnMoves(currField, moves);
					break;
				case WHITE_KNIGHT:
				case BLACK_KNIGHT:
					generateLegalKnightMoves(currField, moves);
					break;
				case WHITE_BISHOP:
				case BLACK_BISHOP:
					generateLegalBishopMoves(currField, moves);
					break;
				case WHITE_ROOK:
				case BLACK_ROOK:
					generateLegalRookMoves(currField, moves);
					break;
				case WHITE_QUEEN:
				case BLACK_QUEEN:
					generateLegalQueenMoves(currField, moves);
					break;
				case WHITE_KING:
				case BLACK_KING:
					generateLegalKingMoves(currField, moves);
					break;
				default:
					assert false : "Thing on:" + fieldsWithPiecesTakingTurn[i] + " is unknown piece: " + piece;
			}
		}
		return moves;
	}

	private void generateLegalKingMoves(Field from, List<State> outputMoves) {
		Field to = Field.fromInts(from.file, from.rank + 1);
		boolean isWhiteTurn = test(WHITE_TURN);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file + 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file + 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file + 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file - 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file - 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file - 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		int kingMovedBitflag = isWhiteTurn ? WHITE_KING_MOVED : BLACK_KING_MOVED;
		if (!test(kingMovedBitflag) && !isFieldCheckedBy(from, !isWhiteTurn)) {

			Content rook = isWhiteTurn ? Content.WHITE_ROOK : Content.BLACK_ROOK;
			int qsRookMovedBitflag = isWhiteTurn ? WHITE_QS_ROOK_MOVED: BLACK_QS_ROOK_MOVED;
			int ksRookMovedBitflag = isWhiteTurn ? WHITE_KS_ROOK_MOVED: BLACK_KS_ROOK_MOVED;
			Field kingQsTo = isWhiteTurn ? Field.C1 : Field.C8;
			Field kingKsTo = isWhiteTurn ? Field.G1 : Field.G8;
			Field qsRook = isWhiteTurn ? Field.A1 : Field.A8;
			Field ksRook = isWhiteTurn ? Field.H1 : Field.H8;

			if (fieldsOkForQsCastle(isWhiteTurn) && getContent(qsRook) == rook && !test(qsRookMovedBitflag)) {
				outputMoves.add(fromLegalQueensideCastling(from, kingQsTo));
			}
			if (fieldsAreOkForKsCastling(isWhiteTurn) && getContent(ksRook) == rook && !test(ksRookMovedBitflag)) {
				outputMoves.add(fromLegalKingsideCastling(from, kingKsTo));
			}
		}
	}

	private boolean fieldsOkForQsCastle(boolean isWhiteKingCastling) {
		for (int file = File.D; file >= File.C; file--) {
			Field field = Field.fromLegalInts(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(field) != Content.EMPTY || isFieldCheckedBy(field, !isWhiteKingCastling)) {
				return false;
			}
		}
		return getContent(isWhiteKingCastling ? Field.B1 : Field.B8) == Content.EMPTY;
	}

	private boolean fieldsAreOkForKsCastling(boolean isWhiteKingCastling) {
		for (int file = File.F; file <= File.G; file++) {
			Field field = Field.fromLegalInts(file, isWhiteKingCastling ? Rank._1 : Rank._8);
			if (getContent(field) != Content.EMPTY || isFieldCheckedBy(field, !isWhiteKingCastling)) {
				return false;
			}
		}
		return true;
	}

	private void generateLegalQueenMoves(Field from, List<State> outputMoves) {
		generateLegalRookMoves(from, outputMoves);
		generateLegalBishopMoves(from, outputMoves);
	}

	private void generateLegalRookMoves(Field from, List<State> outputMoves) {
		int deltaFile;
		int deltaRank;
		deltaFile = 1;
		deltaRank = 0;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
		deltaFile = -1;
		deltaRank = 0;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
		deltaFile = 0;
		deltaRank = 1;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
		deltaFile = 0;
		deltaRank = -1;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
	}

	private void generateSlidingPieceMove(Field from, int deltaFile, int deltaRank, List<State> outputMoves) {
		if (pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, deltaRank))) {
			for (int i = 1; true; i++) {
				Field to = Field.fromInts(from.file + deltaFile * i, from.rank + i * deltaRank);
				if (generateSlidingPieceMove(from, to, outputMoves)) {
					break;
				}
			}
		}
	}

	private void generateLegalBishopMoves(Field from, List<State> outputMoves) {
		int deltaFile;
		int deltaRank;
		deltaFile = 1;
		deltaRank = 1;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
		deltaFile = 1;
		deltaRank = -1;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
		deltaFile = -1;
		deltaRank = 1;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
		deltaFile = -1;
		deltaRank = -1;
		generateSlidingPieceMove(from, deltaFile, deltaRank, outputMoves);
	}

	private boolean generateSlidingPieceMove(Field from, Field to, List<State> outputMoves) {
		if (to == null || isSameColorPieceOn(to)) {
			return true;
		}
		outputMoves.add(fromLegalMove(from, to));
		return isOppositeColorPieceOn(to);
	}

	private void generateLegalPawnMoves(Field from, List<State> outputMoves) {
		int pawnDisplacement = test(WHITE_TURN) ? 1 : -1;
		int pawnDoubleDisplacement = test(WHITE_TURN) ? 2 : -2;
		Field to = Field.fromLegalInts(from.file, from.rank + pawnDisplacement);
		// head-on move
		if (getContent(to) == Content.EMPTY && pieceIsFreeToMove(from, Pin.FILE)) {
			if (isPromotingField(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
				to = Field.fromLegalInts(from.file, from.rank + pawnDoubleDisplacement);
				if (isInitialFieldOfPawn(from) && getContent(to) == Content.EMPTY) {
					outputMoves.add(fromLegalPawnFirstMove(from, to, Field.fromLegalInts(from.file, from.rank + pawnDisplacement)));
				}
			}
		}
		// move with take to the queen-side
		int deltaFile = -1;
		to = Field.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);

		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantField)
				&& pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isPromotingField(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
			}
		}
		// move with take to the king side
		deltaFile = 1;
		to = Field.fromInts(from.file + deltaFile, from.rank + pawnDisplacement);
		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantField)
				&& pieceIsFreeToMove(from, Pin.fromDeltas(deltaFile, pawnDisplacement))) {
			if (isPromotingField(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
			}
		}
	}

	private void generatePromotionMoves(Field from, Field to, List<State> outputMoves) {
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_QUEEN));
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_ROOK));
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_BISHOP));
		outputMoves.add(fromLegalMoveWithPromotion(from, to, Content.WHITE_KNIGHT));
	}

	private void generateLegalKnightMoves(Field from, List<State> outputMoves) {
		Field to = Field.fromInts(from.file + 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file + 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file - 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file - 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file + 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file + 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file - 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromInts(from.file - 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
	}
	
	private boolean pieceIsFreeToMove(Field pieceLocation, Pin movementDirection) {
		Pin pin = pinnedPieces[pieceLocation.ordinal()];
		return pin == null || pin == movementDirection;
	}
}
