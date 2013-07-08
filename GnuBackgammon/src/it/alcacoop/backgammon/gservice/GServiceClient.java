package it.alcacoop.backgammon.gservice;

import it.alcacoop.backgammon.GnuBackgammon;
import it.alcacoop.backgammon.fsm.BaseFSM.Events;

import java.io.*; 
import java.net.*;


public class GServiceClient implements GServiceMessages {
  
  private Socket clientSocket;
  private DataOutputStream outToServer;
  private BufferedReader inFromServer;
  private boolean active = false;
  public static GServiceClient instance;

  public GServiceNetHandler queue;
  public GServiceCookieMonster coockieMonster;
  
  private GServiceClient() {
    queue = new GServiceNetHandler();
    coockieMonster = new GServiceCookieMonster();
  }

  public static GServiceClient getInstance() {
    if (instance == null) instance = new GServiceClient();
    return instance;
  }
  
  public void onError(Socket s) throws IOException {
    active=false;
    s.close();
    GnuBackgammon.fsm.processEvent(Events.GSERVICE_ERROR, null);
    queue.reset();
  }
  
  public void connect() {
    try {
      clientSocket = new Socket("dmartella.homelinux.net", 4321);
      outToServer = new DataOutputStream(clientSocket.getOutputStream());
      inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    } catch (Exception e) {
      active = false;
      return;
    }
    
    
    Runnable r = new Runnable() {
      @Override
      public void run() {
        System.out.println("STARTING READER THREAD..");
        active = true;
        while (active) {
          try {
            String s = inFromServer.readLine();
            if (s==null) {
              onError(clientSocket);
              break;
            }
            System.out.println("RECEIVED: "+s);
            int coockie = coockieMonster.fIBSCookie(s);
            switch (coockie) {
              case GSERVICE_CONNECTED:
                GnuBackgammon.fsm.processEvent(Events.GSERVICE_CONNECTED, null);
                break;
              case GSERVICE_READY:
                GnuBackgammon.fsm.processEvent(Events.GSERVICE_READY, null);
                break;
              case GSERVICE_HANDSHAKE:
                String chunks[] = s.split(" ");
                GnuBackgammon.fsm.processEvent(Events.GSERVICE_HANDSHAKE, Long.parseLong(chunks[1]));
                break;
              case GSERVICE_OPENING_ROLL:
                chunks = s.split(" ");
                int p[] = {Integer.parseInt(chunks[1]), Integer.parseInt(chunks[2]), Integer.parseInt(chunks[3])};
                queue.post(Events.GSERVICE_FIRSTROLL, p);
                break;
              case GSERVICE_ROLL:
                chunks = s.split(" ");
                int dices[] ={0, 0};
                for (int i=1;i<3;i++)
                  dices[i-1] = Integer.parseInt(chunks[i]);
                queue.post(Events.GSERVICE_ROLL, dices);
                break;
              case GSERVICE_MOVE:
                chunks = s.split(" ");
                int moves[] ={-1, -1, -1, -1, -1, -1, -1, -1};
                for (int i=0;i<8;i++)
                  moves[i] = Integer.parseInt(chunks[i+1]);
                queue.post(Events.GSERVICE_MOVES, moves);
                break;
              case GSERVICE_BOARD:
                chunks = s.split(" ");
                int[][] board = new int[2][25];
                for (int i=0;i<25;i++) board[0][i] = Integer.parseInt(chunks[i+1]);
                for (int i=25;i<50;i++) board[1][i-25] = Integer.parseInt(chunks[i+1]);
                queue.post(Events.GSERVICE_BOARD, board);
                break;
              case GSERVICE_CHATMSG:
                s = s.replace("90 ", "");
                GnuBackgammon.fsm.processEvent(Events.GSERVICE_CHATMSG, s);
                break;
              case GSERVICE_ABANDON:
                chunks = s.split(" ");
                GnuBackgammon.fsm.processEvent(Events.GSERVICE_ABANDON, Integer.parseInt(chunks[1]));
                break;
              case GSERVICE_ERROR:
                onError(clientSocket);
                break;
              case GSERVICE_BYE:
                active = false;
                GnuBackgammon.fsm.processEvent(Events.GSERVICE_BYE, null);
                queue.reset();
                break;
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        System.out.println("SHUTTING DOWN");
      }
    };

    Thread t = new Thread(r);
    t.start();
  }
  
  public void precessReceivedMessage(String s) {
	  System.out.println("RECEIVED: "+s);
      int coockie = coockieMonster.fIBSCookie(s);
      switch (coockie) {
        case GSERVICE_CONNECTED:
          GnuBackgammon.fsm.processEvent(Events.GSERVICE_CONNECTED, null);
          break;
        case GSERVICE_READY:
          GnuBackgammon.fsm.processEvent(Events.GSERVICE_READY, null);
          break;
        case GSERVICE_HANDSHAKE:
          String chunks[] = s.split(" ");
          GnuBackgammon.fsm.processEvent(Events.GSERVICE_HANDSHAKE, Long.parseLong(chunks[1]));
          break;
        case GSERVICE_OPENING_ROLL:
          chunks = s.split(" ");
          int p[] = {Integer.parseInt(chunks[1]), Integer.parseInt(chunks[2]), Integer.parseInt(chunks[3])};
          queue.post(Events.GSERVICE_FIRSTROLL, p);
          break;
        case GSERVICE_ROLL:
          chunks = s.split(" ");
          int dices[] ={0, 0};
          for (int i=1;i<3;i++)
            dices[i-1] = Integer.parseInt(chunks[i]);
          queue.post(Events.GSERVICE_ROLL, dices);
          break;
        case GSERVICE_MOVE:
          chunks = s.split(" ");
          int moves[] ={-1, -1, -1, -1, -1, -1, -1, -1};
          for (int i=0;i<8;i++)
            moves[i] = Integer.parseInt(chunks[i+1]);
          queue.post(Events.GSERVICE_MOVES, moves);
          break;
        case GSERVICE_BOARD:
          chunks = s.split(" ");
          int[][] board = new int[2][25];
          for (int i=0;i<25;i++) board[0][i] = Integer.parseInt(chunks[i+1]);
          for (int i=25;i<50;i++) board[1][i-25] = Integer.parseInt(chunks[i+1]);
          queue.post(Events.GSERVICE_BOARD, board);
          break;
        case GSERVICE_CHATMSG:
          s = s.replace("90 ", "");
          GnuBackgammon.fsm.processEvent(Events.GSERVICE_CHATMSG, s);
          break;
        case GSERVICE_ABANDON:
          chunks = s.split(" ");
          GnuBackgammon.fsm.processEvent(Events.GSERVICE_ABANDON, Integer.parseInt(chunks[1]));
          break;
        case GSERVICE_ERROR:
//          onError(clientSocket);
          break;
        case GSERVICE_BYE:
          active = false;
          GnuBackgammon.fsm.processEvent(Events.GSERVICE_BYE, null);
          queue.reset();
          break;
      }
  }
  
  public void disconnect() {
    try {
      sendMessage("BYE\n");
      active = false;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void sendMessage(String msg) {
	GnuBackgammon.Instance.nativeFunctions.gserviceSendReliableRealTimeMessage(msg);
//    try {
//      outToServer.writeBytes(msg+"\n");
//    } catch (Exception e) {}
  }
}