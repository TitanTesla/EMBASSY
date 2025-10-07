package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import ui.LoginController;

public class Main extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    AppConfig.ensureFoldersAndDb();
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/login.fxml"));
    Scene scene = new Scene(loader.load());
    LoginController ctrl = loader.getController();
    ctrl.onLoggedIn(user -> {
      try {
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/ui/main.fxml"));
        Parent mainRoot = mainLoader.load();
        Scene mainScene = new Scene(mainRoot, stage.getWidth(), stage.getHeight());
        stage.setTitle("Embassy Inventory");
        stage.setScene(mainScene);
        stage.setMaximized(true);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    stage.setTitle("Login");
    stage.setScene(scene);
    stage.setMaximized(true);
    stage.show();
  }
  public static void main(String[] args) { launch(args); }
}
