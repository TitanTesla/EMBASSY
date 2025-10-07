package ui;

import auth.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

public class LoginController {
  @FXML private TextField userField;
  @FXML private PasswordField passField;
  @FXML private Label msg;

  private final AuthService auth = new AuthService();
  private Consumer<String> onLogged;

  public void onLoggedIn(Consumer<String> cb) { this.onLogged = cb; }

  @FXML
  public void handleLogin(ActionEvent event) {
    String u = userField.getText() == null ? "" : userField.getText().trim();
    String p = passField.getText() == null ? "" : passField.getText().trim();

    // Basic validation to avoid empty submissions
    if (u.isEmpty() || p.isEmpty()) {
      msg.setText("Enter username and password");
      return;
    }

    // Get the Stage safely from the event source (may be null if called programmatically)
    Stage stage = null;
    if (event != null && event.getSource() instanceof Node) {
      Node src = (Node) event.getSource();
      if (src.getScene() != null && src.getScene().getWindow() instanceof Stage) {
        stage = (Stage) src.getScene().getWindow();
      }
    }

    if (auth.login(u, p)) {
      msg.setText("");
      if (onLogged != null) {
        onLogged.accept(u);
      }
      // We keep using the same Stage; ensure it is visible if it was hidden elsewhere.
      if (stage != null && !stage.isShowing()) {
        stage.show();
      }
    } else {
      msg.setText("Wrong credentials");
    }
  }
}
