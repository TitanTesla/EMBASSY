package ui;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Category;
import model.OutflowEntry;
import model.Product;
import model.UnitType;
import service.ExportService;
import service.InventoryService;
import service.OutflowService;
import javafx.scene.layout.GridPane;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class MainController {
  @FXML private TableView<Product> inventoryTable;
  @FXML private TableColumn<Product, String> colSku;
  @FXML private TableColumn<Product, String> colName;
  @FXML private TableColumn<Product, String> colPrice;
  @FXML private TableColumn<Product, Integer> colQty;
  @FXML private TableColumn<Product, String> colUnit;
  @FXML private TableColumn<Product, String> colCat;
  @FXML private TableColumn<Product, String> colCreated;
  @FXML private TableColumn<Product, String> colTotalPrice;

  @FXML private TableView<OutflowEntry> outflowTable;
  @FXML private TableColumn<OutflowEntry, String> ofDate, ofUser, ofSku, ofName, ofUnit;
  @FXML private TableColumn<OutflowEntry, Integer> ofQty;
  @FXML private TableColumn<OutflowEntry, String> ofCat;
  @FXML private TableColumn<OutflowEntry, BigDecimal> ofPrice;
  @FXML private TableColumn<OutflowEntry, BigDecimal> ofTotalPrice;

  @FXML private Label exportMsg;

  // UI controls from FXML (needed for sorting, search and analytics text)
  @FXML private ChoiceBox<String> sortChoice;
  @FXML private TextField searchField;
  @FXML private Label analyticsLabel;

  private final InventoryService inv = new InventoryService();
  private final OutflowService out = new OutflowService();
  private final ExportService exp = new ExportService();

  private FilteredList<Product> filtered;
  private static final DateTimeFormatter OUTFLOW_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
  private boolean searchBound = false;
  private static String fmtMoney(BigDecimal v) { return v == null ? "" : MONEY.format(v); }

  private static String toUiDate(Object dt) {
    if (dt == null) return "";
    if (dt instanceof LocalDateTime ldt) {
      return OUTFLOW_DTF.format(ldt);
    }
    try {
      return OUTFLOW_DTF.format(LocalDateTime.parse(dt.toString()));
    } catch (DateTimeParseException e) {
      return dt.toString();
    }
  }

  private void applyFilter(String newText) {
    if (filtered == null) return;
    String q = newText == null ? "" : newText.toLowerCase();
    filtered.setPredicate(p -> p != null &&
        (p.sku().toLowerCase().contains(q) || p.name().toLowerCase().contains(q)));
  }

  @FXML
  public void initialize() {
    colSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sku()));
    colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
    colPrice.setCellValueFactory(p -> new SimpleStringProperty(fmtMoney(p.getValue().price())));
    colQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().qty()).asObject());
    colUnit.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().unit().name()));
    colCat.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().category().name()));

    // New inventory columns
    if (colCreated != null) {
      colCreated.setCellValueFactory(c ->
          new SimpleStringProperty(toUiDate(c.getValue().addedOn()))
      );
    }
    if (colTotalPrice != null) {
      colTotalPrice.setCellValueFactory(c -> {
        Product p = c.getValue();
        BigDecimal total = p.price().multiply(BigDecimal.valueOf(p.qty()));
        return new SimpleStringProperty(fmtMoney(total));
      });
    }

    ofDate.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue() == null ? "" : toUiDate(c.getValue().dateTime())
    ));

    ofUser.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue() == null || c.getValue().user() == null ? "" : c.getValue().user()));

    ofSku.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue() == null || c.getValue().sku() == null ? "" : c.getValue().sku()));

    ofName.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue() == null || c.getValue().productName() == null ? "" : c.getValue().productName()));

    ofUnit.setCellValueFactory(c -> new SimpleStringProperty(
        c.getValue() == null || c.getValue().unit() == null ? "" : String.valueOf(c.getValue().unit())));

    ofQty.setCellValueFactory(c -> new SimpleIntegerProperty(
        c.getValue() == null ? 0 : c.getValue().qty()).asObject());

    // Outflow columns sourced from persisted entry values
    if (ofCat != null) {
      ofCat.setCellValueFactory(c -> {
        OutflowEntry e = c.getValue();
        String cat = "";
        if (e != null && e.category() != null) cat = String.valueOf(e.category());
        return new SimpleStringProperty(cat);
      });
    }
    if (ofPrice != null) {
      ofPrice.setCellValueFactory(c -> {
        OutflowEntry e = c.getValue();
        BigDecimal val = (e == null) ? null : e.price();
        return new ReadOnlyObjectWrapper<>(val);
      });
      ofPrice.setCellFactory(col -> new TableCell<OutflowEntry, BigDecimal>() {
        @Override protected void updateItem(BigDecimal item, boolean empty) {
          super.updateItem(item, empty);
          setText(empty ? null : fmtMoney(item));
        }
      });
      ofPrice.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
    }
    if (ofTotalPrice != null) {
      ofTotalPrice.setCellValueFactory(c -> {
        OutflowEntry e = c.getValue();
        BigDecimal total = null;
        if (e != null) {
          total = e.totalPrice();
          if (total == null) {
            BigDecimal p = e.price();
            if (p != null) total = p.multiply(BigDecimal.valueOf(e.qty()));
          }
        }
        return new ReadOnlyObjectWrapper<>(total);
      });
      ofTotalPrice.setCellFactory(col -> new TableCell<OutflowEntry, BigDecimal>() {
        @Override protected void updateItem(BigDecimal item, boolean empty) {
          super.updateItem(item, empty);
          setText(empty ? null : fmtMoney(item));
        }
      });
      ofTotalPrice.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
    }

    sortChoice.setItems(FXCollections.observableArrayList("SKU","PRICE_ASC","PRICE_DESC","QTY_ASC","QTY_DESC","TOTAL_PRICE_ASC","TOTAL_PRICE_DESC","DATE_NEWEST","DATE_OLDEST"));
    sortChoice.setValue("SKU");
    sortChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> refreshInventory());
    if (!searchBound) {
      searchField.textProperty().addListener((obs, __, txt) -> applyFilter(txt));
      searchBound = true;
    }

    refreshInventory();
    refreshOutflow();
  }

  // Helper: compute total price for a product (null-safe) used by comparators
  private static BigDecimal calcTotal(Product p) {
    if (p == null || p.price() == null) return null;
    return p.price().multiply(BigDecimal.valueOf(p.qty()));
  }

  private List<Product> getInventorySorted(String sortKey) {
    // start from a stable base (by SKU) and sort in-memory for all keys
    List<Product> base = inv.list("SKU");

    Comparator<Product> cmp;
    switch (sortKey) {
      case "PRICE_ASC":
        cmp = Comparator.comparing(Product::price, Comparator.nullsLast(Comparator.naturalOrder()));
        break;
      case "PRICE_DESC":
        cmp = Comparator.comparing(Product::price, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        break;
      case "QTY_ASC":
        cmp = Comparator.comparingInt(Product::qty);
        break;
      case "QTY_DESC":
        cmp = Comparator.comparingInt(Product::qty).reversed();
        break;
      case "DATE_NEWEST":
        cmp = Comparator.comparing(Product::addedOn, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        break;
      case "DATE_OLDEST":
        cmp = Comparator.comparing(Product::addedOn, Comparator.nullsLast(Comparator.naturalOrder()));
        break;
      case "TOTAL_PRICE_ASC":
        cmp = Comparator.comparing(
            MainController::calcTotal,
            Comparator.nullsLast(Comparator.naturalOrder())
        );
        break;
      case "TOTAL_PRICE_DESC":
        cmp = Comparator.comparing(
            MainController::calcTotal,
            Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed();
        break;
      case "SKU":
      default:
        cmp = Comparator.comparing(Product::sku, String.CASE_INSENSITIVE_ORDER);
    }

    return base.stream().sorted(cmp).toList();
  }

  @FXML
  public void refreshInventory() {
    String sortKey = sortChoice.getValue();
    List<Product> list = getInventorySorted(sortKey);
    filtered = new FilteredList<>(FXCollections.observableList(list), p -> p != null);
    inventoryTable.setItems(filtered);
    applyFilter(searchField.getText());
    int totalQty = list.stream().mapToInt(Product::qty).sum();
    analyticsLabel.setText("Items: " + list.size() + "  Qty total: " + totalQty);
  }

  @FXML
  public void refreshOutflow() {
    outflowTable.setItems(FXCollections.observableArrayList(out.listAll()));
  }

  @FXML
  public void addProduct() {
    Dialog<Product> dlg = productDialog(null);
    Optional<Product> res = dlg.showAndWait();
    res.ifPresent(p -> {
      inv.create(p.sku(), p.name(), p.price(), p.qty(), p.unit(), p.category());
      refreshInventory();
    });
  }

  @FXML
  public void updateProduct() {
    Product sel = inventoryTable.getSelectionModel().getSelectedItem();
    if (sel == null) { alert("Pick a product first"); return; }
    Dialog<Product> dlg = productDialog(sel);
    Optional<Product> res = dlg.showAndWait();
    res.ifPresent(p -> {
      inv.update(sel.sku(), p.name(), p.price(), p.unit(), p.category());
      refreshInventory();
    });
  }

  @FXML
  public void deleteProduct() {
    Product sel = inventoryTable.getSelectionModel().getSelectedItem();
    if (sel == null) { alert("Pick a product first"); return; }
    if (confirm("Delete " + sel.name() + " ?")) {
      inv.delete(sel.sku());
      refreshInventory();
    }
  }

  @FXML
  public void receiveStock() {
    TextInputDialog d = new TextInputDialog("1");
    Window owner = inventoryTable.getScene() != null ? inventoryTable.getScene().getWindow() : null;
    if (owner != null) d.initOwner(owner);
    d.initModality(Modality.WINDOW_MODAL);
    d.setResizable(false);
    Product sel = inventoryTable.getSelectionModel().getSelectedItem();
    if (sel == null) { alert("Pick a product first"); return; }
    d.setHeaderText("Receive qty for " + sel.sku());
    d.setContentText("Qty");
    d.showAndWait().ifPresent(q -> {
      try {
        int add = Integer.parseInt(q);
        inv.receive(sel.sku(), add);
        refreshInventory();
      } catch (Exception e) { alert(e.getMessage()); }
    });
  }

  @FXML
  public void issueStock() {
    TextInputDialog d = new TextInputDialog("1");
    Window owner = inventoryTable.getScene() != null ? inventoryTable.getScene().getWindow() : null;
    if (owner != null) d.initOwner(owner);
    d.initModality(Modality.WINDOW_MODAL);
    d.setResizable(false);
    Product sel = inventoryTable.getSelectionModel().getSelectedItem();
    if (sel == null) { alert("Pick a product first"); return; }
    d.setHeaderText("Issue qty for " + sel.sku());
    d.setContentText("Qty");
    d.showAndWait().ifPresent(q -> {
      try {
        int take = Integer.parseInt(q);
        Product after = inv.issue(sel.sku(), take);
        out.logIssue("finance", after.sku(), after.name(), after.unit().name(), take, after.category().name(), after.price());
        refreshInventory();
        refreshOutflow();
      } catch (Exception e) { alert(e.getMessage()); }
    });
  }

  @FXML
  public void exportInventory() {
    Path p = exp.exportInventory(getInventorySorted(sortChoice.getValue()), sortChoice.getValue());
    exportMsg.setText("Saved: " + p.toString());
  }

  @FXML
  public void exportOutflow() {
    String tag = getOutflowSortTag();
    Path p = exp.exportOutflow(out.listAll(), tag);
    exportMsg.setText("Saved: " + p.toString());
  }

  private String getOutflowSortTag() {
    if (outflowTable == null) return "UNSORTED";
    var order = outflowTable.getSortOrder();
    if (order == null || order.isEmpty()) return "UNSORTED";

    TableColumn<?, ?> c = order.get(0);
    String dir = c.getSortType() == TableColumn.SortType.DESCENDING ? "DESC" : "ASC";

    if (c == ofDate)       return "DATE_" + dir;
    if (c == ofUser)       return "USER_" + dir;
    if (c == ofSku)        return "SKU_" + dir;
    if (c == ofName)       return "PRODUCT_" + dir;
    if (c == ofUnit)       return "UNIT_" + dir;
    if (c == ofQty)        return "QTY_" + dir;
    if (c == ofPrice)      return "PRICE_" + dir;
    if (c == ofTotalPrice) return "TOTAL_PRICE_" + dir;
    if (c == ofCat)        return "CATEGORY_" + dir;

    return "UNSORTED";
  }

  private Dialog<Product> productDialog(Product base) {
    Dialog<Product> d = new Dialog<>();
    Window owner = inventoryTable.getScene() != null ? inventoryTable.getScene().getWindow() : null;
    if (owner != null) d.initOwner(owner);
    d.initModality(Modality.WINDOW_MODAL);
    d.setResizable(false);
    d.setTitle(base == null ? "Add Product" : "Update Product");
    ButtonType ok = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

    TextField fSku = new TextField();
    TextField fName = new TextField();
    TextField fPrice = new TextField();
    TextField fQty = new TextField();
    ChoiceBox<UnitType> cbUnit = new ChoiceBox<>(FXCollections.observableArrayList(UnitType.values()));
    ChoiceBox<Category> cbCat = new ChoiceBox<>(FXCollections.observableArrayList(Category.values()));

    if (base != null) {
      fSku.setText(base.sku());
      fSku.setDisable(true);
      fName.setText(base.name());
      fPrice.setText(base.price().toString());
      fQty.setText(String.valueOf(base.qty()));
      cbUnit.setValue(base.unit());
      cbCat.setValue(base.category());
    } else {
      cbUnit.setValue(UnitType.EACH);
      cbCat.setValue(Category.OTHER);
    }

    GridPane g = new GridPane();
    g.setHgap(8); g.setVgap(8);
    g.addRow(0, new Label("SKU"), fSku);
    g.addRow(1, new Label("Name"), fName);
    g.addRow(2, new Label("Price"), fPrice);
    g.addRow(3, new Label("Qty"), fQty);
    g.addRow(4, new Label("Unit"), cbUnit);
    g.addRow(5, new Label("Category"), cbCat);
    d.getDialogPane().setContent(g);

    d.setResultConverter(bt -> {
      if (bt == ok) {
        try {
          return new Product(
              base == null ? fSku.getText().trim() : base.sku(),
              fName.getText().trim(),
              new BigDecimal(fPrice.getText().trim()),
              Integer.parseInt(fQty.getText().trim()),
              cbUnit.getValue(),
              cbCat.getValue(),
              base == null ? java.time.LocalDateTime.now() : base.addedOn()
          );
        } catch (Exception e) {
          alert("Invalid input");
          return null;
        }
      }
      return null;
    });
    return d;
  }

  private void alert(String msg) {
    new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
  }

  private boolean confirm(String msg) {
    return new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL)
        .showAndWait().filter(ButtonType.OK::equals).isPresent();
  }

  private Product findProduct(String sku) {
    if (sku == null || sku.isBlank()) return null;
    try {
      for (Product p : inv.list("SKU")) {
        if (p != null && p.sku().equalsIgnoreCase(sku)) {
          return p;
        }
      }
    } catch (Exception ignored) { }
    return null;
  }
}
