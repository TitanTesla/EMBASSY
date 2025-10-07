package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.time.LocalDateTime;

public record OutflowEntry(
    LocalDateTime dateTime,
    String user,
    String sku,
    String productName,
    String unit,
    int qty,
    String category,
    BigDecimal price,
    BigDecimal totalPrice
) {
    // Convenience constructor for older call sites (without category/price/totalPrice)
    public OutflowEntry(LocalDateTime dateTime, String user, String sku, String productName, String unit, int qty) {
        this(dateTime, user, sku, productName, unit, qty, "", BigDecimal.ZERO, null);
    }

    // Convenience constructor without totalPrice (it will be derived as price * qty)
    public OutflowEntry(LocalDateTime dateTime, String user, String sku, String productName, String unit, int qty, String category, BigDecimal price) {
        this(dateTime, user, sku, productName, unit, qty, category, price, null);
    }

    // --- JavaFX bean-style getters so PropertyValueFactory("...") works ---
    public LocalDateTime getDateTime() { return dateTime; }
    public String getUser() { return user; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public String getUnit() { return unit; }
    public int getQty() { return qty; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }

    public BigDecimal getTotalPrice() { return totalPrice; }

    /** Backward-compatibility alias; prefer getTotalPrice(). */
    @Deprecated
    public BigDecimal getTotal() {
        return getTotalPrice();
    }

    // Normalize/null-safety
    public OutflowEntry {
        Objects.requireNonNull(dateTime, "dateTime");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(unit, "unit");
        if (qty < 0) qty = 0;
        if (category == null) category = "";
        if (price == null) price = BigDecimal.ZERO;
        price = price.setScale(2, RoundingMode.HALF_UP);
        if (totalPrice == null) totalPrice = price.multiply(BigDecimal.valueOf(qty));
        totalPrice = totalPrice.setScale(2, RoundingMode.HALF_UP);
    }
}
