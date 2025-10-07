package repo;

import db.Db;
import model.OutflowEntry;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutflowRepo {

  private static BigDecimal toBigDec(String s) {
    if (s == null) return BigDecimal.ZERO;
    String t = s.trim();
    if (t.isEmpty()) return BigDecimal.ZERO;
    try {
      return new BigDecimal(t);
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
  }

  // --- Helpers --------------------------------------------------------------

  private static LocalDateTime parseLdt(String s) {
    if (s == null || s.isBlank()) return LocalDateTime.now();
    String normalized = s.replace(' ', 'T'); // accept both ' ' and 'T'
    return LocalDateTime.parse(normalized);   // ISO-8601
  }

  /** Map UI sort keys -> SQL order-by clause (safe allowlist). */
  private static final Map<String, String> ORDER_BY = new HashMap<>();
  static {
    // Date/time
    ORDER_BY.put("DATE_DESC", "o.date_time DESC");
    ORDER_BY.put("DATE_ASC",  "o.date_time ASC");

    // User / SKU / Name / Unit / Category (text)
    ORDER_BY.put("USER_ASC",  "o.user ASC");
    ORDER_BY.put("USER_DESC", "o.user DESC");
    ORDER_BY.put("SKU_ASC",   "o.sku ASC");
    ORDER_BY.put("SKU_DESC",  "o.sku DESC");
    ORDER_BY.put("NAME_ASC",  "o.product_name ASC");
    ORDER_BY.put("NAME_DESC", "o.product_name DESC");
    ORDER_BY.put("UNIT_ASC",  "o.unit ASC");
    ORDER_BY.put("UNIT_DESC", "o.unit DESC");
    ORDER_BY.put("CATEGORY_ASC",  "o.category ASC");
    ORDER_BY.put("CATEGORY_DESC", "o.category DESC");

    // Qty (numeric)
    ORDER_BY.put("QTY_ASC",   "CAST(o.qty AS INTEGER) ASC");
    ORDER_BY.put("QTY_DESC",  "CAST(o.qty AS INTEGER) DESC");

    // Price / Total Price (numeric, persisted on outflow)
    ORDER_BY.put("PRICE_ASC",      "o.price ASC");
    ORDER_BY.put("PRICE_DESC",     "o.price DESC");
    ORDER_BY.put("TOTALPRICE_ASC", "o.total_price ASC");
    ORDER_BY.put("TOTALPRICE_DESC","o.total_price DESC");
  }

  private static String orderByFor(String sortKey) {
    if (sortKey == null) return "o.date_time DESC";
    String key = sortKey.trim().toUpperCase();
    return ORDER_BY.getOrDefault(key, "o.date_time DESC");
  }

  // --- Commands -------------------------------------------------------------

  public void add(OutflowEntry e) {
    try (Connection c = Db.connect();
         PreparedStatement ps = c.prepareStatement(
             "insert into outflow(date_time,user,sku,product_name,unit,qty,category,price,total_price) values(?,?,?,?,?,?,?,?,?)")) {

      // Persist ISO text for the timestamp for predictable round-tripping.
      ps.setString(1, e.dateTime().toString());
      ps.setString(2, e.user());
      ps.setString(3, e.sku());
      ps.setString(4, e.productName());
      ps.setString(5, e.unit());
      ps.setInt(6, e.qty());

      String cat = e.category() == null ? "" : e.category();
      ps.setString(7, cat);

      BigDecimal price = e.price() == null ? BigDecimal.ZERO : e.price();
      ps.setBigDecimal(8, price);

      BigDecimal total = e.totalPrice();
      if (total == null) total = price.multiply(BigDecimal.valueOf(e.qty()));
      ps.setBigDecimal(9, total);

      ps.executeUpdate();
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  // --- Queries --------------------------------------------------------------

  /** Default listing – newest first by date_time. */
  public List<OutflowEntry> listAll() {
    return listSorted("DATE_DESC");
  }

  /** Listing with a UI sort key (see ORDER_BY allowlist above). */
  public List<OutflowEntry> listSorted(String sortKey) {
    String orderBy = orderByFor(sortKey);

    String sql =
        "select " +
        "  o.date_time, " +
        "  o.user, " +
        "  o.sku, " +
        "  o.product_name, " +
        "  o.unit, " +
        "  o.qty, " +
        "  coalesce(o.category, '') as category, " +
        "  coalesce(o.price, 0) as price, " +
        "  coalesce(o.total_price, 0) as total_price " +
        "from outflow o " +
        "order by " + orderBy;

    try (Connection c = Db.connect();
         Statement st = c.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
      return readRows(rs);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private List<OutflowEntry> readRows(ResultSet rs) throws SQLException {
    List<OutflowEntry> out = new ArrayList<>();
    while (rs.next()) {
      LocalDateTime dt = parseLdt(rs.getString("date_time"));
      String user = rs.getString("user");
      String sku = rs.getString("sku");
      String name = rs.getString("product_name");
      String unit = rs.getString("unit");
      int qty = rs.getInt("qty");
      String category = rs.getString("category");

      BigDecimal price = toBigDec(rs.getString("price"));
      BigDecimal total = toBigDec(rs.getString("total_price"));
      if (total.compareTo(BigDecimal.ZERO) == 0) {
        total = price.multiply(BigDecimal.valueOf(qty));
      }

      out.add(new OutflowEntry(dt, user, sku, name, unit, qty, category, price, total));
    }
    return out;
  }
}
