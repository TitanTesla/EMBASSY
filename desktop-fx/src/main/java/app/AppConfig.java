package app;

import db.Db;
import util.PathUtil;

public class AppConfig {
  public static final String HARD_USER = "finance";
  public static final String HARD_PASS = "1234";

  public static void ensureFoldersAndDb() {
    // The PathUtil.ensureBaseFolders() is still needed if it creates the
    // root folder where the DB goes (like ~/.embassyfx)
    // If Db.java's userDbPath() is already creating the folder, you can remove this later.
    PathUtil.ensureBaseFolders();

    // *** FIX: Call connect() instead of init() ***
    // We call connect() and immediately close it in the try-with-resources
    // block. This forces the entire initialization process to run without
    // keeping the connection open unnecessarily if it's not immediately needed.
    try (java.sql.Connection c = Db.connect()) {
      // The database is now guaranteed to be initialized.
      // We don't need to do anything else here.
    } catch (java.sql.SQLException e) {
      throw new RuntimeException("Failed to initialize database connection.", e);
    }
  }
}
