package machess;

import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Game state seen as game board rather than a list of figures.
 * All is static in order to avoid allocations.
 */
public class State {
	private static final boolean WHITE = true;
	private static final boolean BLACK = false;
	
	private static final byte PIECE_TYPE_MASK = 0x07;
	private static final byte IS_WHITE_FLAG = 0x08;

	// Field has these flags set to true when a piece of given color can walk on this field on the next move
	private static final byte IN_CHECK_BY_WHITE = 0x10;
	private static final byte IN_CHECK_BY_BLACK = 0x20;
	// flag for fields adjacent to both kings at once
	private static final byte NO_KINGS_FLAG = 0x40;

	private static final int INITIAL_PLAYER_PIECES_COUNT = 16;

	private final boolean isWhiteTurn;
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
	 * new game
	 */
	State() {
		isWhiteTurn = WHITE;
		board = new byte[Field.FILES_COUNT * Field.RANKS_COUNT];
		for (int file = 0; file < Field.FILES_COUNT; file++) {
			board[Field.fromInts(file, 1).ordinal()] = Content.WHITE_PAWN.asByte;
			board[Field.fromInts(file, 6).ordinal()] = Content.BLACK_PAWN.asByte;

			switch (file) {
				case 0:
				case 7:
					board[Field.fromInts(file, 0).ordinal()] = Content.WHITE_ROOK.asByte;
					board[Field.fromInts(file, 7).ordinal()] = Content.BLACK_ROOK.asByte;
					break;
				case 1:
				case 6:
					board[Field.fromInts(file, 0).ordinal()] = Content.WHITE_KNIGHT.asByte;
					board[Field.fromInts(file, 7).ordinal()] = Content.BLACK_KNIGHT.asByte;
					break;
				case 2:
				case 5:
					board[Field.fromInts(file, 0).ordinal()] = Content.WHITE_BISHOP.asByte;
					board[Field.fromInts(file, 7).ordinal()] = Content.BLACK_BISHOP.asByte;
					break;
				case 3:
					board[Field.fromInts(file, 0).ordinal()] = Content.WHITE_QUEEN.asByte;
					board[Field.fromInts(file, 7).ordinal()] = Content.BLACK_QUEEN.asByte;
					break;
				case 4:
					board[Field.fromInts(file, 0).ordinal()] = Content.WHITE_KING.asByte;
					board[Field.fromInts(file, 7).ordinal()] = Content.BLACK_KING.asByte;
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
		initFieldsInCheck();
	}

	private State(byte[] board, Field[] fieldsWithWhites, byte whitesCount, Field[] fieldsWithBlacks, byte blacksCount,
	      boolean isWhiteTurn, @Nullable Field enPassantField, int plyNumber) {
		this.board = board;
		this.fieldsWithWhites = fieldsWithWhites;
		this.fieldsWithBlacks = fieldsWithBlacks;
		this.whitesCount = whitesCount;
		this.blacksCount = blacksCount;
		this.isWhiteTurn = isWhiteTurn;
		this.enPassantField = enPassantField;
		this.plyNumber = plyNumber;
		resetFieldsInCheck();
		initFieldsInCheck();
	}

	private State fromLegalPawnFirstMove(Field from, Field to, Field enPassantField) {
		assert enPassantField != null;
		return fromLegalMove(from, to, null, enPassantField);
	}

	private State fromLegalMoveWithPromotion(Field from, Field to, Content promotion) {
		assert promotion != null;
		return fromLegalMove(from, to, promotion, null);
	}

	/**
	 * Generates new BoardState based on move. Typical move without special events.
	 */
	State fromLegalMove(Field from, Field to) {
		return fromLegalMove(from, to, null, null);
	}

	/**
	 * Generates new BoardState based on move. It does not verify game rules - assumes input is a legal move.
	 * This is the root method - it covers all cases. All 'overload' methods should call this one.
	 */
	private State fromLegalMove(Field from, Field to, @Nullable Content promotion, @Nullable Field futureEnPassantField) {
		assert from != to : from + "->" + to + " is no move";
		byte[] boardCopy = board.clone();
		Field[] fieldsWithWhitesCopy = fieldsWithWhites.clone();
		Field[] fieldsWithBlacksCopy = fieldsWithBlacks.clone();

		//  update boardCopy
		Content movedPiece = Content.fromByte(boardCopy[from.ordinal()]);
		assert movedPiece != Content.EMPTY : from + "->" + to + " moves nothing";
		boardCopy[from.ordinal()] = Content.EMPTY.asByte;

		Content takenPiece = Content.fromByte(boardCopy[to.ordinal()]);
		assert takenPiece != Content.BLACK_KING && takenPiece != Content.WHITE_KING : from + "->" + to + " is taking king";
		boardCopy[to.ordinal()] = promotion != null ? promotion.asByte : movedPiece.asByte;

		Field fieldWithPawnTakenEnPassant = null;
		if (enPassantField == to) {
			if (movedPiece == Content.WHITE_PAWN) {
				fieldWithPawnTakenEnPassant = Field.fromInts(to.file, to.rank - 1);
				takenPiece = Content.fromByte(boardCopy[fieldWithPawnTakenEnPassant.ordinal()]);
				boardCopy[fieldWithPawnTakenEnPassant.ordinal()] = Content.EMPTY.asByte;
			} else if (movedPiece == Content.BLACK_PAWN) {
				fieldWithPawnTakenEnPassant = Field.fromInts(to.file, to.rank + 1);
				takenPiece = Content.fromByte(boardCopy[fieldWithPawnTakenEnPassant.ordinal()]);
				boardCopy[fieldWithPawnTakenEnPassant.ordinal()] = Content.EMPTY.asByte;
			}
		}

		// update pieces lists
		Field[] movingPieces = isWhiteTurn ? fieldsWithWhitesCopy : fieldsWithBlacksCopy;
		byte movingPiecesCount = isWhiteTurn ? whitesCount : blacksCount;
		Field[] takenPieces = isWhiteTurn ? fieldsWithBlacksCopy : fieldsWithWhitesCopy;
		byte takenPiecesCount = isWhiteTurn ? blacksCount : whitesCount;

		for (int i = 0; i < movingPiecesCount; i++) {
			if (movingPieces[i] == from) {
				movingPieces[i] = to;
				break;
			}
		}
		if (takenPiece != Content.EMPTY) {
			assert movedPiece.isWhite != takenPiece.isWhite : from + "->" + to + " is friendly take";
			for (int i = 0; i < takenPiecesCount; i++) {
				if (takenPieces[i] == to || takenPieces[i] == fieldWithPawnTakenEnPassant) {
					// decrement size of alive pieces
					takenPiecesCount--;
					takenPieces[i] = takenPieces[takenPiecesCount];
					break;
				}
			}
		}
		byte updatedWhitesCount = isWhiteTurn ? whitesCount : takenPiecesCount;
		byte updatedBlacksCount = isWhiteTurn ? takenPiecesCount : blacksCount;
		return new State(boardCopy, fieldsWithWhitesCopy, updatedWhitesCount, fieldsWithBlacksCopy, updatedBlacksCount,
				!isWhiteTurn, futureEnPassantField, plyNumber + 1);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Turn: ").append(isWhiteTurn ? "WHITE" : "BLACK").append('\n');
		sb.append(" | a  | b  | c  | d  | e  | f  | g  | h  |\n");
		sb.append(" =========================================\n");
		for (byte rank = Field.RANKS_COUNT - 1; rank >= 0; rank--) {
			sb.append(rank + 1).append("|");
			for (byte file = 0; file < Field.FILES_COUNT; file++) {
				Content content = getContent(file, rank);
				sb.append(content.symbol).append(" |");
			}
			sb.append("\n-+----+----+----+----+----+----+----+----+\n");
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
		sb.append(debugCheckFlags());
		return sb.toString();
	}

	private String debugCheckFlags() {
		if (!Config.DEBUG_FIELD_IN_CHECK_FLAGS) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(" | a  | b  | c  | d  | e  | f  | g  | h  |\n");
		sb.append(" =========================================\n");
		for (byte rank = Field.RANKS_COUNT - 1; rank >= 0; rank--) {
			sb.append(rank + 1).append("|");
			for (byte file = 0; file < Field.FILES_COUNT; file++) {
				byte contentAsByte = board[Field.fromInts(file, rank).ordinal()];

				sb.append(intToString(contentAsByte, 4))
						.append('|');
			}
			sb.append("\n-+----+----+----+----+----+----+----+----+\n");
		}
		return sb.toString();
	}

	public static String intToString(int number, int groupSize) {
		number >>>= 4;
		StringBuilder result = new StringBuilder();
		for(int i = 3; i >= 0 ; i--) {
			int mask = 1 << i;
			result.append((number & mask) != 0 ? "1" : ".");

			if (i % groupSize == 0)
				result.append(" ");
		}
		result.replace(result.length() - 1, result.length(), "");

		return result.toString();
	}

	public Content getContent(int file, int rank) {
		return getContent(Field.fromInts(file, rank));
	}

	public Content getContent(Field field) {
		return Content.fromByte(board[field.ordinal()]);
	}

	private boolean isPromotingField(Field field) {
		return isWhiteTurn ? field.rank == Field.WHITE_PROMOTION_RANK : field.rank == Field.BLACK_PROMOTION_RANK;
	}

	private boolean isInitialFieldOfPawn(Field field) {
		return isWhiteTurn ? field.rank == Field.WHITE_PAWN_INITIAL_RANK : field.rank == Field.BLACK_PAWN_INITIAL_RANK;
	}

	/**
	 * Tells if Field field is occupied by a piece of color that's currently taking turn.
	 */
	boolean isSameColorPieceOn(Field field) {
		return isWhiteTurn ? isWhitePieceOn(field) : isBlackPieceOn(field);
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
		return isWhiteTurn ? isBlackPieceOn(field) : isWhitePieceOn(field);
	}

	/**
	 * Tells if Field field is occupied by a piece of color isWhite.
	 */
	boolean isOppositeColorPieceOn(Field field, boolean isWhite) {
		return isWhite ? isBlackPieceOn(field) : isWhitePieceOn(field);
	}

	private boolean isWhitePieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_FLAG) != 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	private boolean isBlackPieceOn(Field field) {
		byte contentAsByte = board[field.ordinal()];
		return (contentAsByte & IS_WHITE_FLAG) == 0 && (contentAsByte & PIECE_TYPE_MASK) != 0;
	}

	private void resetFieldsInCheck() {
		for (int i = 0; i < board.length; i++) {
			byte contentAsByte = board[i];
			board[i] = (byte)(contentAsByte & (State.PIECE_TYPE_MASK | State.IS_WHITE_FLAG));
		}
	}

	private void initFieldsInCheck() {
		initFieldsInCheck(BLACK);
		initFieldsInCheck(WHITE);
		initFieldsInCheckByKings();
	}
	
	private void initFieldsInCheck(boolean isCheckedByWhite) {
		int countOfPiecesTakingTurn = isCheckedByWhite ? whitesCount : blacksCount;
		Field[] fieldsWithPiecesTakingTurn = isCheckedByWhite ? fieldsWithWhites  : fieldsWithBlacks;

		// for every piece except king - skip i == 0
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
		assert Math.abs(blackKing.rank - whiteKing.rank) > 1
				|| Math.abs(blackKing.file - whiteKing.file) > 1 : "Kings to close. w: " + whiteKing+ ", b: " + blackKing;

		Set<Field> whiteNeighbourhood = getKingNeighbourhood(whiteKing, WHITE);
		Set<Field> blackNeighbourhood = getKingNeighbourhood(blackKing, BLACK);

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

	private Set<Field> getKingNeighbourhood(Field king, boolean isWhiteKing) {
		Set<Field> neighbourhood = new HashSet<>();
		Field to = Field.fromUnsafeInts(king.file, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromUnsafeInts(king.file + 1, king.rank + 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromUnsafeInts(king.file + 1, king.rank);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromUnsafeInts(king.file + 1, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromUnsafeInts(king.file, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromUnsafeInts(king.file - 1, king.rank - 1);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromUnsafeInts(king.file - 1, king.rank);
		if (to != null) {
			neighbourhood.add(to);
		}
		to = Field.fromUnsafeInts(king.file - 1, king.rank + 1);
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
			Field to = Field.fromUnsafeInts(rookField.file + i, rookField.rank);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(rookField.file - i, rookField.rank);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(rookField.file, rookField.rank + i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(rookField.file, rookField.rank - i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
	}

	private void initFieldsInCheckByBishop(Field bishopField, boolean isCheckedByWhite) {
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(bishopField.file + i, bishopField.rank + i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(bishopField.file + i, bishopField.rank - i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(bishopField.file - i, bishopField.rank + i);
			if (setCheckFlagForSlidingPiece(to, isCheckedByWhite)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(bishopField.file - i, bishopField.rank - i);
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
		Field to = Field.fromUnsafeInts(from.file - 1, from.rank + pawnDisplacement);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		// check to the king side
		to = Field.fromUnsafeInts(from.file + 1, from.rank + pawnDisplacement);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
	}

	private void initFieldsInCheckByKnight(Field knightField, boolean isCheckedByWhite) {
		Field to = Field.fromUnsafeInts(knightField.file + 1, knightField.rank + 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromUnsafeInts(knightField.file + 1, knightField.rank - 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromUnsafeInts(knightField.file - 1, knightField.rank + 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromUnsafeInts(knightField.file - 1, knightField.rank - 2);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromUnsafeInts(knightField.file + 2, knightField.rank + 1);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromUnsafeInts(knightField.file + 2, knightField.rank - 1);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromUnsafeInts(knightField.file - 2, knightField.rank + 1);
		if (to != null) {
			setCheckFlagOnField(to, isCheckedByWhite);
		}
		to = Field.fromUnsafeInts(knightField.file - 2, knightField.rank - 1);
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

	public enum Content {
		EMPTY(0x00,"   ", BLACK),
		BLACK_PAWN(0x01,    "pp ", BLACK),
		BLACK_KNIGHT(0x02,  "NN_", BLACK),
		BLACK_BISHOP(0x03,  "BB_", BLACK),
		BLACK_ROOK(0x04,    "_RR", BLACK),
		BLACK_QUEEN(0x05,   "QQ_", BLACK),
		BLACK_KING(0x06,    "KK_", BLACK),
		WHITE_PAWN(0x09,    " p ", WHITE),
		WHITE_KNIGHT(0x0A,  " N_", WHITE),
		WHITE_BISHOP(0x0B,  " B_", WHITE),
		WHITE_ROOK(0x0C,    " _R", WHITE),
		WHITE_QUEEN(0x0D,   " Q_", WHITE),
		WHITE_KING(0x0E,    " K_", WHITE);

		private static final Content[] byteToContents = {
				EMPTY, BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING, EMPTY,
				EMPTY, WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING, EMPTY,
		};

		final byte asByte;
		/**
		 * printable symbol for toString()
		 */
		public final String symbol;

		final boolean isWhite;

		Content(int asByte, String symbol, boolean isWhite) {
			this.asByte = (byte) asByte;
			this.symbol = symbol;
			this.isWhite = isWhite;
		}

		static Content fromByte(byte contentAsByte) {
			return byteToContents[contentAsByte & (State.PIECE_TYPE_MASK | State.IS_WHITE_FLAG)];
		}
	}

	List<State> generateLegalMoves() {
		// TODO pinned pieces movement
		// TODO generation when king under check
		List<State> moves = new ArrayList<>();

		int countOfPiecesTakingTurn = isWhiteTurn ? whitesCount : blacksCount;
		Field[] fieldsWithPiecesTakingTurn = isWhiteTurn ? fieldsWithWhites  : fieldsWithBlacks;

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
		//TODO castlings
		Field to = Field.fromUnsafeInts(from.file, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file + 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file + 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file + 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file - 1, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file - 1, from.rank);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file - 1, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to) && isFieldOkForKing(to, isWhiteTurn)) {
			outputMoves.add(fromLegalMove(from, to));
		}
	}

	private void generateLegalQueenMoves(Field from, List<State> outputMoves) {
		generateLegalRookMoves(from, outputMoves);
		generateLegalBishopMoves(from, outputMoves);
	}

	private void generateLegalRookMoves(Field from, List<State> outputMoves) {
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file + i, from.rank);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file - i, from.rank);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file, from.rank + 1);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file, from.rank - 1);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
	}

	private void generateLegalBishopMoves(Field from, List<State> outputMoves) {
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file + i, from.rank + i);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file + i, from.rank - i);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file - i, from.rank + i);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
		for (int i = 1; true; i++) {
			Field to = Field.fromUnsafeInts(from.file - i, from.rank - i);
			if (generateSlidingPieceMove(from, to, outputMoves)) {
				break;
			}
		}
	}

	private boolean generateSlidingPieceMove(Field from, Field to, List<State> outputMoves) {
		if (to == null || isSameColorPieceOn(to)) {
			return true;
		}
		outputMoves.add(fromLegalMove(from, to));
		return isOppositeColorPieceOn(to);
	}

	private void generateLegalPawnMoves(Field from, List<State> outputMoves) {
		int pawnDisplacement = isWhiteTurn ? 1 : -1;
		int pawnDoubleDisplacement = isWhiteTurn ? 2 : -2;
		Field to = Field.fromInts(from.file, from.rank + pawnDisplacement);
		// head-on move
		if (getContent(to) == Content.EMPTY) {
			if (isPromotingField(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
				to = Field.fromInts(from.file, from.rank + pawnDoubleDisplacement);
				if (isInitialFieldOfPawn(from) && getContent(to) == Content.EMPTY) {
					outputMoves.add(fromLegalPawnFirstMove(from, to, Field.fromInts(from.file, from.rank + pawnDisplacement)));
				}
			}
		}
		// move with take to the queen-side
		to = Field.fromUnsafeInts(from.file - 1, from.rank + pawnDisplacement);
		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantField)) {
			if (isPromotingField(to)) {
				generatePromotionMoves(from, to, outputMoves);
			} else {
				outputMoves.add(fromLegalMove(from, to));
			}
		}
		// move with take to the king side
		to = Field.fromUnsafeInts(from.file + 1, from.rank + pawnDisplacement);
		if (to != null && (isOppositeColorPieceOn(to) || to == enPassantField)) {
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
		Field to = Field.fromUnsafeInts(from.file + 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file + 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file - 1, from.rank + 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file - 1, from.rank - 2);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file + 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file + 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file - 2, from.rank + 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
		to = Field.fromUnsafeInts(from.file - 2, from.rank - 1);
		if (to != null && !isSameColorPieceOn(to)) {
			outputMoves.add(fromLegalMove(from, to));
		}
	}
}
