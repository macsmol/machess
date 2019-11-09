package machess;


import java.awt.Point;

import java.util.ArrayList;

public final class BoardLogic {
    public static final byte PION=1;
    public static final byte ROOK =2;
    public static final byte BISHOP =3;
    public static final byte KNIGHT =4;
    public static final byte QUEEN =5;
    public static final byte KING =6;
    public static final byte WHITE=8;//bitflag for white figures

    public static final int RES_MOVE_OK= 0;
    public static final int RES_MOVE_ILLEGAL=1;
    public static final int RES_WHITE_WINS= 2;
    public static final int RES_BLACK_WINS= 3;
    public static final int RES_DRAW= 4;
    private int lastResult;

    //HAS_MOVED-flaga bitowa używana dla króla,piona i wieży
    private static final byte HAS_MOVED=16;//bit

    //coordinates of of black and white peons
    //that can be at this moment taken en-passant
    private int enPassantWhiteX=-1;
    private int enPassantBlackX=-1;
    private static final int EN_PASSANT_WHITE_Y=4;
    private static final int EN_PASSANT_BLACK_Y=3;

    private int whiteKingX=4;
    private int whiteKingY=7;
    private int blackKingX=4;
    private int blackKingY=0;

    private boolean isWhiteTurn=true;

    private ArrayList<Point> rescueFields;
    //list of fields with the property that if a figure of certain colour is
    //placed on them the king of that colour is saved from check

    private byte[][] board;

    public BoardLogic(){
        board=new byte[8][8];

        for(int i=0;i<8;i++)
            for(int j=0;j<8;j++)
                board[i][j]=0;

        for(int i=0;i<8;i++){
            board[1][i]=PION;
            board[6][i]=PION | WHITE;
        }
        board[0][0]= ROOK;
        board[0][1]= KNIGHT;
        board[0][2]= BISHOP;
        board[0][3]= QUEEN;
        board[0][4]= KING;
        board[0][5]= BISHOP;
        board[0][6]= KNIGHT;
        board[0][7]= ROOK;
        board[7][0]= ROOK | WHITE;
        board[7][1]= KNIGHT | WHITE;
        board[7][2]= BISHOP | WHITE;
        board[7][3]= QUEEN | WHITE;
        board[7][4]= KING | WHITE;
        board[7][5]= BISHOP | WHITE;
        board[7][6]= KNIGHT | WHITE;
        board[7][7]= ROOK | WHITE;

        rescueFields=new ArrayList<Point>();
    }

    public byte getFieldContent(int x,int y){
        if(y<0 || y>7 || x<0 || x>7)
            return 0;
        return (byte)(board[y][x] & 15);//nie zwracamy info o tym czy pion był ruszony
    }

    private boolean wouldKingBeInCheck(int  xFrom,int yFrom,int xTo,int yTo){
        int tmpKingX;
        int tmpKingY;
        byte tmpFigure=board[yTo][xTo];
        board[yTo][xTo]=board[yFrom][xFrom];
        board[yFrom][xFrom]=0;
        if(isWhiteTurn){
            if(getFieldContent(xTo,yTo)==(KING | WHITE)){
                tmpKingX=xTo;
                tmpKingY=yTo;
            }
            else{
                tmpKingX=whiteKingX;
                tmpKingY=whiteKingY;
            }
            if(isFieldInCheck(tmpKingX,tmpKingY,true)){
                board[yFrom][xFrom]=board[yTo][xTo];
                board[yTo][xTo]=tmpFigure;
                return true;
            }
        }
        else{//black's turn
            if(getFieldContent(xTo,yTo)== KING){
                tmpKingX=xTo;
                tmpKingY=yTo;
            }
            else{
                tmpKingX=blackKingX;
                tmpKingY=blackKingY;
            }
            if(isFieldInCheck(tmpKingX,tmpKingY,false)){
                board[yFrom][xFrom]=board[yTo][xTo];
                board[yTo][xTo]=tmpFigure;
                return true;
            }
        }
        board[yFrom][xFrom]=board[yTo][xTo];
        board[yTo][xTo]=tmpFigure;
        return false;
    }
    private boolean isFieldInCheckSlantingLine(int x,int y,boolean isKingWhite){
        byte goniec= BISHOP;
        byte hetman= QUEEN;

        if(!isKingWhite){
            goniec |=WHITE;
            hetman|=WHITE;
        }

        rescueFields.clear();
        for(int i=1;x+i<8 && y+i<8;i++){
            rescueFields.add(new Point(x+i,y+i));
            if(getFieldContent(x+i,y+i)==goniec
                    || getFieldContent(x+i,y+i)==hetman)
                return true;
            if(board[y+i][x+i]!=0)
                break;
        }

        rescueFields.clear();
        for(int i=1;x+i<8 && y-i>=0;i++){
            rescueFields.add(new Point(x+i,y-i));
            if(getFieldContent(x+i,y-i)==goniec
                    || getFieldContent(x+i,y-i)==hetman)
                return true;
            if(board[y-i][x+i]!=0)
                break;
        }

        rescueFields.clear();
        for(int i=1;x-i>=0 && y-i>=0;i++){
            rescueFields.add(new Point(x-i,y-i));
            if(getFieldContent(x-i,y-i)==goniec
                    || getFieldContent(x-i,y-i)==hetman)
                return true;
            if(board[y-i][x-i]!=0)
                break;
        }

        rescueFields.clear();
        for(int i=1;x-i>=0 && y+i<8;i++){
            rescueFields.add(new Point(x-i,y+i));
            if(getFieldContent(x-i,y+i)==goniec
                    || getFieldContent(x-i,y+i)==hetman)
                return true;
            if(board[y+i][x-i]!=0)
                break;
        }

        rescueFields.clear();
        return false;
    }
    private boolean isFieldInCheckStraightLine(int x,int y,boolean isKingWhite){
        byte wieza= ROOK;
        byte hetman= QUEEN;

        if(!isKingWhite){
            wieza |=WHITE;
            hetman|=WHITE;
        }

        rescueFields.clear();
        for(int i=x+1;i<8;i++){
            rescueFields.add(new Point(i,y));
            if(getFieldContent(i,y)==wieza || getFieldContent(i,y)==hetman)
                return true;
            if(board[y][i]!=0)
                break;
        }

        rescueFields.clear();
        for(int i=x-1;i>=0;i--){
            rescueFields.add(new Point(i,y));
            if(getFieldContent(i,y)==wieza || getFieldContent(i,y)==hetman)
                return true;
            if(board[y][i]!=0)
                break;
        }

        rescueFields.clear();
        for(int i=y+1;i<8;i++){
            rescueFields.add(new Point(x,i));
            if(getFieldContent(x,i)==wieza || getFieldContent(x,i)==hetman)
                return true;
            if(board[i][x]!=0)
                break;
        }

        rescueFields.clear();
        for(int i=y-1;i>=0;i--){
            rescueFields.add(new Point(x,i));
            if(getFieldContent(x,i)==wieza || getFieldContent(x,i)==hetman)
                return true;
            if(board[i][x]!=0)
                break;
        }

        rescueFields.clear();
        return false;
    }
    private boolean isFieldInCheckBySkoczek(int x,int y, boolean isKingWhite){
        byte skoczek= KNIGHT;

        if(!isKingWhite)
            skoczek |=WHITE;

        rescueFields.clear();
        if(getFieldContent(x-2,y-1)==skoczek){
            rescueFields.add(new Point(x-2,y-1));
            return true;
        }
        if(getFieldContent(x-2,y+1)==skoczek){
            rescueFields.add(new Point(x-2,y+1));
            return true;
        }
        if(getFieldContent(x+2,y-1)==skoczek){
            rescueFields.add(new Point(x+2,y-1));
            return true;
        }
        if(getFieldContent(x+2,y+1)==skoczek){
            rescueFields.add(new Point(x+2,y+1));
            return true;
        }
        if(getFieldContent(x-1,y-2)==skoczek){
            rescueFields.add(new Point(x-1,y-2));
            return true;
        }
        if(getFieldContent(x-1,y+2)==skoczek){
            rescueFields.add(new Point(x-1,y+2));
            return true;
        }
        if(getFieldContent(x+1,y-2)==skoczek){
            rescueFields.add(new Point(x+1,y-2));
            return true;
        }
        if(getFieldContent(x+1,y+2)==skoczek){
            rescueFields.add(new Point(x+1,y+2));
            return true;
        }

        rescueFields.clear();
        return false;
    }
    private boolean isFieldInCheckByOtherKing(int x,int y, boolean isKingWhite){
        byte krol= KING;

        if(!isKingWhite)
            krol |=WHITE;

        if(getFieldContent(x+1, y)==krol
                || getFieldContent(x+1, y+1)==krol
                || getFieldContent(x, y+1)==krol
                || getFieldContent(x-1, y+1)==krol
                || getFieldContent(x-1, y)==krol
                || getFieldContent(x-1, y-1)==krol
                || getFieldContent(x, y-1)==krol
                || getFieldContent(x+1, y-1)==krol)
            return true;

        return false;
    }


    //czy jakiś pion bije figure danego koloru na danym polu
    private boolean isFieldInCheckByPion(int x,int y, boolean isFigureWhite){
        if(board[y][x]==0)//nie ma figury wiec na pewno nic nie bije
            return false;
        if(isFigureWhite){
            rescueFields.clear();
            if( getFieldContent(x-1, y-1)==PION){
                rescueFields.add(new Point(x-1,y-1));

                if(enPassantBlackX==x-1
                        && (getFieldContent(x,y-1)==(PION | WHITE)
                        || getFieldContent(x-2,y-1)==(PION | WHITE)))
                    rescueFields.add(new Point(x-1,y-2));

                return true;
            }
            rescueFields.clear();
            if(getFieldContent(x+1, y-1)==PION){
                rescueFields.add(new Point(x+1,y-1));

                if(enPassantBlackX==x+1
                        && (getFieldContent(x,y-1)==(PION | WHITE)
                        || getFieldContent(x+2,y-1)==(PION | WHITE)))
                    rescueFields.add(new Point(x+1,y-2));

                return true;
            }
        }
        else{
            rescueFields.clear();
            if( getFieldContent(x-1, y+1)==(PION | WHITE)){
                rescueFields.add(new Point(x-1,y+1));

                if(enPassantWhiteX==x-1
                        && (getFieldContent(x,y+1)==PION
                        || getFieldContent(x-2,y+1)==PION))
                    rescueFields.add(new Point(x-1,y-2));

                return true;
            }
            rescueFields.clear();
            if(getFieldContent(x+1, y+1)==(PION | WHITE)){
                rescueFields.add(new Point(x+1,y+1));

                if(enPassantWhiteX==x+1
                        && (getFieldContent(x,y+1)==PION
                        || getFieldContent(x+2,y+1)==PION))
                    rescueFields.add(new Point(x+1,y+2));

                return true;
            }
        }

        rescueFields.clear();
        return false;
    }


    //czy jest jakiś pion koloru isPeonWhite który
    //może wejść w danym momencie na pole (x,y) ale nie bić.
    private boolean canPionWalkOnField(int x,int y,boolean isPionWhite){
        if(isPionWhite){
            if(getFieldContent(x,y)==0
                    && getFieldContent(x,y+1)==(PION | WHITE))//pion rusza się o pole
                return true;
            if(y==4
                    && board[y][x]==0
                    && board[y+1][x]==0
                    && board[y+2][x]==(PION | WHITE))//pion rusza się o 2 pola
                return true;
        }
        else{
            if(getFieldContent(x,y)==0
                    && getFieldContent(x,y-1)==PION)//pion rusza się o pole
                return true;
            if(y==3
                    && board[y][x]==0
                    && board[y-1][x]==0
                    && board[y-2][x]==PION)//pion rusza się o 2 pola
                return true;
        }
        return false;
    }

    //czy król danego koloru(lub inna figura) jest pod szachem(może być zbita) na danym polu
    private boolean isFieldInCheck(int x,int y, boolean isKingWhite){
        if(isKingWhite){
            return(isFieldInCheckStraightLine(x,y,true)
                    || isFieldInCheckSlantingLine(x, y, true)
                    || isFieldInCheckBySkoczek(x, y, true)
                    || isFieldInCheckByOtherKing(x,y,true)
                    || isFieldInCheckByPion(x, y, true));
        }
        else{
            return(isFieldInCheckStraightLine(x,y,false)
                    || isFieldInCheckSlantingLine(x, y, false)
                    || isFieldInCheckBySkoczek(x, y, false)
                    || isFieldInCheckByOtherKing(x,y,false)
                    || isFieldInCheckByPion(x, y, false));
        }
    }



    //czy jakaś figura danego koloru może w danym momencie wejść na pole (x,y)
    private boolean canFigureWalkOnField(int x,int y, boolean isFigureWhite){
        if(isFigureWhite){
            return(isFieldInCheckStraightLine(x,y,false)
                    || isFieldInCheckSlantingLine(x, y, false)
                    || isFieldInCheckBySkoczek(x, y, false)
                    || isFieldInCheckByPion(x, y, false)
                    || canPionWalkOnField(x, y, true));
        }
        else{
            return(isFieldInCheckStraightLine(x,y,true)
                    || isFieldInCheckSlantingLine(x, y, true)
                    || isFieldInCheckBySkoczek(x, y, true)
                    || isFieldInCheckByPion(x, y, true)
                    || canPionWalkOnField(x, y, false));
        }
    }


    //allows or denies the right to take peons en-passant.
    private void manageEnPassantability(int yFrom,int xTo,int yTo){
        enPassantWhiteX=-1;
        enPassantBlackX=-1;
        //if black peon moved 2 fields
        if(getFieldContent(xTo,yTo)==PION && yTo==yFrom+2)
            enPassantBlackX=xTo;
            //if black peon moved 2 fields
        else if(getFieldContent(xTo,yTo)==(PION | WHITE)&& yTo==yFrom-2)
            enPassantWhiteX=xTo;
    }

    private boolean isFieldTakenByFigureOfGivenColor(int x,int y,boolean isColorWhite){
        if(!isColorWhite){
            if(getFieldContent(x,y)==PION		//check if given..
                    || getFieldContent(x,y)== ROOK        //field is...
                    || getFieldContent(x,y)== BISHOP        //taken by any of...
                    || getFieldContent(x,y)== KNIGHT    //the black figures..
                    || getFieldContent(x,y)== KING        //...
                    || getFieldContent(x,y)== QUEEN)	//.
                return true;
        }
        else if(WHITE==(board[y][x] & WHITE))
            return true;
        return false;
    }

    //wywoływać tylko po upewnieniu się że żadne z pól nie jest puste
    private boolean isFieldTakenByFigureOfSameColour(
            int xFrom,int yFrom,int xTo,int yTo){
        if((board[yFrom][xFrom] & WHITE)==0){//this is a black figure
            if(getFieldContent(xTo,yTo)==PION			//check if field we're..
                    || getFieldContent(xTo,yTo)== ROOK        //moving to is...
                    || getFieldContent(xTo,yTo)== BISHOP        //taken by any of...
                    || getFieldContent(xTo,yTo)== KNIGHT    //the black figures
                    || getFieldContent(xTo,yTo)== KING        //
                    || getFieldContent(xTo,yTo)== QUEEN)	//
                return true;
        }
        else//this is a white figure
            if(WHITE==(board[yTo][xTo] & WHITE))
                return true;
        return false;
    }



    //czy pion dokona bicia wchodząc na pole x,y
    private boolean isFieldForPionToTake(int x,int y,boolean isPeonWhite){
        //is there an opponent figure on that field?
        if(board[y][x]!=0
                //or can black peon can be taken en-passant?
                || isPeonWhite && enPassantBlackX==x && EN_PASSANT_BLACK_Y==y+1
                //or can white peon can be taken en-passant?
                || !isPeonWhite && enPassantWhiteX==x && EN_PASSANT_WHITE_Y==y-1)
            return true;
        return false;
    }

    private boolean isPionDisplacementLegal(int xFrom,int yFrom,int xTo,int yTo){
        if((board[yFrom][xFrom] & WHITE)==WHITE && yTo==yFrom-1){//biały pion bije
            if((xTo==xFrom+1 || xTo==xFrom-1) && isFieldForPionToTake(xTo,yTo,true))
                return true;
        }
        else if((board[yFrom][xFrom] & WHITE)==0 && yTo==yFrom+1){//czarny pion bije
            if((xTo==xFrom+1 || xTo==xFrom-1)&& isFieldForPionToTake(xTo,yTo,false))
                return true;
        }
        if((board[yFrom][xFrom] & HAS_MOVED)==0){//pion nie ruszył się jeszcze
            if((board[yFrom][xFrom] & WHITE)==WHITE){//biały pion
                if(xFrom!=xTo//moves sideways
                        || board[yTo][xTo]!=0//destination is taken
                        || yTo-yFrom!=-2 && yTo-yFrom!=-1//does not move 1 or 2 fields fwd
                        || yTo-yFrom==-2 && board[yFrom-1][xFrom]!=0)//jumps over sth
                    return false;
            }else{									 //czarny pion
                if(xFrom!=xTo || board[yTo][xTo]!=0
                        || yTo-yFrom!=2 && yTo-yFrom!=1
                        || yTo-yFrom==2 && board[yFrom+1][xFrom]!=0)
                    return false;
            }
        }
        else{
            if((board[yFrom][xFrom] & WHITE)==WHITE){//biały pion
                if(xFrom!=xTo || yTo-yFrom!=-1 || board[yTo][xTo]!=0)
                    return false;
            }else{									//czarny pion
                if(xFrom!=xTo || yTo-yFrom!=1  || board[yTo][xTo]!=0)
                    return false;
            }
        }
        return true;
    }

    //assumes that goniec is moving sideways
    private boolean isGoniecJumpingOver(int xFrom,int yFrom,int xTo,int yTo){
        if(xFrom<xTo && yFrom<yTo){
            for(int i=1;i<xTo-xFrom;i++)
                if(board[yFrom+i][xFrom+i]!=0)
                    return true;
        }
        else if(xFrom<xTo && yFrom>yTo){
            for(int i=1;i<xTo-xFrom;i++)
                if(board[yFrom-i][xFrom+i]!=0)
                    return true;
        }
        else if(xFrom>xTo && yFrom<yTo){
            for(int i=1;i<xFrom-xTo;i++)
                if(board[yFrom+i][xFrom-i]!=0)
                    return true;
        }
        else{//xFrom>xTo && yFrom>yTo
            for(int i=1;i<xFrom-xTo;i++)
                if(board[yFrom-i][xFrom-i]!=0)
                    return true;
        }
        return false;
    }

    //assumes that wieza is moving horizontally or vertically
    private boolean isWiezaJumpingOver(int xFrom,int yFrom,int xTo,int yTo){
        if(yFrom<yTo){
            for(int i=yFrom+1;i<yTo;i++)
                if(board[i][xFrom]!=0)
                    return true;
        }
        else if(yFrom>yTo){
            for(int i=yFrom-1;i>yTo;i--)
                if(board[i][xFrom]!=0)
                    return true;
        }
        if(xFrom<xTo){
            for(int i=xFrom+1;i<xTo;i++)
                if(board[yFrom][i]!=0)
                    return true;
        }
        else{// if(xFrom>xTo)
            for(int i=xFrom-1;i>xTo;i--)
                if(board[yFrom][i]!=0)
                    return true;
        }
        return false;
    }

    private boolean isDisplacementLegal(int xFrom,int yFrom,int xTo,int yTo){
        byte figure=(byte)(getFieldContent(xFrom, yFrom) & 7);//ignore color of figure
        switch(figure){
            case BISHOP:
                if(Math.abs(xTo-xFrom)!=Math.abs(yTo-yFrom)
                        || isGoniecJumpingOver(xFrom,yFrom,xTo,yTo))
                    return false;
                break;
            case ROOK:
                if(xFrom!=xTo && yFrom!=yTo
                        || isWiezaJumpingOver(xFrom,yFrom,xTo,yTo))
                    return false;
                break;
            case KNIGHT:
                if(Math.abs(xTo-xFrom)==2 && Math.abs(yTo-yFrom)==1)
                    return true;
                else if(Math.abs(xTo-xFrom)==1 && Math.abs(yTo-yFrom)==2)
                    return true;
                else
                    return false;
            case QUEEN:
                if((xFrom==xTo || yFrom==yTo)
                        //QUEEN behaves like ROOK
                        && !isWiezaJumpingOver(xFrom,yFrom,xTo,yTo))
                    return true;
                if(Math.abs(xTo-xFrom)==Math.abs(yTo-yFrom)
                        //QUEEN behaves like BISHOP
                        && !isGoniecJumpingOver(xFrom,yFrom,xTo,yTo))
                    return true;
                return false;
            case KING:
                if(Math.abs(xTo-xFrom)>1 || Math.abs(yTo-yFrom)>1)
                    return false;
                break;
            case PION:
                if(!isPionDisplacementLegal(xFrom,yFrom,xTo,yTo))
                    return false;
        }
        return true;
    }

    private void takeEnPassant(int xFrom,int yFrom,int xTo,int yTo){
        //if black peon did en-passant take
        if(getFieldContent(xFrom,yFrom)==PION
                && yFrom==4 && enPassantWhiteX==xTo)
            board[EN_PASSANT_WHITE_Y][enPassantWhiteX]=0;
        //if white peon did en-passant take
        if(getFieldContent(xFrom,yFrom)==(PION | WHITE)
                && yFrom==3 && enPassantBlackX==xTo)
            board[EN_PASSANT_BLACK_Y][enPassantBlackX]=0;
    }

    //determines if the color of the figure at x,y
    //is the same as turn color
    public	boolean isTheTurnCorrect(int x,int y){
        if((getFieldContent(x,y) & WHITE)==WHITE && isWhiteTurn
                || (getFieldContent(x,y) & WHITE)==0 && !isWhiteTurn
                && (getFieldContent(x,y) & 15)!=0)
            return true;
        return false;
    }

    private boolean isMoveIsLegal(int xFrom,int yFrom,int xTo,int yTo){
        if(!isMoveValid(xFrom, yFrom, xTo, yTo)
                || !isTheTurnCorrect(xFrom,yFrom)
                || isFieldTakenByFigureOfSameColour(xFrom,yFrom,xTo,yTo)
                || !isDisplacementLegal(xFrom,yFrom,xTo,yTo)
                || wouldKingBeInCheck(xFrom, yFrom, xTo, yTo))
            return false;
        return true;
    }

    private void manageKingsPositions(int x,int y){
        if(getFieldContent(x, y)==(KING | WHITE)){
            whiteKingX=x;
            whiteKingY=y;
        }
        else if(getFieldContent(x, y)== KING){
            blackKingX=x;
            blackKingY=y;
        }
    }

    private boolean isFieldOutsideBoard(int x,int y){
        if(x<0 || x>7 || y<0 || y>7)
            return true;
        return false;
    }

    //could king of given colour be placed on given field
    private boolean couldKingEscapeTo(int x,int y,boolean isKingWhite){
        if(isFieldOutsideBoard(x, y)
                || isFieldTakenByFigureOfGivenColor(x, y, isKingWhite)
                || isFieldInCheck(x,y,isKingWhite))
            return false;
        return true;
    }

    //checks if the previous move resulted in check, check-mate or stalemate
    private int checkMoveResult(){
        if(isWhiteTurn){//if previous move was move of black player (white king could be in check)
            if(!couldKingEscapeTo(whiteKingX-1,whiteKingY,true)
                    && !couldKingEscapeTo(whiteKingX-1,whiteKingY-1,true)
                    && !couldKingEscapeTo(whiteKingX,whiteKingY-1,true)
                    && !couldKingEscapeTo(whiteKingX+1,whiteKingY-1,true)
                    && !couldKingEscapeTo(whiteKingX+1,whiteKingY,true)
                    && !couldKingEscapeTo(whiteKingX+1,whiteKingY+1,true)
                    && !couldKingEscapeTo(whiteKingX,whiteKingY+1,true)
                    && !couldKingEscapeTo(whiteKingX-1,whiteKingY+1,true)
                    && isFieldInCheck(whiteKingX, whiteKingY, true)){//to koniecznie jako ostatnie w if()

                ArrayList<Point> tmp=copyRescueFields();

                for(int i=0; i<tmp.size();i++){
                    if(canFigureWalkOnField(tmp.get(i).x, tmp.get(i).y,true))
                        return RES_MOVE_OK;
                }
                return RES_BLACK_WINS;
            }

        }
        else{//if previous move was move of white player( black king could be in check)
            if(!couldKingEscapeTo(blackKingX-1,blackKingY,false)
                    && !couldKingEscapeTo(blackKingX-1,blackKingY-1,false)
                    && !couldKingEscapeTo(blackKingX,blackKingY-1,false)
                    && !couldKingEscapeTo(blackKingX+1,blackKingY-1,false)
                    && !couldKingEscapeTo(blackKingX+1,blackKingY,false)
                    && !couldKingEscapeTo(blackKingX+1,blackKingY+1,false)
                    && !couldKingEscapeTo(blackKingX,blackKingY+1,false)
                    && !couldKingEscapeTo(blackKingX-1,blackKingY+1,false)
                    && isFieldInCheck(blackKingX, blackKingY, false)){//to koniecznie jako ostatnie w if()

                ArrayList<Point> tmp=copyRescueFields();

                for(int i=0; i<tmp.size();i++){
                    if(canFigureWalkOnField(tmp.get(i).x, tmp.get(i).y,false))
                        return RES_MOVE_OK;
                }
                return RES_WHITE_WINS;
            }
        }
        return RES_MOVE_OK;
    }

    private boolean isMoveValid(int xFrom,int yFrom,int xTo,int yTo){
        if((xFrom==xTo && yFrom==yTo)//is moving to the same place
                || xTo<0 || xTo>7 || yTo<0 || yTo>7//is out of bounds
                || xFrom<0 || xFrom>7 || yFrom<0 || yFrom>7//is out of bounds
                || board[yFrom][xFrom]==0)//is starting field empty
            return false;
        return true;
    }

    private void managePionSwap(int x,int y,int promo){
        if(promo!= QUEEN && promo!= BISHOP && promo!= ROOK && promo!= KNIGHT)
            promo= QUEEN;
        if(getFieldContent(x, y)==PION && y==7)
            board[y][x]=(byte)promo;
        else if(getFieldContent(x, y)==(PION | WHITE) && y==0)
            board[y][x]=(byte)(promo | WHITE);

    }

    private ArrayList<Point> copyRescueFields(){
        ArrayList<Point> copy=new ArrayList<Point>();
        for(int i=0; i<rescueFields.size();i++)
            copy.add(rescueFields.get(i));
        return copy;
    }

    //returns one of
    //public static final int RES_MOVE_OK
    // public static final int RES_MOVE_ILLEGAL
    // public static final int RES_WHITE_WINS
    // public static final int RES_BLACK_WINS
    // public static final int RES_DRAW
    public int moveFigure(int xFrom,int yFrom,int xTo,int yTo,int promo){
        if(lastResult==RES_BLACK_WINS
                || lastResult==RES_WHITE_WINS
                || lastResult==RES_DRAW)
            return lastResult;
        if(isMoveIsLegal(xFrom,yFrom,xTo,yTo)){
            takeEnPassant(xFrom,yFrom,xTo,yTo);
            board[yTo][xTo]=(byte)(board[yFrom][xFrom] | HAS_MOVED);
            board[yFrom][xFrom]=0;
            manageEnPassantability(yFrom,xTo,yTo);
            manageKingsPositions(xTo,yTo);
            managePionSwap(xTo,yTo,promo);
            isWhiteTurn=!isWhiteTurn;
            lastResult = checkMoveResult();
            return lastResult;
        }
        return RES_MOVE_ILLEGAL;
    }

    public boolean isTurnOfWhite(){
        return isWhiteTurn;
    }
}
