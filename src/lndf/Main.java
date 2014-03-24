package lndf;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.stage.Stage;

import java.io.File;
import java.util.Collection;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class Main extends Application {
    private static final int DIM_X = 800;
    private static final int DIM_Y = 500;
    private Image capturedImageSrc;
    private ImageView capturedImage;
    private double cutOutStartX;
    private double cutOutStartY;
    private double cutOutEndX;
    private double cutOutEndY;
    private Rectangle cutOutRegion;


    @Override
    public void init() {
        cutOutRegion = RectangleBuilder.create()
                .x(50)
                .y(50)
                .width(100)
                .height(100)
                .build();
    }

    @Override
    public void start(Stage stage) {
        StackPane pane = new StackPane();
        capturedImageSrc = getMostRecentOrigin();
        capturedImage = new ImageView();
        capturedImage.setImage(capturedImageSrc);
        capturedImage.setPreserveRatio(true);
        capturedImage.setFitHeight(200.0);
        capturedImage.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("CLICK!");
                cutOutStartX = mouseEvent.getX();
                cutOutStartY = mouseEvent.getY();
            }
        });

        capturedImage.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("DRAGGED!");
                cutOutEndX = mouseEvent.getX();
                cutOutEndY = mouseEvent.getY();

                cutOutRegion.setX(cutOutStartX - capturedImage.getFitWidth());
                cutOutRegion.setY(cutOutStartY- capturedImage.getFitHeight());
                cutOutRegion.setWidth(cutOutEndX - cutOutStartX);
                cutOutRegion.setWidth(cutOutEndY - cutOutStartY);
            }
        });

        capturedImage.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("RELEASED!");

            }
        });
        pane.getChildren().add(capturedImage);
        pane.getChildren().add(cutOutRegion);
        Scene scene = new Scene(pane, DIM_X, DIM_Y);

        stage.setScene(scene);
        stage.show();
    }

    private Image getMostRecentOrigin() {
        File dir = new File("media");
        File mostRecentOrigin = null;
        for (final File f : dir.listFiles()) {
            if (mostRecentOrigin == null || f.lastModified() > mostRecentOrigin.lastModified()) {
                mostRecentOrigin = f;
            }
        }
        return new Image("file:" + dir.getName() + "/" + mostRecentOrigin.getName());
    }


    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}