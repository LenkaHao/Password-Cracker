import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static final int PORT = 8080;
    static final int WORKER_PORT = 9000;
    static final double COMBO = Math.pow(52, 5);
    static final int GROUP_SIZE = 1000;
    static final int WORKER_SIZE_BOUND = 10;

    private ServerSocket serverSocket;
    private ServerSocket serverSocketForWorker;
    // for each partition: job done or not
    public Map<String, Boolean> partitions;
    // for each worker: thread for its connection
    public Map<Socket, Thread> threads;

    public Server() throws IOException {
        partitions = new HashMap<>();
        generateMap(partitions);
        serverSocket = new ServerSocket(PORT);
        serverSocketForWorker = new ServerSocket(WORKER_PORT);
    }

    public static void main(String[] args) throws Exception{
        Server server = new Server();

        while (true) {
            // create thread for web server
            Socket socket = server.serverSocket.accept();
            new ServerThread(socket).start();

            // create a new thread for every client
            Socket workerSocket = server.serverSocketForWorker.accept();
            ServerThreadForWorker thread = new ServerThreadForWorker(workerSocket);
            // put socket and thread into mapping
            server.threads.put(workerSocket, thread);
        }
    }

    private void generateMap(Map<String, Boolean> groups) {
        // generate mapping for calculation
        Map<Integer, Character> mapping = new HashMap<>();
        for (int i = 0; i < 26; i++) {
            mapping.put(i, (char) (i + 65));
        }
        for (int i = 26; i < 52; i++) {
            mapping.put(i, (char) (i + 71));
        }

        // convert interval/group size to base 52
        int gap = GROUP_SIZE;
        List<Integer> interval = new LinkedList<>();
        while (gap > 0) {
            interval.add(0, gap % 52);
            gap /= 52;
        }
        while (interval.size() < 5) {
            interval.add(0, 0);
        }

        // generate each string for test
        groups.put("AAAAA", false);
        int[] current = new int[] {0, 0, 0, 0, 0};
        for (int i = 0; i < Math.ceil(COMBO / GROUP_SIZE)-1; i++) {
            int[] next = nextCombo(current, interval);

            String nextStr = "";
            for (int j = 0; j < 5; j++) {
                nextStr += mapping.get(next[j]);
            }
            groups.put(nextStr, false);
            current = next;
        }
    }

    private int[] nextCombo(int[] current, List<Integer> interval) {
        int[] result = new int[5];
        int carry = 0;
        int idx = 4;
        while (idx >=0 ) {
            int digit = current[idx] + interval.get(idx) + carry;
            result[idx] = digit % 52;
            carry = digit / 52;
            idx -= 1;
        }
        return result;
    }
}


// Thread to communicate with web server
class ServerThread extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private DataOutputStream writer;

    public ServerThread(Socket socket) {
        this.socket = socket;

        // set up I/O
        try {
            InputStream input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            writer = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }



}

// Thread to communicate with workers
class ServerThreadForWorker extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private DataOutputStream writer;

    public ServerThreadForWorker(Socket socket) {
        this.socket = socket;

        // set up I/O
        try {
            InputStream input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            writer = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void run() {
        try {
            // parse input
            String input = reader.readLine();
            // TODO: read input from web server
            String[] params = input.split(" ");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
