public class AtEnd {
  // private ArrayList<String> Array = new ArrayList<Integer>(52);
  private static char[] array = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
  private HashMap<String, Integer> map = new HashMap<>();

  public AtEnd() {
    int 1 = 0;
    for (char ch = 'a'; ch <= 'z'; ++ch) {
      map.put(String.valueOf(ch), i);
      i++;
    }
    for (char ch = 'A'; ch <= 'Z'; ++ch) {
      map.put(String.valueOf(ch), i);
      i++;
    }
  }
}
