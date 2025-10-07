package db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {

  /**
   * Defines the permanent, user-writable path for the live database file.
   * This is outside the application's bundled JAR/APP.
   * @return Path to the user's database directory (~/.embassyfx/embassy.db)
   */
  private static Path userDbPath() {
    // Create the directory ~/.embassyfx/
    Path dir = Path.of(System.getProperty("user.home"), ".embassyfx");
    try { Files.createDirectories(dir); } catch (Exception ignore) {}

    // Return the full path to the database file
    return dir.resolve("embassy.db");
  }

  /**
   * On first run, checks if the live database exists at userDbPath().
   * If not, it copies the packaged seed.db resource to that location.
   */
  private static void ensureDatabaseFileExists() {
    Path target = userDbPath();

    // 1. If the live file already exists, we're done.
    if (Files.exists(target)) return;

    try (
            // 2. Look for the packaged seed.db file in resources/bootstrap/
            var in = Db.class.getResourceAsStream("/bootstrap/seed.db")
    ) {
      if (in != null) {
        // 3. If seed.db is found, copy it to the user's path.
        Files.copy(in, target);
      } else {
        // 4. Fallback: If no seed is packaged, create an empty file.
        // (This happens if you skipped creating seed.db)
        Files.createFile(target);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to create local DB at " + target, e);
    }
  }

  /**
   * Contains your existing schema creation and migration logic.
   * This ensures the tables exist and performs any necessary data migrations.
   */
  private static void initialize(Connection c) throws SQLException {
    // Run your existing init() logic here
    try (Statement st = c.createStatement()) {
      st.execute("""
                create table if not exists products(
                  sku text primary key,
                  name text not null,
                  price real not null,
                  qty integer not null,
                  unit text not null,
                  category text not null,
                  added_on text not null
                )""");
      st.execute("""
                create table if not exists outflow(
                  id integer primary key autoincrement,
                  date_time text not null,
                  user text not null,
                  sku text not null,
                  product_name text not null,
                  unit text not null,
                  qty integer not null,
                  category text not null,
                  price real not null,
                  total_price real not null
                )""");

      // Ensure columns exist for older DBs (Migration logic)
      try (Statement alter = c.createStatement()) {
        alter.execute("alter table outflow add column category text not null default ''");
      } catch (SQLException ignore) {}
      try (Statement alter = c.createStatement()) {
        alter.execute("alter table outflow add column price real not null default 0");
      } catch (SQLException ignore) {}
      try (Statement alter = c.createStatement()) {
        alter.execute("alter table outflow add column total_price real not null default 0");
      } catch (SQLException ignore) {}

      // Backfill from products where possible
      try (Statement fix = c.createStatement()) {
        fix.execute("""
                update outflow
                set category = coalesce((
                  select p.category from products p where p.sku = outflow.sku
                ), '')
                where trim(ifnull(category, '')) = ''
              """);
      }
      try (Statement fix = c.createStatement()) {
        fix.execute("""
                update outflow
                set price = coalesce((
                  select p.price from products p where p.sku = outflow.sku
                ), 0)
                where ifnull(price, 0) = 0
              """);
      }
      try (Statement fix = c.createStatement()) {
        fix.execute("""
                update outflow
                set total_price = round(price * qty, 2)
                where ifnull(total_price, 0) = 0
              """);
      }
    }
  }

  /**
   * The main connection method. It performs setup on first run, then connects.
   * This replaces the old Db.connect() method and incorporates the setup steps.
   */
  public static Connection connect() {
    // Step 1: Ensure the live DB file exists on the user's machine
    ensureDatabaseFileExists();

    // Step 2: Define the JDBC URL using the user-local path
    String url = "jdbc:sqlite:" + userDbPath().toString();

    try {
      // Step 3: Get the connection
      Connection c = DriverManager.getConnection(url);

      // Step 4: Run schema creation/migration logic (your old init())
      initialize(c);

      return c;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to connect or initialize database", e);
    }
  }

  // NOTE: Your original public static void init() is now private and called by connect().
  // You should remove the old public init() method from your project.
}