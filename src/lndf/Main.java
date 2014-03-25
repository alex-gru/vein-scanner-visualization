package lndf;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class Main extends Application {
    private static final int DIM_X = 800;
    private static final int DIM_Y = 500;
    private String currOriginName;

    private Image capturedImageSrc;
    private ImageView capturedImage;
    private double cutOutStartX;
    private double cutOutStartY;
    private double cutOutEndX;
    private double cutOutEndY;
    private Rectangle cutOutRegion;

    private EventHandler<? super MouseEvent> mousePressedHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            System.out.println("CLICK!");
            cutOutStartX = mouseEvent.getX();
            cutOutStartY = mouseEvent.getY();
        }
    };
    private EventHandler<? super MouseEvent> mouseDraggedHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            System.out.println("DRAGGED!");
            cutOutEndX = mouseEvent.getX();
            cutOutEndY = mouseEvent.getY();

            cutOutRegion.setX(capturedImage.getX() + cutOutStartX);
            cutOutRegion.setY(capturedImage.getY() + cutOutStartY);
            cutOutRegion.setWidth(cutOutEndX - cutOutStartX);
            cutOutRegion.setHeight(cutOutRegion.getWidth());
        }
    };

    private EventHandler<? super MouseEvent> mouseReleasedHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            try {
                System.out.println("RELEASED!");
                int cutOutWidth = (int) (cutOutEndX - cutOutStartX);
                int cutOutHeight = (int) (cutOutEndY - cutOutStartY);
                BufferedImage bImage = ImageIO.read(new File(currOriginName)).getSubimage((int) cutOutStartX, (int) cutOutStartY, cutOutWidth, cutOutHeight);
                File outputfile = new File(currOriginName.replace(".", "_ROI."));
                ImageIO.write(bImage, "jpeg", outputfile);
            } catch (IOException e) {
                System.out.println("An error occured.");
            }
        }
    };

    private ChangeListener<? super Number> sceneSizeChangedListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
            capturedImage.setFitWidth(newSceneWidth.doubleValue() / 2);
            cutOutRegion.setX(cutOutRegion.getX() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
            cutOutRegion.setY(cutOutRegion.getWidth() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
            cutOutRegion.setWidth(cutOutRegion.getWidth() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
            cutOutRegion.setHeight(cutOutRegion.getWidth());
        }
    };

    @Override
    public void init() {
        cutOutRegion = RectangleBuilder.create().build();
    }

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        capturedImageSrc = getMostRecentOrigin();
        capturedImage = new ImageView();
        capturedImage.setImage(capturedImageSrc);
        capturedImage.setPreserveRatio(true);
        capturedImage.setOnMousePressed(mousePressedHandler);

        capturedImage.setOnMouseDragged(mouseDraggedHandler);

        capturedImage.setOnMouseReleased(mouseReleasedHandler);
        root.getChildren().add(capturedImage);
        root.getChildren().add(cutOutRegion);
        Scene scene = new Scene(root, DIM_X, DIM_Y);
        scene.widthProperty().addListener(sceneSizeChangedListener);

        stage.setScene(scene);
        stage.show();
        capturedImage.setFitWidth(stage.getWidth() / 2);
    }

    private Image getMostRecentOrigin() {
        File dir = new File("media");
        File mostRecentOrigin = null;
        for (final File f : dir.listFiles()) {
            if (mostRecentOrigin == null || f.lastModified() > mostRecentOrigin.lastModified()) {
                mostRecentOrigin = f;
            }
        }
        currOriginName = dir.getName() + "/" + mostRecentOrigin.getName();
        return new Image("file:" + dir.getName() + "/" + mostRecentOrigin.getName());
    }


    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}