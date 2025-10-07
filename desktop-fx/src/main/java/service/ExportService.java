package service;

import model.OutflowEntry;
import model.Product;
import util.CsvEscaper;
import util.NumberUtil;
import util.PathUtil;
import util.TimeUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class ExportService {
  private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  // Format helpers that work whether the model exposes LocalDateTime or String
  private static String fmt(LocalDateTime dt) {
    return dt == null ? "" : DT_FMT.format(dt);
  }
  private static String fmt(String s) {
    return s == null ? "" : s; // already a string; assume correctly formatted
  }

  public Path exportInventory(java.util.List<Product> products, String suffix) {
    try {
      Path folder = PathUtil.ensureExportFolder();
      String name = "inventory_" + suffix + "_" + TimeUtil.timestamp() + ".csv";
      Path target = folder.resolve(name);
      List<List<String>> rows = new ArrayList<>();
      rows.add(List.of("SKU","Name","Qty","Price","Total Price","Unit","Category","AddedOn"));
      for (Product p : products) {
        rows.add(List.of(
            p.sku(),
            p.name(),
            String.valueOf(p.qty()),
            NumberUtil.price(p.price()),
            NumberUtil.price((p.price() == null ? BigDecimal.ZERO : p.price()).multiply(BigDecimal.valueOf(p.qty()))),
            p.unit().name(),
            p.category().name(),
            fmt(p.addedOn())
        ));
      }
      writeCsv(target, rows);
      return target;
    } catch (IOException e) { throw new RuntimeException(e); }
  }

  public Path exportOutflow(java.util.List<OutflowEntry> entries, String suffix) {
    try {
      Path folder = PathUtil.ensureExportFolder();
      String name = "outflow_" + (suffix == null || suffix.isBlank() ? "" : suffix + "_") + TimeUtil.timestamp() + ".csv";
      Path target = folder.resolve(name);
      List<List<String>> rows = new ArrayList<>();
      rows.add(List.of("DateTime","User","SKU","Product","Category","Unit","Qty","Price","Total Price"));
      for (OutflowEntry e : entries) {
        // Resolve price (prefer explicit price; if absent, try derive from totalPrice/qty)
        BigDecimal price = e.price();
        if (price == null) {
          BigDecimal derived = null;
          try {
            java.lang.reflect.Method m = e.getClass().getMethod("totalPrice");
            Object v = m.invoke(e);
            if (v instanceof BigDecimal && e.qty() > 0) {
              derived = ((BigDecimal) v).divide(BigDecimal.valueOf(e.qty()), 2, java.math.RoundingMode.HALF_UP);
            }
          } catch (Exception ignore) { /* accessor not present or not usable */ }
          price = derived == null ? BigDecimal.ZERO : derived;
        }

        // Resolve totalPrice (prefer explicit accessor; otherwise compute qty * price)
        BigDecimal total = null;
        try {
          java.lang.reflect.Method m2 = e.getClass().getMethod("totalPrice");
          Object v2 = m2.invoke(e);
          if (v2 instanceof BigDecimal) {
            total = (BigDecimal) v2;
          }
        } catch (Exception ignore) { /* accessor not present or not usable */ }
        if (total == null) {
          total = (price == null ? BigDecimal.ZERO : price).multiply(BigDecimal.valueOf(e.qty()));
        }

        // Category fallback
        String cat = e.category();
        if (cat == null) cat = "";

        rows.add(List.of(
            fmt(e.dateTime()),
            e.user(),
            e.sku(),
            e.productName(),
            cat,
            e.unit(),
            String.valueOf(e.qty()),
            NumberUtil.price(price),
            NumberUtil.price(total)
        ));
      }
      writeCsv(target, rows);
      return target;
    } catch (IOException e) { throw new RuntimeException(e); }
  }

  private void writeCsv(Path target, java.util.List<java.util.List<String>> rows) throws IOException {
    Path tmp = Paths.get(target.toString() + ".tmp");
    try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
      for (java.util.List<String> r : rows) {
        boolean first = true;
        for (String v : r) {
          if (!first) w.write(",");
          w.write(CsvEscaper.escape(v));
          first = false;
        }
        w.write(System.lineSeparator());
      }
    }
    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }
}
