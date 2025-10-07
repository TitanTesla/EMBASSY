package service;

import model.Category;
import model.Product;
import model.UnitType;
import repo.ProductRepo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public class InventoryService {
  private final ProductRepo repo = new ProductRepo();

  public void create(String sku, String name, BigDecimal price, int qty, UnitType unit, Category cat) {
    if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU is required");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
    if (price == null || price.signum() < 0) throw new IllegalArgumentException("Price must be >= 0");
    if (qty < 0) throw new IllegalArgumentException("Qty must be >= 0");
    if (unit == null) throw new IllegalArgumentException("Unit is required");
    if (cat == null) throw new IllegalArgumentException("Category is required");
    price = price.setScale(2, RoundingMode.HALF_UP);
    Product p = new Product(sku.trim(), name.trim(), price, qty, unit, cat, LocalDateTime.now());
    repo.upsert(p);
  }

  public void update(String sku, String name, BigDecimal price, UnitType unit, Category cat) {
    Product cur = repo.find(sku);
    if (cur == null) throw new IllegalArgumentException("SKU not found");
    if (unit == null) throw new IllegalArgumentException("Unit is required");
    if (cat == null) throw new IllegalArgumentException("Category is required");
    if (price == null || price.signum() < 0) throw new IllegalArgumentException("Price must be >= 0");
    price = price.setScale(2, RoundingMode.HALF_UP);
    Product upd = new Product(cur.sku(), name, price, cur.qty(), unit, cat, cur.addedOn());
    repo.upsert(upd);
  }

  public void delete(String sku) { repo.delete(sku); }

  public Product find(String sku) {
    return repo.find(sku);
  }

  public List<Product> list(String sort) {
    String order = switch (sort) {
      case "PRICE_ASC" -> "order by price asc";
      case "PRICE_DESC" -> "order by price desc";
      case "QTY_ASC" -> "order by qty asc";
      case "QTY_DESC" -> "order by qty desc";
      case "DATE_ASC" -> "order by added_on asc";
      case "DATE_DESC" -> "order by added_on desc";
      case "NAME_ASC" -> "order by name asc";
      case "NAME_DESC" -> "order by name desc";
      case "SKU_ASC" -> "order by sku asc";
      case "SKU_DESC" -> "order by sku desc";
      case "CATEGORY_ASC" -> "order by category asc";
      case "CATEGORY_DESC" -> "order by category desc";
      case "TOTALPRICE_ASC" -> "order by (price * qty) asc";
      case "TOTALPRICE_DESC" -> "order by (price * qty) desc";
      default -> "order by sku asc";
    };
    return repo.list(order);
  }

  public void receive(String sku, int add) {
    Product p = repo.find(sku);
    if (p == null) throw new IllegalArgumentException("SKU not found");
    if (add <= 0) throw new IllegalArgumentException("Qty to add must be > 0");
    repo.updateQty(sku, p.qty() + add);
  }

  public Product issue(String sku, int take) {
    Product p = repo.find(sku);
    if (p == null) throw new IllegalArgumentException("SKU not found");
    if (take <= 0) throw new IllegalArgumentException("Qty must be > 0");
    if (p.qty() < take) throw new IllegalArgumentException("Not enough stock");
    repo.updateQty(sku, p.qty() - take);
    return repo.find(sku);
  }
}
