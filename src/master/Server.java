// Java implementation of  Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

class WorkerInfo{
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

// Server class
public class Server
{
    // for loadbalancing
    // worker hostname: thread obj

    private static ArrayList<WorkerInfo> WorkerList = new ArrayList<>();            // !!!!!!! have to init when new worker join


    public static ArrayList<WorkerInfo> getWorkerList(){
      return WorkerList;
    }


    private static class WorkerRegisterHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private DataOutputStream writer;

        public void run() {
          // server is listening on port 8000
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(9000);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                try {
                    Ws.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                try {
                    ss.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
                System.exit(-1);
            }
          }

        }
    }




    public static void main(String[] args) throws IOException
    {

        WorkerRegisterHandler Wt = new WorkerRegisterHandler();
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
              Thread Ct = new JobHandler(Cs, Cdis, Cdos);

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
class JobHandler extends ClientHandler
{
    // each job would have WORKER_SIZE number of working parts
    // private Semaphore available = new Semaphore(WORKER_SIZE, true);
    // private Semaphore wait = new Semaphore(1, true);
    private HashMap<Socket, LinkedList<Thread>> WorkerJobs = new HashMap<>();// !!!!!!! have to init when new worker join
    private final String[] Job;
    private int NextHost = 0;
    private String nextHead = "AAAAA";
    // private int
    private String result = "";
    private LinkedList<Thread> WorkingPartList = new LinkedList<>();
    // public Map<String, Boolean> partitions;// start, working
    private Semaphore wait = new Semaphore(1, true);

    private class Dispatcher extends Thread {
      public void Dispatch(String s) throws InterruptedException {
        String[] Received = s.split("/", 2);
        if(Received[0].equals("p")) {
          GROUP_SIZE = Integer.parseInt(Received[1]);
        } else if(Received[0].equals("n")) {
          if(WORKER_SIZE<Integer.parseInt(Received[1])) {
            int increment = Integer.parseInt(Received[1])-WORKER_SIZE;
            available.release(increment);
            waitReConfig.acquire();
            NextHost = Integer.parseInt(Received[1]);
            WORKER_SIZE = Integer.parseInt(Received[1]);
            waitReConfig.release();
          } else if(WORKER_SIZE>Integer.parseInt(Received[1])){
            int decrement = WORKER_SIZE-Integer.parseInt(Received[1]);
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
            NextHost = Integer.parseInt(Job[1]);
            WORKER_SIZE = Integer.parseInt(Job[1]);
            waitReConfig.release();
          }
        } else {
          System.out.println("I should never go to here.");
        }
      }

      @Override
      public void run()
      {
        while(true){
          String received = "";
          try{
            received = Cdis.readUTF();
          } catch (IOException e) {
              e.printStackTrace();
          }
            try {
                Dispatch(received);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
      }
    }

    // Constructor
    public JobHandler(Socket Cs, DataInputStream Cdis, DataOutputStream Cdos)
    {
        super(Cs, Cdis, Cdos);
        // this.Job = ;
        String received = "";
        String toreturn;
        try{
          received = Cdis.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.Job = received.split("/", 2);

        // number of worker
        int increment = Integer.parseInt(Job[2]);
        available.release(increment);
        WORKER_SIZE = Integer.parseInt(Job[1]);
        // part size
        GROUP_SIZE = Integer.parseInt(Job[3]);

        // create a new thread object
        Thread Ct = new Dispatcher();

        // Invoking the start() method
        Ct.start();
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
              DataInputStream Win = Server.getWorkerList().get(NextHost).GetWdis();
              DataOutputStream Wout = Server.getWorkerList().get(NextHost).GetWdos();
              Socket Ws = Server.getWorkerList().get(NextHost).GetWs();

              Thread PartHandler = new Thread("UIHandler"){
                  @Override
                  public void run() {
                    try{
                      if(Thread.currentThread().interrupted()){
                        throw new InterruptedException("InterruptedException!!!!!");
                      }
                      String line = Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n";     // MD5/nextHead/GROUP_SIZE
                      Wout.writeUTF(line);
                      line = Win.readLine();
                      if(line.equals("11111")) {
                      } else if(line.equals("00000")){
                        reSubmittedParts.add(Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n");
                      } else {
                        result = line;
                      }
                    } catch(InterruptedException | IOException e) { // only when blocked!!!!!!!!!!!!!!
                        // System.exit(-1);
                        reSubmittedParts.add(Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n");
                        // wait.release();
                        try {
                            Wout.writeUTF("QUIT\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        System.out.println("I was killed!");

                    }
                    available.release();
                  }
              };
              PartHandler.start();
              WorkerJobs.get(Ws).add(PartHandler);
              // WorkerJobs.put(Ws, );
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
                  nextHead = NextParition.getNextParition(nextHead, GROUP_SIZE);
              }
            }

            while(reSubmittedParts.size()!=0) {
              DataInputStream Win = Server.getWorkerList().get(NextHost).GetWdis();
              DataOutputStream Wout = Server.getWorkerList().get(NextHost).GetWdos();
              Socket Ws = Server.getWorkerList().get(NextHost).GetWs();
              // String LastUsedHead;
              // boolean normalnext1 = false;
              // boolean normalnext2 = false;

              Thread PartHandler = new Thread("UIHandler"){
                  @Override
                  public void run() {
                    try{
                      if(Thread.currentThread().interrupted()){
                        throw new InterruptedException("InterruptedException!!!!!");
                      }
                      String line = reSubmittedParts.poll();
                      Wout.writeUTF(line);
                      line = Win.readLine();
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
                    } catch(InterruptedException | IOException e) {
                        // System.exit(-1);
                        reSubmittedParts.add(Job[1]+"/"+nextHead+"/"+GROUP_SIZE+"\n");
                        // wait.release();
                        try {
                            Wout.writeUTF("QUIT\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        System.out.println("I was killed!");

                    }
                    available.release();
                  }
              };
              PartHandler.start();

              WorkerJobs.get(Ws).add(PartHandler);
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
            try {
                this.Cdis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                this.Cdos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
class ClientHandler extends Thread{
  // DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd");
  // DateFormat fortime = new SimpleDateFormat("hh:mm:ss");
  protected final DataInputStream Cdis;
  protected final DataOutputStream Cdos;
  protected final Socket Cs;
  protected int GROUP_SIZE = 1000;
  // protected int WORKER_SIZE_BOUND = 10;
  protected int WORKER_SIZE = 0;
  protected Semaphore available = new Semaphore(WORKER_SIZE, true);
  protected Semaphore waitReConfig = new Semaphore(1, true);
  protected ConcurrentLinkedQueue<String> reSubmittedParts = new ConcurrentLinkedQueue<>();

  // Constructor
  public ClientHandler(Socket Cs, DataInputStream Cdis, DataOutputStream Cdos)
  {
      this.Cs = Cs;
      this.Cdis = Cdis;
      this.Cdos = Cdos;
  }
}
class NextParition {
     // public static void main(String[] args) {
     //     System.out.println("Hello");
     //     String result1 = getNextParition("AAAAA", 10);
     //     System.out.println(result1);
     // }

     public static String getNextParition(String lastString, int groupSize) {
         Map<Integer, Character> toChar = new HashMap<>();
         for (int i = 0; i < 26; i++) {
             toChar.put(i, (char) (i + 65));
         }
         for (int i = 26; i < 52; i++) {
             toChar.put(i, (char) (i + 71));
         }

         Map<Character, Integer> toInt = new HashMap<>();
         for (int idx : toChar.keySet()) {
             toInt.put(toChar.get(idx), idx);
         }

         int[] lastIdx = new int[5];
         for (int i = 0; i < 5; i++) {
             lastIdx[i] = toInt.get(lastString.charAt(i));
         }

         List<Integer> interval = getInterval(groupSize);
         int[] nextIdx = new int[5];
         int carry = 0;
         int idx = 4;
         while (idx >=0 ) {
             int digit = lastIdx[idx] + interval.get(idx) + carry;
             nextIdx[idx] = digit % 52;
             carry = digit / 52;
             idx -= 1;
         }

         if (nextIdx[0] >= 52) {
             return "-1";
         }

         String nextString = "";
         for (int i = 0; i < 5; i++) {
             nextString += toChar.get(nextIdx[i]);
         }

         return nextString;
     }

     public static List<Integer> getInterval(int groupSize) {
         int gap = groupSize;
         List<Integer> interval = new LinkedList<>();
         while (gap > 0) {
             interval.add(0, gap % 52);
             gap /= 52;
         }
         while (interval.size() < 5) {
             interval.add(0, 0);
         }
         return interval;
     }
 }
