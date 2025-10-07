package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtil {
  public static Path baseFolder() {
    return Paths.get(System.getProperty("user.home"), "Documents", "EmbassyInventory");
  }
  public static Path exportFolder() {
    return baseFolder().resolve("exports");
  }
  public static void ensureBaseFolders() {
    try {
      Files.createDirectories(exportFolder());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  public static String dbPath() {
    return baseFolder().resolve("data.db").toString();
  }
  public static java.nio.file.Path ensureExportFolder() {
    ensureBaseFolders();
    return exportFolder();
  }
}
