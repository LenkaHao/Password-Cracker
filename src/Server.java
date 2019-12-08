import java.net.*;
import java.util.*;

public class Server {
    static final int PORT = 8080;
    static final double COMBO = Math.pow(52, 5);
    static final int GROUP_SIZE = 1000;
    static final int WORKER_SIZE = 10;

    public static void main(String[] args) throws Exception{
        // initiate server socket
        ServerSocket serverSocket = new ServerSocket(PORT);

        // book-keeping: start of each group of 1000 combos -> job done or not
        Map<String, Boolean> groups = new HashMap<>();
        generateMap(groups);
    }

    public static void generateMap(Map<String, Boolean> groups) {
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
            System.out.println(nextStr);
            current = next;
        }

        System.out.println(groups.size());
    }

    public static int[] nextCombo(int[] current, List<Integer> interval) {
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
