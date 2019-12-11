import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

class NextPartition {
     // public static void main(String[] args) {
     //     System.out.println("Hello");
     //     String result1 = getNextParition("AAAAA", 10);
     //     System.out.println(result1);
     // }

     public static String getNextPartition(String lastString, int groupSize) {
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
