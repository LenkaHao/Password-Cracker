import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;


class JobHandler extends ClientHandler
{
    // each job would have WORKER_SIZE number of working parts
    // private Semaphore available = new Semaphore(WORKER_SIZE, true);
    // private Semaphore wait = new Semaphore(1, true);
    // TODO:  remove entry in this list
    // private HashMap<Socket, LinkedList<Thread>> WorkerJobs = new HashMap<>();// !!!!!!! have to init when new worker join
    private String[] Job;
    private int NextHost;
    private String nextHead = "AAAAA";
    // private int
    private String result = "";
    // TODO:  remove entry in this list
    private ConcurrentLinkedQueue<Integer> IdleList = new ConcurrentLinkedQueue<>();
    private Boolean keeplistening = true;
    // public Map<String, Boolean> partitions;// start, working
    // private Semaphore wait = new Semaphore(1, true);

    private class Dispatcher extends Thread {
      public void Dispatch(String s) throws InterruptedException {
        String[] Received = s.split("/");
        if(Received[0].equals("p")) {
          waitReConfig.acquire();
          GROUP_SIZE = Integer.parseInt(Received[1]);
          waitReConfig.release();
        } else if(Received[0].equals("n")) {
          if(Integer.parseInt(Received[1])>Server.getWorkerList().size()){
            return;
          }
          if(WORKER_SIZE<Integer.parseInt(Received[1])) {
            waitReConfig.acquire();
            int increment = Integer.parseInt(Received[1])-WORKER_SIZE;
            System.out.println("IdleList.size():              "+IdleList.size());
            // System.out.println(Arrays.toString(IdleList.toArray()));
            System.out.println("available.availablePermits(): "+available.availablePermits());
            for(int i = WORKER_SIZE; i < Integer.parseInt(Received[1]); i++){
              IdleList.add(i);
            }
            available.release(increment);
            System.out.println("IdleList.size():              "+IdleList.size());
            System.out.println("0 "+ Arrays.toString(IdleList.toArray()));
            System.out.println("available.availablePermits(): "+available.availablePermits());
            WORKER_SIZE = Integer.parseInt(Received[1]);
            waitReConfig.release();
          } else if(WORKER_SIZE>Integer.parseInt(Received[1])){
            int decrement = WORKER_SIZE-Integer.parseInt(Received[1]);
            waitReConfig.acquire();
            System.out.println("IdleList.size():              "+IdleList.size());
            System.out.println("available.availablePermits(): "+available.availablePermits());
            // available.acquire(WORKER_SIZE);
            for(int i = WORKER_SIZE-1 ; i > Integer.parseInt(Received[1])-1; i--){
              PrintWriter PWdos = new PrintWriter(Server.getWorkerList().get(i).GetWdos(), true);
              PWdos.write("QUIT\n");
              PWdos.flush();
              IdleList.remove(i);
            }
            System.out.println("IdleList.size():              "+IdleList.size());
            System.out.println("4 "+Arrays.toString(IdleList.toArray()));
            System.out.println("available.availablePermits(): "+available.availablePermits());
            available.acquire(decrement);
            WORKER_SIZE = Integer.parseInt(Received[1]);
            waitReConfig.release();
          }
        } else {
          // System.out.println("I should never go to here.");
        }
      }

      @Override
      public void run()
      {
        while(keeplistening){
          String received = "";
          try{
            BufferedReader BCdis = new BufferedReader(new InputStreamReader(Cdis));
            received = BCdis.readLine();
            System.out.println("job got from client:                              "+received);
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
    public JobHandler(Socket Cs, InputStream Cdis, OutputStream Cdos)
    {
        super(Cs, Cdis, Cdos);
    }

    @Override
    public void run()
    {
      String received = "";
      String toreturn;
      try{
        BufferedReader BCdis = new BufferedReader(new InputStreamReader(Cdis));
        received = BCdis.readLine();
        // System.out.println("job got from client:                              "+received);
      } catch (IOException e) {
          e.printStackTrace();
      }

      this.Job = received.split("/");
      // number of worker
      int increment = Integer.parseInt(Job[2]);
      available.release(increment);
      for(int i = 0; i < increment; i++){
        IdleList.add(i);
      }
      System.out.println("1 "+Arrays.toString(IdleList.toArray()));
      // System.out.println("IdleList.size():              "+IdleList.size());
      // System.out.println("available.availablePermits(): "+available.availablePermits());
      WORKER_SIZE = Integer.parseInt(Job[2]);
      // System.out.println("WORKER_SIZE:                                        "+WORKER_SIZE);
      // part size
      GROUP_SIZE = Integer.parseInt(Job[3]);
      // System.out.println("GROUP_SIZE:                                         "+GROUP_SIZE);

      // create a new thread object
      Thread Ct = new Dispatcher();

      // Invoking the start() method
      Ct.start();


        try
        {
        //     // loop head of partition
            while(!nextHead.equals("-1")) {
              waitReConfig.acquire();
              available.acquire();
              if(!result.equals("")) {
                PrintWriter PCdos = new PrintWriter(Cdos, true);
                PCdos.write(result);
                PCdos.flush();
                for(int i = 0 ; i <= WORKER_SIZE-1; i++){
                  PrintWriter PWdos = new PrintWriter(Server.getWorkerList().get(i).GetWdos(), true);
                  PWdos.write("QUIT\n");
                  PWdos.flush();
                }
                return;
              }
              
              NextHost = IdleList.poll();
              System.out.println("1NextHost :                 "+NextHost);
              // System.out.println("IdleList.size():              "+IdleList.size());
              // System.out.println("available.availablePermits(): "+available.availablePermits());

              InputStream Win = Server.getWorkerList().get(NextHost).GetWdis();
              OutputStream Wout = Server.getWorkerList().get(NextHost).GetWdos();
              Socket Ws = Server.getWorkerList().get(NextHost).GetWs();
              Thread PartHandler = new Thread("UIHandler"){
                  private int thisHost = NextHost;
                  private boolean quitted = false;
                  @Override
                  public void run() {

                    try{

                      String line = Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n";     // MD5/nextHead/GROUP_SIZE
                      PrintWriter PWout = new PrintWriter(Wout, true);
                      PWout.write(line);
                      PWout.flush();
                      // System.out.println("req to worker:                        "+line);
                      BufferedReader BWin = new BufferedReader(new InputStreamReader(Win));
                      line = BWin.readLine();
                      System.out.println("data received: " + line);
                      if(line.equals("1")) { // to next
                        // System.out.println("rep from worker:                    "+line);
                      } else if(line.equals("-1")){  // failed
                        // System.out.println("rep from worker:                    "+line);
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        System.out.println("reSubmittedParts:                   "+Arrays.toString(reSubmittedParts.toArray()));
                      } else if(line.equals("EXIT")){
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        System.out.println("reSubmittedParts:                   "+Arrays.toString(reSubmittedParts.toArray()));
                        quitted = true;
                      } else if(line.equals("0")){
                        throw new Exception("my format was wrong!!!");
                      } else {  // done!
                        result = line;
                        // System.out.println("                                    done");
                      }
                    } catch( Exception e) { // only when blocked!!!!!!!!!!!!!!
                      e.printStackTrace();
                    }
                    if(!quitted) {
                      IdleList.add(thisHost);
                      System.out.println("thisHost :                 "+thisHost);
                      System.out.println("2 "+Arrays.toString(IdleList.toArray()));
                      // System.out.println("2 IdleList");
                    }
                    available.release();
                    // System.out.println("IdleList.size():              "+IdleList.size());
                    // System.out.println("available.availablePermits(): "+available.availablePermits());
                  }
              };
              PartHandler.start();
              nextHead = NextPartition.getNextPartition(nextHead, GROUP_SIZE);
              System.out.println("nextHead:                   "+nextHead);
              waitReConfig.release();
            }

            while(reSubmittedParts.size()!=0) {
              waitReConfig.acquire();
              available.acquire();
              NextHost = IdleList.poll();
              System.out.println("2NextHost :                 "+NextHost);
              // System.out.println("IdleList.size():              "+IdleList.size());
              // System.out.println("available.availablePermits(): "+available.availablePermits());
              if(!result.equals("")) {
                PrintWriter PCdos = new PrintWriter(Cdos, true);
                PCdos.write(result);
                PCdos.flush();
                for(int i = 0 ; i <= WORKER_SIZE-1; i++){
                  PrintWriter PWdos = new PrintWriter(Server.getWorkerList().get(i).GetWdos(), true);
                  PWdos.write("QUIT\n");
                  PWdos.flush();
                }
                break;
              }
              InputStream Win = Server.getWorkerList().get(NextHost).GetWdis();
              OutputStream Wout = Server.getWorkerList().get(NextHost).GetWdos();
              Socket Ws = Server.getWorkerList().get(NextHost).GetWs();

              Thread PartHandler = new Thread("UIHandler"){
                private int thisHost = NextHost;
                private boolean quitted = false;
                  @Override
                  public void run() {
                    try{
                      String line = reSubmittedParts.poll();
                      PrintWriter PWout = new PrintWriter(Wout, true);
                      PWout.write(line);
                      PWout.flush();
                      BufferedReader BWin = new BufferedReader(new InputStreamReader(Win));
                      line = BWin.readLine();
                      if(line.equals("1")) { // to next
                        // System.out.println("rep from worker:                    "+line);
                      } else if(line.equals("-1")){  // failed
                        // System.out.println("rep from worker:                    "+line);
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        System.out.println("reSubmittedParts:                   "+Arrays.toString(reSubmittedParts.toArray()));
                      } else if(line.equals("EXIT")){
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        System.out.println("reSubmittedParts:                   "+Arrays.toString(reSubmittedParts.toArray()));
                        quitted = true;
                      } else if(line.equals("0")){
                        throw new Exception("my format was wrong!!!");
                      } else {  // done!
                        result = line;
                        // System.out.println("                                    done");
                      }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    if(!quitted) {
                      IdleList.add(thisHost);
                      System.out.println("thisHost :                 "+thisHost);
                      System.out.println("2 "+Arrays.toString(IdleList.toArray()));
                      // System.out.println("2 IdleList");
                    }
                    // System.out.println("3 IdleList");
                    available.release();
                    // System.out.println("IdleList.size():              "+IdleList.size());
                    // System.out.println("available.availablePermits(): "+available.availablePermits());
                  }
              };
              PartHandler.start();
              waitReConfig.release();
            }

        } catch(Exception e) {
            e.printStackTrace();
            keeplistening = false;
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
