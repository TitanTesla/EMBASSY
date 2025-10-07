package service;

import model.OutflowEntry;
import repo.OutflowRepo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OutflowService {
  private final OutflowRepo repo = new OutflowRepo();

  // New API: capture category and price; compute totalPrice
  public void logIssue(String user, String sku, String name, String unit, int qty, String category, BigDecimal price) {
    if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
    if (category == null) category = "";
    BigDecimal priceNN = (price == null ? BigDecimal.ZERO : price);

    LocalDateTime now = LocalDateTime.now();
    BigDecimal total = priceNN.multiply(BigDecimal.valueOf(qty));

    // Construct OutflowEntry with LocalDateTime, category, price, and totalPrice
    repo.add(new OutflowEntry(now, user, sku, name, unit, qty, category, priceNN, total));
  }

  // Backward compatibility overload: if caller doesn't pass category/price yet
  public void logIssue(String user, String sku, String name, String unit, int qty) {
    logIssue(user, sku, name, unit, qty, "", BigDecimal.ZERO);
  }

  public List<OutflowEntry> listAll() { return repo.listAll(); }
}
