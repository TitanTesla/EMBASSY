package util;

public class CsvEscaper {
  public static String escape(String s) {
    if (s == null) return "";
    boolean need = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
    String v = s.replace("\"", "\"\"");
    return need ? "\"" + v + "\"" : v;
  }
}