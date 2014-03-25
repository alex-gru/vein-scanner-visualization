package lndf;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class Main extends Application {
    private static final int SCENESIZE_X = 1920;
    private static final int SCENESIZE_Y = (SCENESIZE_X * 9) / 16;
    private static final int CAPTUREDIMAGE_SIZE_X = SCENESIZE_X / 3;

    private static final String ORIGIN_DIR_NAME = "media/";
    private static final String ROI_DIR_NAME = ORIGIN_DIR_NAME + "ROI/";
    private static final String FEATURE_DIR_NAME = ORIGIN_DIR_NAME + "Feature/";
    private static final double CUTOUT_LINEWIDTH = 5;

    private File originDir = new File(ORIGIN_DIR_NAME);
    private File roiDir = new File(ROI_DIR_NAME);
    private File featureDir = new File(FEATURE_DIR_NAME);

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

            double ratio = capturedImageSrc.getWidth() / capturedImage.getFitWidth();

            System.out.println("\t(" + cutOutStartX + "," + cutOutStartY + ")");
            System.out.println("\t==> in source: (" + cutOutStartX * ratio + "," + cutOutStartY * ratio + ")");
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

            //check if selection outside of image
            double originWidth = capturedImage.getBoundsInLocal().getWidth();
            double originHeight = capturedImage.getBoundsInLocal().getHeight();
            double cutOutRegionHeight = cutOutRegion.getBoundsInLocal().getHeight();
            double cutOutRegionWidth = cutOutRegion.getBoundsInLocal().getWidth();

            if (cutOutStartX + cutOutRegionWidth > originWidth) {
                cutOutEndX = originWidth;
                cutOutRegion.setWidth(cutOutEndX - cutOutStartX);
                cutOutRegion.setHeight(cutOutRegion.getWidth());
            }
            if (cutOutStartY + cutOutRegionHeight > originHeight) {
                cutOutEndY = originHeight;
                cutOutRegion.setHeight(cutOutEndY - cutOutStartY);
                cutOutRegion.setWidth(cutOutRegion.getHeight());
            }
            cutOutRegion.setVisible(true);
        }
    };
    private EventHandler<? super MouseEvent> mouseReleasedHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            try {
                System.out.println("RELEASED!");
                cutOutRegion.setVisible(false);
                double ratio = capturedImageSrc.getWidth() / capturedImage.getFitWidth();

                int cutOutStartX_ABSOLUTE = (int) (cutOutStartX * ratio);
                int cutOutStartY_ABSOLUTE = (int) (cutOutStartY * ratio);

                // - 1, for safety against raster exceptions
                int cutOutRegionWidth = (int) ((cutOutRegion.getBoundsInLocal().getWidth() - 1) * ratio);
                int cutOutRegionHeight = cutOutRegionWidth;
                System.out.println("(" + cutOutStartX_ABSOLUTE + "," + cutOutStartY_ABSOLUTE + ")\t" + cutOutRegionWidth + ", " + cutOutRegionHeight);

                BufferedImage bImage = ImageIO.read(new File(ORIGIN_DIR_NAME + currOriginName)).getSubimage(cutOutStartX_ABSOLUTE, cutOutStartY_ABSOLUTE, cutOutRegionWidth, cutOutRegionHeight);
                File outputFile = new File(ROI_DIR_NAME + currOriginName.replace(".", "_ROI."));
                ImageIO.write(bImage, "jpg", outputFile);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("An error occured.");
            }
        }
    };
    private ChangeListener<? super Number> sceneSizeChangedListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
//            capturedImage.setFitWidth(newSceneWidth.doubleValue() / 2);
//            cutOutRegion.setX(cutOutRegion.getX() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
//            cutOutRegion.setY(cutOutRegion.getWidth() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
//            cutOutRegion.setWidth(cutOutRegion.getWidth() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
//            cutOutRegion.setHeight(cutOutRegion.getWidth());

            cutOutRegion.setVisible(false);
        }
    };
    private FilenameFilter fileNameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jpg");
        }
    };

    @Override
    public void init() {
        if (!originDir.exists() || !originDir.isDirectory()) {
            originDir.mkdir();
        }
        if (!roiDir.exists() || !roiDir.isDirectory()) {
            roiDir.mkdir();
        }
        if (!featureDir.exists() || !featureDir.isDirectory()) {
            featureDir.mkdir();
        }
        cutOutRegion = RectangleBuilder.create().build();
        cutOutRegion.setStroke(Color.YELLOW);
        cutOutRegion.setStrokeWidth(CUTOUT_LINEWIDTH);
        cutOutRegion.setFill(Color.TRANSPARENT);
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
        Scene scene = new Scene(root, SCENESIZE_X, SCENESIZE_Y);
        scene.widthProperty().addListener(sceneSizeChangedListener);

        stage.setScene(scene);
        stage.show();
        capturedImage.setFitWidth(SCENESIZE_X / 3);
    }

    private Image getMostRecentOrigin() {
        File mostRecentOrigin = null;

        for (final File f : originDir.listFiles(fileNameFilter)) {
            System.out.println("RESULT");
            if (mostRecentOrigin == null || f.lastModified() > mostRecentOrigin.lastModified()) {
                mostRecentOrigin = f;
            }
        }
        currOriginName = mostRecentOrigin.getName();
        System.out.println("file:" + ORIGIN_DIR_NAME + currOriginName);
        return new Image("file:" + ORIGIN_DIR_NAME + currOriginName);
    }


    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}