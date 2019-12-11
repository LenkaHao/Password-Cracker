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
    private HashMap<Socket, LinkedList<Thread>> WorkerJobs = new HashMap<>();// !!!!!!! have to init when new worker join
    private String[] Job;
    private int NextHost = 0;
    private String nextHead = "AAAAA";
    // private int
    private String result = "";
    // TODO:  remove entry in this list
    private ConcurrentLinkedQueue<Thread> WorkingPartList = new ConcurrentLinkedQueue<>();
    // public Map<String, Boolean> partitions;// start, working
    private Semaphore wait = new Semaphore(1, true);

    private class Dispatcher extends Thread {
      public void Dispatch(String s) throws InterruptedException {
        String[] Received = s.split("/");
        if(Received[0].equals("p")) {
          waitReConfig.acquire();
          GROUP_SIZE = Integer.parseInt(Received[1]);
          waitReConfig.release();
          // PrintWriter PCdos = new PrintWriter(Cdos, true);
          // PCdos.write("OK");
          // PCdos.flush();
        } else if(Received[0].equals("n")) {
          if(WORKER_SIZE<Integer.parseInt(Received[1])) {
            int increment = Integer.parseInt(Received[1])-WORKER_SIZE;
            available.release(increment);
            waitReConfig.acquire();
            NextHost = Integer.parseInt(Received[1])-1;
            System.out.println("NextHost:                         "+NextHost);
            WORKER_SIZE = Integer.parseInt(Received[1]);
            // PrintWriter PCdos = new PrintWriter(Cdos, true);
            // PCdos.write("OK");
            // PCdos.flush();
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
              // wait.acquire();
              // WorkerJobs.put(Ws);
              WorkerJobs.put(Ws, new LinkedList<Thread>());
              // wait.release();
            }
            NextHost = Integer.parseInt(Received[1])-1;
            System.out.println("NextHost:                         "+NextHost);
            WORKER_SIZE = Integer.parseInt(Received[1]);
            // PrintWriter PCdos = new PrintWriter(Cdos, true);
            // PCdos.write("OK");
            // PCdos.flush();
            waitReConfig.release();
          }
        } else {
          System.out.println("I should never go to here.");
        }
      }

      @Override
      public void run()
      {
        // System.out.println("job got from client:                              "+received);
        while(true){
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
        for(WorkerInfo WI: Server.getWorkerList()){
          WorkerJobs.put(WI.GetWs(), new LinkedList<Thread>());
        }
    }

    @Override
    public void run()
    {
      // this.Job = ;
      String received = "";
      String toreturn;
      try{
        BufferedReader BCdis = new BufferedReader(new InputStreamReader(Cdis));
        received = BCdis.readLine();
        System.out.println("job got from client:                              "+received);
      } catch (IOException e) {
          e.printStackTrace();
      }

      this.Job = received.split("/");
      // System.out.println("job got from client:                              "+this.Job);
      // System.out.println("job got from client:                              "+Job);
      // number of worker
      int increment = Integer.parseInt(Job[2]);
      available.release(increment);
      System.out.println("available node size:                                "+available.availablePermits());
      WORKER_SIZE = Integer.parseInt(Job[2]);
      System.out.println("WORKER_SIZE:                                        "+WORKER_SIZE);
      // part size
      GROUP_SIZE = Integer.parseInt(Job[3]);
      System.out.println("GROUP_SIZE:                                         "+GROUP_SIZE);

      // create a new thread object
      Thread Ct = new Dispatcher();

      // Invoking the start() method
      Ct.start();


        try
        {
            // loop head of partition
            while(!nextHead.equals("-1")) {
              waitReConfig.acquire();
              available.acquire();
              if(!result.equals("")) {
                PrintWriter PCdos = new PrintWriter(Cdos, true);
                PCdos.write(result);
                PCdos.flush();
                for(Thread t: WorkingPartList)
                {
                  t.interrupt();
                }
                break;
              }
              InputStream Win = Server.getWorkerList().get(NextHost).GetWdis();
              OutputStream Wout = Server.getWorkerList().get(NextHost).GetWdos();
              Socket Ws = Server.getWorkerList().get(NextHost).GetWs();
              Thread PartHandler = new Thread("UIHandler"){
                  @Override
                  public void run() {
                    try{
                      if(Thread.currentThread().interrupted()){
                        throw new InterruptedException("InterruptedException!!!!!");
                      }
                      String line = Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n";     // MD5/nextHead/GROUP_SIZE
                      PrintWriter PWout = new PrintWriter(Wout, true);
                      PWout.write(line);
                      PWout.flush();
                      System.out.println("req to worker:                        "+line);
                      BufferedReader BWin = new BufferedReader(new InputStreamReader(Win));
                      line = BWin.readLine();
                      if(line.equals("11111")) {
                        System.out.println("rep from worker:                    "+line);
                      } else if(line.equals("00000")){
                        System.out.println("rep from worker:                    "+line);
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        System.out.println("reSubmittedParts:                   "+reSubmittedParts.peek());
                      } else {
                        result = line;
                        System.out.println("                                    done");
                      }
                    } catch(InterruptedException | IOException e) { // only when blocked!!!!!!!!!!!!!!
                        // System.exit(-1);
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        System.out.println("reSubmittedParts:                   "+reSubmittedParts.peek());
                        // wait.release();
                        // try {
                          PrintWriter PWout = new PrintWriter(Wout, true);
                          PWout.write("QUIT\n");
                          PWout.flush();
                        // } catch (IOException ex) {
                        //     ex.printStackTrace();
                        // }
                        System.out.println("I was killed!");

                    }
                    available.release();
                    WorkingPartList.remove(Thread.currentThread());
                    System.out.println("available node size:                 "+available.availablePermits());
                  }
              };
              PartHandler.start();
              WorkingPartList.add(PartHandler);
              // wait.acquire();
              WorkerJobs.get(Ws).add(PartHandler);
              // wait.release();
              // WorkerJobs.put(Ws, );
              NextHost = (NextHost+1)%WORKER_SIZE;
              System.out.println("NextHost:                         "+NextHost);
              nextHead = NextParition.getNextParition(nextHead, GROUP_SIZE);
              System.out.println("nextHead:                   "+nextHead);
              waitReConfig.release();
            }

            while(reSubmittedParts.size()!=0) {
              waitReConfig.acquire();
              available.acquire();
              if(!result.equals("")) {
                PrintWriter PCdos = new PrintWriter(Cdos, true);
                PCdos.write(result);
                PCdos.flush();
                for(Thread t: WorkingPartList)
                {
                  t.interrupt();
                }
                break;
              }
              InputStream Win = Server.getWorkerList().get(NextHost).GetWdis();
              OutputStream Wout = Server.getWorkerList().get(NextHost).GetWdos();
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
                      PrintWriter PWout = new PrintWriter(Wout, true);
                      PWout.write(line);
                      PWout.flush();
                      BufferedReader BWin = new BufferedReader(new InputStreamReader(Win));
                      line = BWin.readLine();
                      if(line.equals("11111")) {
                        // wait.acquire();
                        // normalnext = true;
                        // wait.release();
                      } else if(line.equals("00000")){
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        // wait.release();
                      } else {
                        result = line;
                      }
                    } catch(InterruptedException | IOException e) {
                        // System.exit(-1);
                        reSubmittedParts.add(Job[1]+" "+nextHead+" "+GROUP_SIZE+"\n");
                        // wait.release();
                        // try {
                          PrintWriter PWout = new PrintWriter(Wout, true);
                          PWout.write("QUIT\n");
                          PWout.flush();
                        // } catch (IOException ex) {
                        //     ex.printStackTrace();
                        // }
                        System.out.println("I was killed!");

                    }
                    available.release();
                    WorkingPartList.remove(Thread.currentThread());
                  }
              };
              PartHandler.start();
              WorkingPartList.add(PartHandler);
              // wait.acquire();
              WorkerJobs.get(Ws).add(PartHandler);
              // wait.acquire();
              NextHost = (NextHost+1)%WORKER_SIZE;
              System.out.println("NextHost:                         "+NextHost);

              waitReConfig.release();
              // nextHead = NextParition.getNextParition(nextHead, GROUP_SIZE);
              // System.out.println("nextHead:                   "+nextHead);
            }
            // closing resources
            // this.Cdis.close();
            // this.Cdos.close();

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
