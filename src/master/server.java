// Java implementation of  Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;

// Server class
public class Server
{
    // for loadbalancing
    private static int LastUsedHost = 0;
    // private static int PartSize = 1000;
    private static int GROUP_SIZE = 1000;
    private static int WORKER_SIZE_BOUND = 10;
    // worker hostname: thread obj
    private HashMap<Socket, Thread> WorkerJobs = new HashMap<>();
    private ArrayList<WorkerInfo> WorkerList = new ArrayList<>();
    public class WorkerInfo{
      private final Socket Ws;
      private final DataInputStream Wdis;
      private final DataOutputStream Wdos;
      public WorkerInfo(Socket Ws, DataInputStream Wdis, DataOutputStream Wdos){
        this.Ws = Ws;
        this.Wdis = Wdis;
        this.Wdos = Wdos;
      }
      public Socket GetWs(){
        return Ws;
      }
      public DataInputStream GetWdis(){
        return Ws;
      }
      public DataOutputStream GetWdos(){
        return Ws;
      }
    }
    public static void main(String[] args) throws IOException
    {
        // server is listening on port 5056
        ServerSocket ss = new ServerSocket(8000);

        // running infinite loop for getting
        // client request
        while (true)
        {
            Socket s = null;

            try
            {
                // socket object to receive incoming client requests
                s = ss.accept();

                System.out.println("A new client is connected : " + s);

                // obtaining input and out streams
                DataInputStream Cdis = new DataInputStream(s.getInputStream());
                DataOutputStream Cdos = new DataOutputStream(s.getOutputStream());

                System.out.println("Assigning new thread for this client");

                // create a new thread object
                Thread t = new Dispatcher(s, Cdis, Cdos);

                // Invoking the start() method
                t.start();

            }
            catch (Exception e) {
                s.close();
                e.printStackTrace();
            }
        }
    }

}
class Dispatcher extends Threads{
  private final Socket s;
  private final DataInputStream Cdis;
  private final DataOutputStream Cdos;
  public Dispatcher(Socket s, DataInputStream Cdis, DataOutputStream Cdos) {
    this.s = s;
    this.Cdis = Cdis;
    this.Cdos = Cdos;
  }
  public void Dispatch(String s) {
    String[] arrOfStr = s.split("/", 2);
    if(arrOfStr[0].equals("r")) {
      Thread t = new JobHandler(arrOfStr, s, Cdis, Cdos);

      t.start();
      // return t;
    } else if(arrOfStr[0].equals("p")) {
      LastUsedHost = 0;
      GROUP_SIZE = arrOfStr[1];
      // return null;
    } else if(arrOfStr[0].equals("n")) {
      LastUsedHost = 0;
      WORKER_SIZE_BOUND = arrOfStr[1];
      // return null;
    } else {
      // return null
    }
  }

  @Override
  public void run()
  {
    String received;
    try{
      received = Cdis.readUTF();
    } catch (IOException e) {
        e.printStackTrace();
    }

    Dispatch(received);
    // Thread t = new ch(s);
    // t.start();
  }
}

// ClientHandler class
class ClientHandler extends Threads{
  // DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd");
  // DateFormat fortime = new SimpleDateFormat("hh:mm:ss");
  protected final DataInputStream Cdis;
  protected final DataOutputStream Cdos;
  protected final Socket Cs;

  // Constructor
  public ClientHandler(Socket Cs, DataInputStream Cdis, DataOutputStream Cdos)
  {
      this.Cs = Cs;
      this.Cdis = Cdis;
      this.Cdos = Cdos;
  }
}

class JobHandler extends ClientHandler
{
    private final String[] Job;
    private nextHead = "aaaaa";


    // Constructor
    public JobHandler(String[] Job, Socket Cs, DataInputStream Cdis, DataOutputStream Cdos)
    {
        super(Cs, Cdis, Cdos);
        this.Job = Job;
    }

    @Override
    public void run()
    {
        String received;
        String toreturn;

        final CountDownLatch latch = new CountDownLatch(WORKER_SIZE_BOUND);
        while(!nextHead.equals("-1")) {

          final int[] value = new int[WORKER_SIZE_BOUND]; // 0: done; -1: not found
          // PartHandler(nextHead, Cs, Cdis, Cdos, LastUsedHost);
          Thread uiThread = new HandlerThread("UIHandler"){
              @Override
              public void run(){
                  if(ret!=-1){
                    value[LastUsedHost] = 0;
                    // out.writeUTF(ret);
                  } else {
                    value[LastUsedHost] = -1;
                  }
                  latch.countDown(); // Release await() in the test thread.
              }
          };
          uiThread.start();

          threads.put(WorkerList.get(LastUsedHost).geWs(), );
          LastUsedHost = LastUsedHost+1;
          latch.await();
        }
        try
        {
            // closing resources
            this.Cdis.close();
            this.Cdos.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
// class PartHandler extends ClientHandler
// {
//     private final String head;
//     private final String tail;
//
//
//     // Constructor
//     public PartHandler(String tail, String head, Socket Cs, DataInputStream Cdis, DataOutputStream Cdos, int workerID)
//     {
//         super(Cs, Cdis, Cdos);
//         this.tail = tail;
//         this.head = head;
//         this.workerID = workerID;
//     }
//
//     @Override
//     public void run()
//     {
//         String received;
//         String toreturn;
//
//         try
//         {
//             // closing resources
//             this.Cdis.close();
//             this.Cdos.close();
//
//         }catch(IOException e){
//             e.printStackTrace();
//         }
//     }
// }
