package repo;

import db.Db;
import model.Category;
import model.Product;
import model.UnitType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductRepo {
  public void upsert(Product p) {
    String sql = """
      insert into products(sku,name,price,qty,unit,category,added_on)
      values(?,?,?,?,?,?,?)
      on conflict(sku) do update set
        name=excluded.name, price=excluded.price, qty=excluded.qty,
        unit=excluded.unit, category=excluded.category""";
    try (Connection c = Db.connect();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, p.sku());
      ps.setString(2, p.name());
      ps.setBigDecimal(3, p.price());
      ps.setInt(4, p.qty());
      ps.setString(5, p.unit().name());
      ps.setString(6, p.category().name());
      ps.setString(7, p.addedOn().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Product find(String sku) {
    try (Connection c = Db.connect();
         PreparedStatement ps = c.prepareStatement("select * from products where sku=?")) {
      ps.setString(1, sku);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) return null;
      return map(rs);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(String sku) {
    try (Connection c = Db.connect();
         PreparedStatement ps = c.prepareStatement("delete from products where sku=?")) {
      ps.setString(1, sku);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Product> list(String order) {
    String sql = "select * from products " + order;
    try (Connection c = Db.connect();
         Statement st = c.createStatement()) {
      ResultSet rs = st.executeQuery(sql);
      List<Product> out = new ArrayList<>();
      while (rs.next()) out.add(map(rs));
      return out;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void updateQty(String sku, int qty) {
    try (Connection c = Db.connect();
         PreparedStatement ps = c.prepareStatement("update products set qty=? where sku=?")) {
      ps.setInt(1, qty);
      ps.setString(2, sku);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private Product map(ResultSet rs) throws SQLException {
    return new Product(
        rs.getString("sku"),
        rs.getString("name"),
        rs.getBigDecimal("price") != null ? rs.getBigDecimal("price") : new java.math.BigDecimal(rs.getDouble("price")),
        rs.getInt("qty"),
        UnitType.valueOf(rs.getString("unit")),
        Category.valueOf(rs.getString("category")),
        java.time.LocalDateTime.parse(rs.getString("added_on"))
    );
  }
}
