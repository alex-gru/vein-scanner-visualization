package demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class DemoLabel extends Application {
    private Label label;

    @Override
    public void init() {
        label = new Label("Handvenen - Lange Nacht der Forschung");
    }

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.getChildren().add(label);

        Scene scene = new Scene(root, 200, 200);

        stage.setTitle("This is a dummy scene.");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}
