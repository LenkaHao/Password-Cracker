import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

class ClientHandler extends Thread{
  // DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd");
  // DateFormat fortime = new SimpleDateFormat("hh:mm:ss");
  protected final InputStream Cdis;
  protected final OutputStream Cdos;
  protected final Socket Cs;
  protected int GROUP_SIZE = 1000;
  // protected int WORKER_SIZE_BOUND = 10;
  protected int WORKER_SIZE = 0;
  protected Semaphore available = new Semaphore(WORKER_SIZE, true);
  protected Semaphore waitReConfig = new Semaphore(1, true);
  protected ConcurrentLinkedQueue<String> reSubmittedParts = new ConcurrentLinkedQueue<>();

  // Constructor
  public ClientHandler(Socket Cs, InputStream Cdis, OutputStream Cdos)
  {
      this.Cs = Cs;
      this.Cdis = Cdis;
      this.Cdos = Cdos;
  }
}
