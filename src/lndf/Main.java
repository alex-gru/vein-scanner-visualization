package lndf;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class Main extends Application {
    private static final int DIM_X = 800;
    private static final int DIM_Y = 500;

    @Override
    public void init() {
    }

    @Override
    public void start(Stage stage) {
        StackPane pane = new StackPane();

        Image capturedImageSrc = new Image("file:media/IMG_0031.JPG");
        ImageView capturedImage = new ImageView();
        capturedImage.setImage(capturedImageSrc);
        capturedImage.setPreserveRatio(true);
        capturedImage.setFitHeight(200.0);
        pane.getChildren().add(capturedImage);
        Scene scene = new Scene(pane, DIM_X, DIM_Y);

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