// Java implementation of  Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

// Server class
public class Server
{
    // for loadbalancing
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
        return Wdis;
      }
      public DataOutputStream GetWdos(){
        return Wdos;
      }
    }


    private class WorkerRegisterHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private DataOutputStream writer;

        public void run() {
          // server is listening on port 8000
          ServerSocket ss = new ServerSocket(9000);
          Socket Ws = null;

          while(true){
            try
            {
                // socket object to receive incoming client requests
                Ws = ss.accept();

                System.out.println("A new client is connected : " + Ws);

                // obtaining input and out streams
                DataInputStream Wdis = new DataInputStream(Ws.getInputStream());
                DataOutputStream Wdos = new DataOutputStream(Ws.getOutputStream());

                WorkerList.add(new WorkerInfo(Ws, Wdis, Wdos));

            }
            catch (Exception e) {
                Ws.close();
                ss.close();
                e.printStackTrace();
                System.exit(-1);
            }
          }

        }
    }

    private class Dispatcher extends Thread {
      private final Socket s;
      private final DataInputStream Cdis;
      private final DataOutputStream Cdos;
      private int NextHost = 0;
      // private static int PartSize = 1000;
      private int GROUP_SIZE = 1000;
      private int WORKER_SIZE_BOUND = 10;
      private int WORKER_SIZE = 0;
      private Semaphore available = new Semaphore(WORKER_SIZE, true);
      private Semaphore waitReConfig = new Semaphore(1, true);
      private ConcurrentLinkedQueue<String> reSubmittedParts = new ConcurrentLinkedQueue<>();

      public Dispatcher(Socket s, DataInputStream Cdis, DataOutputStream Cdos) {
        this.s = s;
        this.Cdis = Cdis;
        this.Cdos = Cdos;
      }
      public void Dispatch(String s) {
        String[] arrOfStr = s.split("/", 2);
        if(arrOfStr[0].equals("r")) {

          // number of worker
          int increment = Integer.parseInt(arrOfStr[2]);
          available.release(increment);
          WORKER_SIZE = Integer.parseInt(arrOfStr[1]);

          // part size
          GROUP_SIZE = Integer.parseInt(arrOfStr[3]);

          // job submission
          Thread t = new JobHandler(arrOfStr, this.s, Cdis, Cdos);
          t.start();

        } else if(arrOfStr[0].equals("p")) {
          GROUP_SIZE = Integer.parseInt(arrOfStr[1]);
        } else if(arrOfStr[0].equals("n")) {
          if(WORKER_SIZE<Integer.parseInt(arrOfStr[1])) {
            int increment = Integer.parseInt(arrOfStr[1])-WORKER_SIZE;
            available.release(increment);
            waitReConfig.acquire();
            NextHost = Integer.parseInt(arrOfStr[1]);
            WORKER_SIZE = Integer.parseInt(arrOfStr[1]);
            waitReConfig.release();
          } else if(WORKER_SIZE>Integer.parseInt(arrOfStr[1])){
            int decrement = WORKER_SIZE-Integer.parseInt(arrOfStr[1]);
            int count = 0;
            ArrayList<Socket> removeList = new ArrayList<>(decrement);
            waitReConfig.acquire();
            for (Socket key : WorkerJobs.keySet()) {
                removeList.add(key);
                count = count + 1;
                if (count==decrement){
                  break;
                }
            }
            for(Socket Ws: removeList){
              for(Thread t: WorkerJobs.get(Ws)){
                t.interrupt();
              }
              WorkerJobs.remove(Ws);
            }
            NextHost = Integer.parseInt(arrOfStr[1]);
            WORKER_SIZE = Integer.parseInt(arrOfStr[1]);
            waitReConfig.release();
          }



          // return null;
        } else {
          // return null
        }
      }

      @Override
      public void run()
      {
        while(true){
          String received;
          try{
            received = Cdis.readUTF();
          } catch (IOException e) {
              e.printStackTrace();
          }

          Dispatch(received);
        }
      }
    }

    // ClientHandler class
    private class ClientHandler extends Thread{
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

    private class JobHandler extends ClientHandler
    {
        // each job would have WORKER_SIZE number of working parts
        // private Semaphore available = new Semaphore(WORKER_SIZE, true);
        // private Semaphore wait = new Semaphore(1, true);
        private final String[] Job;
        private String nextHead = "AAAAA";
        // private int
        private String result = "";
        private LinkedList<Thread> WorkingPartList = new LinkedList<>();
        public Map<String, Boolean> partitions;// start, working
        private Semaphore wait = new Semaphore(1, true);


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
                // loop head of partition
                while(!nextHead.equals("-1")) {
                  waitReConfig.acquire();
                  DataInputStream Win = WorkerList.get(NextHost).GetWdis();
                  DataOutputStream Wout = WorkerList.get(NextHost).GetWdos();
                  Socket Ws = WorkerList.get(NextHost).GetWs();

                  Thread PartHandler = new Thread("UIHandler"){
                      @Override
                      public void run() {
                        try{
                          String line = Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n";     // MD5/nextHead/GROUP_SIZE

                          Wout.writeUTF(line);
                          String line = Win.readLine();
                          if(line.equals("11111")) { ///!!!!!!!!!!!!!
                          } else if(line.equals("00000")){
                            reSubmittedParts.add(Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n");
                          } else {
                            result = line;
                          }
                        } catch(ClosedByInterruptException e) {
                            // System.exit(-1);
                            reSubmittedParts.add(Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n");
                            // wait.release();
                            Wout.writeUTF("QUIT\n");
                            System.out.println("I was killed!");

                        }
                        available.release();
                      }
                  };
                  PartHandler.start();
                  WorkerJobs.put(Ws, WorkerJobs.get(Ws).add(PartHandler));
                  NextHost = (NextHost+1)%WORKER_SIZE;
                  waitReConfig.release();
                  available.acquire();
                  if(!result.equals("")) {
                    Cdos.writeUTF(result);
                    for(Thread t: WorkingPartList)
                    {
                      t.interrupt();
                    }
                    break;
                  } else {
                    // !!!!!! increment nexthead
                      nexthead = getNextParition(nexthead, GROUP_SIZE);
                  }
                }

                while(reSubmittedParts.size()!=0) {
                  DataInputStream Win = WorkerList.get(NextHost).GetWdis();
                  DataOutputStream Wout = WorkerList.get(NextHost).GetWdos();
                  Socket Ws = WorkerList.get(NextHost).GetWs();
                  // String LastUsedHead;
                  // boolean normalnext1 = false;
                  // boolean normalnext2 = false;

                  Thread PartHandler = new Thread("UIHandler"){
                      @Override
                      public void run() {
                        try{
                          String line = reSubmittedParts.poll();

                          Wout.writeUTF(line);
                          String line = Win.readLine();
                          if(line.equals("11111")) {
                            // wait.acquire();
                            // normalnext = true;
                            // wait.release();
                          } else if(line.equals("00000")){
                            reSubmittedParts.add(Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n");
                            // wait.release();
                          } else {
                            result = line;
                          }
                        } catch(InterruptedException e) {
                            // System.exit(-1);
                            reSubmittedParts.add(Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n");
                            // wait.release();
                            Wout.writeUTF("QUIT\n");
                            System.out.println("I was killed!");

                        }
                        available.release();
                      }
                  };
                  PartHandler.start();

                  WorkerJobs.put(Ws, WorkerJobs.get(Ws).add(PartHandler));
                  NextHost = (NextHost+1)%WORKER_SIZE;
                  available.acquire();
                  if(!result.equals("")) {
                    Cdos.writeUTF(result);
                    for(Thread t: WorkingPartList)
                    {
                      t.interrupt();
                    }
                    break;
                  } else {
                    // !!!!!! increment nexthead
                    // nexthead

                  }
                }
                // closing resources
                this.Cdis.close();
                this.Cdos.close();

            } catch(Exception e) {
                e.printStackTrace();
                this.Cdis.close();
                this.Cdos.close();
            }
        }
    }

    public static void main(String[] args) throws IOException
    {

        Thread Wt = new WorkerRegisterHandler();
        Wt.start();

        // server is listening on port 8000
        ServerSocket ss = new ServerSocket(8000);
        Socket Cs = null;

        while(true){
          try
          {
              // socket object to receive incoming client requests
              Cs = ss.accept();

              System.out.println("A new client is connected : " + Cs);

              // obtaining input and out streams
              DataInputStream Cdis = new DataInputStream(Cs.getInputStream());
              DataOutputStream Cdos = new DataOutputStream(Cs.getOutputStream());

              System.out.println("Assigning new thread for this client");

              // create a new thread object
              Thread Ct = new Dispatcher(Cs, Cdis, Cdos);

              // Invoking the start() method
              Ct.start();

          }
          catch (Exception e) {
              Cs.close();
              ss.close();
              e.printStackTrace();
              System.exit(-1);
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
