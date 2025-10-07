package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record Product(
    String sku,
    String name,
    BigDecimal price,
    int qty,
    UnitType unit,
    Category category,
    LocalDateTime addedOn
) {
    // Compact canonical constructor for null-safety and sensible defaults
    public Product {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        Objects.requireNonNull(category, "category must not be null");
        price = (price == null) ? BigDecimal.ZERO : price;
        addedOn = (addedOn == null) ? LocalDateTime.now() : addedOn;
    }

    // JavaFX-friendly bean-style getters for PropertyValueFactory
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getQty() { return qty; }
    public UnitType getUnit() { return unit; }
    public Category getCategory() { return category; }
    public LocalDateTime getAddedOn() { return addedOn; }

    // Computed column used by the Inventory table and CSV export
    public BigDecimal getTotalPrice() {
        return (price == null) ? BigDecimal.ZERO : price.multiply(BigDecimal.valueOf(qty));
    }

    // Backward-compatible alias for any older bindings expecting "total"
    public BigDecimal getTotal() {
        return getTotalPrice();
    }
}
