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
    private HashMap<Socket, LinkedList<Thread>> WorkerJobs = new HashMap<>();// !!!!!!! have to init when new worker join
    private ArrayList<WorkerInfo> WorkerList = new ArrayList<>();            // !!!!!!! have to init when new worker join
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
            Socket Cs = null;

            try
            {
                // socket object to receive incoming client requests
                Cs = ss.accept();

                System.out.println("A new client is connected : " + s);

                // obtaining input and out streams
                DataInputStream Cdis = new DataInputStream(Cs.getInputStream());
                DataOutputStream Cdos = new DataOutputStream(Cs.getOutputStream());

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


    private static class Dispatcher extends Thread{
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
          GROUP_SIZE = Integer.parseInt(arrOfStr[1]);
          // return null;
        } else if(arrOfStr[0].equals("n")) {
          LastUsedHost = 0;
          // int NewSize = Integer.parseInt(arrOfStr[1]);
          // if(WORKER_SIZE_BOUND>NewSize) {
          //
          // }
          WORKER_SIZE_BOUND = Integer.parseInt(arrOfStr[1]);

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
    private static class ClientHandler extends Thread{
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

    private static  class JobHandler extends ClientHandler
    {
        private Semaphore available = new Semaphore(WORKER_SIZE_BOUND, true);
        private Semaphore wait = new Semaphore(1, true);
        private final String[] Job;
        private nextHead = "aaaaa";
        private String result = "";
        private LinkedList<Thread> WorkingPartList = new LinkedList<>();


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

            try
            {
                // final CountDownLatch latch = new CountDownLatch(WORKER_SIZE_BOUND);
                while(!nextHead.equals("-1")) {

                  final int[] value = new int[WORKER_SIZE_BOUND]; // 0: done; -1: not found
                  DataInputStream Win = WorkerList.get(LastUsedHost).GetWdis();
                  DataOutputStream Wout = WorkerList.get(LastUsedHost).GetWdos();

                  Thread PartHandler = new Thread("UIHandler"){
                      @Override
                      public void run() {
                        try{
                          String line = Job[1]+"/"+nextHead+"/"+GROUP_SIZE;     // MD5/nextHead/GROUP_SIZE
                          Wout.writeUTF(line);
                          String line = Win.readLine();
                          if(line;.equals("-1")) {
                            available.release();
                            return;
                          } else {
                            result = line;
                            available.release();
                            return;
                          }
                        } catch(InterruptedException e) {
                            System.exit(-1);
                            System.out.println("I was killed!");
                        }
                      }
                  };
                  PartHandler.start();
                  WorkerJobs.put(Ws, WorkerJobs.get(Ws).add(PartHandler));
                  LastUsedHost = LastUsedHost+1;
                  available.acquire();
                  if(!result.equals("")) {
                    Cdos.writeUTF(result);
                    for(Thread t: WorkingPartList)
                    {
                      t.interupt();
                    }
                    break;
                  }
                }
                // closing resources
                this.Cdis.close();
                this.Cdos.close();

            }catch(IOException e){
                e.printStackTrace();
                this.Cdis.close();
                this.Cdos.close();
            }
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
