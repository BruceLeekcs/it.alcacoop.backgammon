package it.alcacoop.gnubackgammon.layers;

import it.alcacoop.gnubackgammon.GameScreen;
import it.alcacoop.gnubackgammon.actors.BoardImage;
import it.alcacoop.gnubackgammon.actors.Checker;
import it.alcacoop.gnubackgammon.actors.Dices;
import it.alcacoop.gnubackgammon.actors.Points;
import it.alcacoop.gnubackgammon.logic.FSM.Events;
import it.alcacoop.gnubackgammon.logic.AvailableMoves;
import it.alcacoop.gnubackgammon.logic.MatchState;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import java.util.Stack;



public class Board extends Group {
  
  public int[][] _board;
  public int[] bearedOff = {0,0};
  public Stack<int[]> moves;

  public Vector2 pos[];
  public BoardImage bimg;
  public Checker checkers[][];
  private Checker lastMoved = null;
  public Checker selected = null;
  
  public Points points;
  public Dices dices;
  public AvailableMoves availableMoves;
  
  
  public Board() {
    _board = new int[2][25];
    
    moves = new Stack<int[]>();
    availableMoves = new AvailableMoves(this);
    checkers = new Checker[2][15]; //[0]=WHITE [1]=BLACK
    
    bimg = new BoardImage();
    bimg.setX(0);
    bimg.setY(0);
    addActor(bimg);

    pos = new Vector2[25];
    for (int i=0; i<24;i++) {
      pos[i] = new Vector2();
      if (i<6) {
        pos[i].x = bimg.getX()+(836-(59*i));
        pos[i].y = bimg.getY()+590;
      }
      if ((i>=6)&&(i<12)) {
        pos[i].x = bimg.getX()+(765-(59*i));
        pos[i].y = bimg.getY()+590;
      }
    }
    for (int i=0; i<12;i++) {
      pos[i+12] = new Vector2();
      if (i<6) {
        pos[i+12].x = bimg.getX()+(116+(59*i));
        pos[i+12].y = bimg.getY()+50;
      }
      if ((i>=6)&&(i<12)) {
        pos[i+12].x = bimg.getX()+(187+(59*i));
        pos[i+12].y = bimg.getY()+50;
      }
    }

    pos[24] = new Vector2();  //HITTED
    pos[24].x = bimg.getX()+476;
    pos[24].y = bimg.getY();
    
    points = new Points(this);
    addActor(points);
    
    for (int i = 0; i<15; i++) {
      checkers[0][i] = new Checker(this, 0);
      checkers[1][i] = new Checker(this, 1);
      addActor(checkers[0][i]);
      addActor(checkers[1][i]);
    }
    
    dices = new Dices(this, bimg.getX()+1005/2, bimg.getY()+692/2);
    addActor(dices);
  }


  public Vector2 getBoardCoord(int color, int x, int y){
    if (y>4) y=4;
    Vector2 ret = new Vector2();

    switch (x) {
    
      case -1: //BEAR OFF
        ret.x = bimg.getX()+918.5f; 
        if (color==1) ret.y = bimg.getY()+605-bearedOff[color]*14;
        else ret.y = bimg.getY()+35+bearedOff[color]*14;
        break;
        
      case 24: //BAR
        ret.x = bimg.getX() + pos[x].x;
        if (color==0) ret.y=bimg.getY() + 570 - (49*y);
        else ret.y=bimg.getY() + 70 + (49*y);
        break;
        
      default: //ON THE TABLE
        if (color==0) { //WHITE
          ret.x = pos[23-x].x;
          if (x>11) ret.y = pos[23-x].y - (49*y);
          else ret.y = pos[23-x].y + (49*y);
        } else { //BLACK
          ret.x = pos[x].x;
          if (x>11) ret.y = pos[x].y + (49*y);
          else ret.y = pos[x].y - (49*y);
        }
    }
    return ret;
  }


  public void initBoard() {
    for (int i=0; i<24; i++) {
      _board[0][i] = MatchState.board[0][i];
      _board[1][i] = MatchState.board[1][i];
    }
    bearedOff[0] = 0;
    bearedOff[1] = 0;
    dices.hide();

    int nchecker = 0;
    //POSITIONING WHITE CHECKERS
    for (int i=0; i<25; i++) {
      for (int j=0;j<_board[0][i];j++) {
        checkers[0][nchecker].reset(i,j);
        nchecker++;
      }
    }
    //POSITIONING BLACK CHECKERS
    nchecker = 0;
    for (int i=0; i<25; i++) {
      for (int j=0;j<_board[1][i];j++) {
        checkers[1][nchecker].reset(i, j);
        nchecker++;
      }
    }
  }

  
  Checker getChecker(int color, int x) {
    Checker _c = null;
    int y = _board[color][x]-1;
    for (int i = 0; i<15; i++) {
      Checker c = checkers[color][i];
      if ((c.boardX==x)&&(c.boardY==y))
        _c = c;
    }
    return _c;
  }


  public void setMoves(int _moves[]) {
    moves.clear();
    if (_moves.length<8) return;

    int ms = 0;
    
    for (int i=3;i>=0;i--) {
      if (_moves[2*i]!=-1) {
        int m[] = new int[2];
        m[0] = _moves[2*i];
        m[1] = _moves[2*i+1];
        moves.push(m);
        ms++;
      }
    }

    if (ms==1) performNextMove(true);
    else performNextMove();
  }


  public void performNextMove() { 
    performNextMove(false);
  }
  public void performNextMove(boolean nodelay) {
    try {
      int m[] = moves.pop();
      if (m!=null) {
        Checker c = getChecker(MatchState.fMove, m[0]);
        if (nodelay) c.moveTo(m[1]);
        else c.moveToDelayed(m[1], 0.2f);
        availableMoves.removeMoves(m[0], m[1]);
        lastMoved = c;
      }  
    } catch (Exception e) {
      GameScreen.fsm.processEvent(Events.NO_MORE_MOVES, null);
    }
  }

  
  public boolean checkHit() {
    if (lastMoved!=null) {
      if (lastMoved.boardX==-1) return false; //BEARED OFF
      
      int c = lastMoved.getSpecularColor();
      int p = lastMoved.getSpecularPosition();
      if (_board[c][p]>0) {
        //CHECKER HITTED
        Checker ch = getChecker(c, p);
        ch.moveTo(24);
        return true;
      }
    }
    return false;
  }

  
  public void selectChecker(int x) {
    if (_board[MatchState.fMove][x]>0) {

      points.reset();
      if (selected!=null) selected.highlight(false);
      
      Checker c = getChecker(MatchState.fMove, x);
      
      if ((selected!=null)&&(c.boardX==selected.boardX)) {
        selected = null;
        return;
      }
      
      int ps[] = availableMoves.getPoints(x);
      //System.out.println("AVAILABLE POINTS: "+ps.length);
      if ((ps==null)||(ps.length==0)) { //NO MOVES FROM HERE!
        c.highlight(false);
        selected = null;        
      } else {//if (ps.length!=0) {
        c.highlight(true);
        selected = c;
        for (int i=0; i<ps.length;i++)
          points.get(ps[i]).highlight();
      }
        
    }
  }
  
  
  public void setDices(int d1, int d2) {
    dices.show(d1, d2);
  }
  
  public int bearingOff() {
    int count = 0;
    for(int i=6;i<25;i++){
      if(_board[MatchState.fMove][i] > 0){
        count+=_board[MatchState.fMove][i];
      }
    }//if count = 0 here, we're in bearoff

    int max_point = 0;
    if(count==0) { //bearoff
      for(int i=0;i<6;i++){
        if(_board[MatchState.fMove][i] > 0){
          max_point = i;
        }
      }//max_point here is the bigger point with checkers 
    }
    if(count!=0)
      return -1;
    else 
      return max_point;
  }

  public int getPIPS() {
    int pips = 0;
    for (int i=0;i<_board[MatchState.fMove].length;i++) {
      pips += _board[MatchState.fMove][i]*(i+1);
    }
    //System.out.println("PIPS: "+pips);
    return pips;
  }
  
  public boolean gameFinished() {
    if (getPIPS()>0) return false;
    else return true;
  }
} //END CLASS