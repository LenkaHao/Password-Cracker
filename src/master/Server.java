// Java implementation of  Server side
// It contains two classes : Server and ClientHandler
// Save file as Server.java
// TODO: let web use sinlge connection always.
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;


// Server class
public class Server {
    // for loadbalancing
    // worker hostname: thread obj

    private static ArrayList<WorkerInfo> WorkerList = new ArrayList<>();            // !!!!!!! have to init when new worker join


    public static ArrayList<WorkerInfo> getWorkerList(){
      return WorkerList;
    }


    private static class WorkerRegisterHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private OutputStream writer;

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
                System.out.println("waiting for Ws!!");
                // socket object to receive incoming client requests
                Ws = ss.accept();

                System.out.println("A new client is connected : " + Ws);

                // obtaining input and out streams
                InputStream Wdis = Ws.getInputStream();
                OutputStream Wdos = Ws.getOutputStream();

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
        System.out.println("started!!");
        WorkerRegisterHandler Wt = new WorkerRegisterHandler();
        Wt.start();
        System.out.println("WorkerRegisterHandler start!!!");

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
              InputStream Cdis = Cs.getInputStream();
              OutputStream Cdos = Cs.getOutputStream();

              System.out.println("Assigning new thread for this client");

              // create a new thread object
              Thread Ct = new JobHandler(Cs, Cdis, Cdos);

              // Invoking the start() method
              Ct.start();
              System.out.println("Done Assigning new thread for this client");

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
