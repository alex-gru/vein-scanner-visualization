package lndf;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class Main extends Application {
    private static final String ORIGIN_DIR_NAME = "media/";
    private static final String ROI_DIR_NAME = ORIGIN_DIR_NAME + "ROI/";
    private static final String FEATURE_DIR_NAME = ORIGIN_DIR_NAME + "Feature/";
    private static final double CUTOUT_LINEWIDTH = 5;

    private double capturedImageWidth;
    private double capturedImageHeight;

    private File originDir = new File(ORIGIN_DIR_NAME);
    private File roiDir = new File(ROI_DIR_NAME);
    private File featureDir = new File(FEATURE_DIR_NAME);

    private String currOriginName;
    private String currRoiName;
    private Image capturedImageSrc;
    private ImageView capturedImage;
    private Image roiImageSrc;
    private ImageView roiImage;

    private Button reloadBtn;

    private double cutOutStartX;
    private double cutOutStartY;
    private double cutOutEndX;
    private double cutOutEndY;
    private Rectangle cutOutRegion;

    private EventHandler<? super MouseEvent> mousePressedHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
//            System.out.println("CLICK!");
            cutOutStartX = mouseEvent.getX() - capturedImage.getX();
            cutOutStartY = mouseEvent.getY()- capturedImage.getY();

            double ratio = capturedImageSrc.getWidth() / capturedImage.getFitWidth();

//            System.out.println("\t(" + cutOutStartX + "," + cutOutStartY + ")");
//            System.out.println("\t==> in source: (" + cutOutStartX * ratio + "," + cutOutStartY * ratio + ")");
        }
    };
    private EventHandler<? super MouseEvent> mouseDraggedHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            cutOutEndX = mouseEvent.getX() - capturedImage.getX();
            cutOutEndY = mouseEvent.getY() - capturedImage.getY();

            cutOutRegion.setX(capturedImage.getX() + cutOutStartX);
            cutOutRegion.setY(capturedImage.getY() + cutOutStartY);
            cutOutRegion.setWidth(cutOutEndX - cutOutStartX);
            cutOutRegion.setHeight(cutOutRegion.getWidth());

            //check if selection outside of image
            double cutOutRegionHeight = cutOutRegion.getBoundsInLocal().getHeight();
            double cutOutRegionWidth = cutOutRegion.getBoundsInLocal().getWidth();

            checkBounds(cutOutRegionWidth, cutOutRegionHeight);
            cutOutRegion.setVisible(true);
        }

        private void checkBounds(double cutOutRegionWidth, double cutOutRegionHeight) {
            if (cutOutStartX + cutOutRegionWidth > capturedImageWidth ||
                    cutOutStartY + cutOutRegionHeight > capturedImageHeight) {
                cutOutRegion.setStroke(Color.RED);
            } else {
                cutOutRegion.setStroke(Color.YELLOW);
            }
        }
    };
    private EventHandler<? super MouseEvent> mouseReleasedHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if (!cutOutRegion.getStroke().equals(Color.RED)) {
                try {
                    cutOutRegion.setVisible(false);
                    double ratio = capturedImageSrc.getWidth() / capturedImage.getFitWidth();

                    int cutOutStartX_ABSOLUTE = (int) (cutOutStartX * ratio);
                    int cutOutStartY_ABSOLUTE = (int) (cutOutStartY * ratio);

                    // - 1, for safety against raster exceptions
                    int cutOutRegionWidth = (int) ((cutOutRegion.getBoundsInLocal().getWidth() - 1) * ratio);
                    int cutOutRegionHeight = cutOutRegionWidth;


                    System.out.println("(" + cutOutStartX_ABSOLUTE + "," + cutOutStartY_ABSOLUTE + ")\t" + cutOutRegionWidth + ", " + cutOutRegionHeight);
                    BufferedImage bImage = ImageIO.read(new File(ORIGIN_DIR_NAME + currOriginName)).getSubimage(cutOutStartX_ABSOLUTE, cutOutStartY_ABSOLUTE, cutOutRegionWidth, cutOutRegionHeight);
                    File roiFile = new File(ROI_DIR_NAME + currRoiName);
                    ImageIO.write(bImage, "jpg", roiFile);
                    roiImage.setImage(new Image ("file:" + ROI_DIR_NAME + currRoiName));
                    roiImage.setX(capturedImage.getX());
                    roiImage.setY(capturedImage.getY() + capturedImageHeight + 40);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
    private EventHandler<? super MouseEvent> reloadHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            System.out.println("RELOAD!");
            capturedImageSrc = getMostRecentOrigin();
            capturedImage.setImage(capturedImageSrc);
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
        Rectangle2D displayBounds = Screen.getPrimary().getVisualBounds();
        Group root = new Group();

        ImageView icon = new ImageView(new Image("file:icons/reload-icon.png"));
        reloadBtn = new Button("", icon);
        reloadBtn.setOnMousePressed(reloadHandler);

        icon.setFitWidth(20);
        icon.setFitHeight(icon.getFitWidth());

        capturedImageSrc = getMostRecentOrigin();
        capturedImage = new ImageView();
        capturedImage.setImage(capturedImageSrc);
        capturedImage.setPreserveRatio(true);
        capturedImage.setX(displayBounds.getWidth() / 20);
        capturedImage.setY(capturedImage.getX());
        capturedImage.setOnMousePressed(mousePressedHandler);
        capturedImage.setOnMouseDragged(mouseDraggedHandler);
        capturedImage.setOnMouseReleased(mouseReleasedHandler);

        roiImage = new ImageView();
        root.getChildren().add(capturedImage);
        root.getChildren().add(roiImage);
        root.getChildren().add(cutOutRegion);
        root.getChildren().add(reloadBtn);

        Scene scene = new Scene(root, displayBounds.getWidth(), displayBounds.getHeight());
        scene.widthProperty().addListener(sceneSizeChangedListener);

        stage.setScene(scene);
        stage.show();

        stage.setWidth(displayBounds.getWidth());
        stage.setHeight(displayBounds.getHeight());
        capturedImage.setFitWidth(displayBounds.getWidth() / 3);
        reloadBtn.setLayoutX(capturedImage.getX() + capturedImage.getFitWidth() - reloadBtn.getWidth() - 10);
        reloadBtn.setLayoutY(capturedImage.getY() + 10);
        capturedImageWidth = capturedImage.getBoundsInLocal().getWidth();
        capturedImageHeight = capturedImage.getBoundsInLocal().getHeight();
        roiImage.setFitWidth(capturedImageWidth);
        roiImage.setFitHeight(roiImage.getFitWidth());
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
        currRoiName = currOriginName.replace(".", "_ROI.");
        return new Image("file:" + ORIGIN_DIR_NAME + currOriginName);
    }


    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}