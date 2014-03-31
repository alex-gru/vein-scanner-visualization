package lndf;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
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
import java.io.IOException;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class LNDF extends Application {
    private static final String ORIGIN_DIR_NAME = "media/";
    private static final String ROI_DIR_NAME = ORIGIN_DIR_NAME + "ROI/";
    private static final String PREP_DIR_NAME = ORIGIN_DIR_NAME + "PREP/";
    private static final String PREP_HISTORY_DIR_NAME = ORIGIN_DIR_NAME + "PREP/HISTORY/";
    private static final String SIFT_DIR_NAME = ORIGIN_DIR_NAME + "SIFT/";
    private static final String SURF_DIR_NAME = ORIGIN_DIR_NAME + "SURF/";

    private static final double CUTOUT_LINEWIDTH = 5;
    private static final Color SHAPE_COLOR = Color.YELLOW;
    private static final Color SHAPE_COLOR_ERROR = Color.RED;
    private static final double CAPTURED_IMAGE_OFFSET = 100;
    private static final String MATLAB_BIN_DIR = "C:\\Program Files\\MATLAB\\R2012a\\bin\\";

    private double capturedImageWidth;
    private static final String MATLAB_PROCESSING_DIR = "E:\\Dropbox\\PR\\LNDF\\Matlab_Processing\\";
    private double capturedImageHeight;

    private File originDir = new File(ORIGIN_DIR_NAME);
    private File roiDir = new File(ROI_DIR_NAME);
    private File prepDir = new File(PREP_DIR_NAME);
    private File prepHistoryDir = new File(PREP_HISTORY_DIR_NAME);
    private File siftDir = new File(SIFT_DIR_NAME);
    private File surfDir = new File(SURF_DIR_NAME);

    private String currOriginName;
    private String currRoiName;
    private String currPrepName;
    private String currSiftName;
    private String currSurfName;

    private Image capturedImageSrc;
    private ImageView capturedImage;

    private Image roiImageSrc;
    private ImageView roiImage;

    private Image prepImageSrc;
    private ImageView prepImage;

    private Image siftImageSrc;
    private ImageView siftImage;

    private Image surfImageSrc;
    private ImageView surfImage;

    private Button reloadBtn;
    private Button prepBtn;

    private double cutOutStartX;
    private double cutOutStartY;
    private double cutOutEndX;
    private double cutOutEndY;
    private Rectangle cutOutRegion;

    private EventHandler<? super MouseEvent> mousePressedHandler;
    private EventHandler<? super MouseEvent> mouseDraggedHandler;
    private EventHandler<? super MouseEvent> mouseReleasedHandler;
    private ChangeListener<? super Number> sceneSizeChangedListener;
    private FilenameFilter fileNameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jpg");
        }
    };
    private EventHandler<? super MouseEvent> reloadHandler;
    private EventHandler<MouseEvent> prepHandler;

    public LNDF() {
        initializeHandlers();
    }

    private void initializeHandlers() {
        mousePressedHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                //            System.out.println("CLICK!");
                cutOutStartX = mouseEvent.getX() - capturedImage.getX();
                cutOutStartY = mouseEvent.getY() - capturedImage.getY();

                double ratio = capturedImageSrc.getWidth() / capturedImage.getFitWidth();

                //            System.out.println("\t(" + cutOutStartX + "," + cutOutStartY + ")");
                //            System.out.println("\t==> in source: (" + cutOutStartX * ratio + "," + cutOutStartY * ratio + ")");
            }
        };
        mouseDraggedHandler = new EventHandler<MouseEvent>() {
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
                    cutOutRegion.setStroke(SHAPE_COLOR_ERROR);
                } else {
                    cutOutRegion.setStroke(SHAPE_COLOR);
                }
            }
        };
        mouseReleasedHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (!cutOutRegion.getStroke().equals(SHAPE_COLOR_ERROR) && cutOutEndX > cutOutStartX && cutOutEndY > cutOutStartY) {
                    double ratio = capturedImageSrc.getWidth() / capturedImage.getFitWidth();

                    int cutOutStartX_ABSOLUTE = (int) (cutOutStartX * ratio);
                    int cutOutStartY_ABSOLUTE = (int) (cutOutStartY * ratio);

                    // - 1, for safety against raster exceptions
                    int cutOutRegionWidth = (int) ((cutOutRegion.getBoundsInLocal().getWidth() - 1) * ratio);
                    int cutOutRegionHeight = cutOutRegionWidth;

//                    System.out.println("(" + cutOutStartX_ABSOLUTE + "," + cutOutStartY_ABSOLUTE + ")\t" + cutOutRegionWidth + ", " + cutOutRegionHeight);

                    try {
                        writeAndLoadROI(cutOutStartX_ABSOLUTE, cutOutStartY_ABSOLUTE, cutOutRegionWidth, cutOutRegionHeight);
                    } catch (Exception e) {
                        System.out.println("An error occured. Details: " + e);
                    }
                }
            }


        };

        sceneSizeChangedListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
//                capturedImage.setFitWidth(newSceneWidth.doubleValue() / 2);
//                cutOutRegion.setX(cutOutRegion.getX() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
//                cutOutRegion.setY(cutOutRegion.getWidth() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
//                cutOutRegion.setWidth(cutOutRegion.getWidth() * (newSceneWidth.doubleValue() / oldSceneWidth.doubleValue()));
//                cutOutRegion.setHeight(cutOutRegion.getWidth());
//                cutOutRegion.setVisible(false);
            }
        };
        reloadHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
//                System.out.println("RELOAD!");
                cutOutRegion.setVisible(false);
                roiImage.setVisible(false);
                prepBtn.setVisible(false);
                prepImage.setVisible(false);
                capturedImageSrc = getMostRecentOrigin();
                capturedImage.setImage(capturedImageSrc);
            }
        };
        prepHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
//                System.out.println("PREP!");
                try {
                    runMatlabPreprocessing();
                    loadPrepImage();
                } catch (Exception e) {
                    System.out.println("An error occured. Details: " + e);
                }
            }
        };
    }

    private void loadPrepImage() {
//        String fileString = "file:" + PREP_DIR_NAME + currRoiName + "_prep.jpg";
        String fileString = PREP_DIR_NAME + "IMG_0025_ROI.JPG_prep.jpg";
        File prepFile = new File(fileString);
        while (!prepFile.exists()) {
            System.out.println("wait..");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        prepImageSrc = new Image("file:" + fileString);
        prepImage.setImage(prepImageSrc);
        prepImage.setVisible(true);
    }

    private void runMatlabPreprocessing() throws IOException, InterruptedException {
        moveCurrentPrepsToHistory();
        Runtime runtime = Runtime.getRuntime();
        String cmd = MATLAB_BIN_DIR + "matlab.exe -nodisplay -nosplash -nodesktop -hidden -r run('" + MATLAB_PROCESSING_DIR + "f_HandVene_Single.m');exit;";
        System.out.println(cmd);
        Process process = runtime.exec(cmd);
        process.waitFor();
    }

    private void moveCurrentPrepsToHistory() {
        for (final File f : prepDir.listFiles(fileNameFilter)) {
            File newDest = new File(prepHistoryDir + "\\" + f.getName());
//            System.out.println(newDest.getAbsolutePath());
            if (!f.renameTo(newDest)) {
                newDest.delete();
                f.renameTo(newDest);
            }
        }
    }

    private void writeAndLoadROI(int cutOutStartX_ABSOLUTE, int cutOutStartY_ABSOLUTE, int cutOutRegionWidth, int cutOutRegionHeight) throws IOException {
        BufferedImage bImage = ImageIO.read(new File(ORIGIN_DIR_NAME + currOriginName)).getSubimage(cutOutStartX_ABSOLUTE, cutOutStartY_ABSOLUTE, cutOutRegionWidth, cutOutRegionHeight);
        File roiFile = new File(ROI_DIR_NAME + currRoiName);
        ImageIO.write(bImage, "jpg", roiFile);
        roiImage.setImage(new Image("file:" + ROI_DIR_NAME + currRoiName));
        roiImage.setVisible(true);
        prepBtn.setVisible(true);
    }

    @Override
    public void init() {
        checkDirs();
        cutOutRegion = RectangleBuilder.create().build();
        cutOutRegion.setStroke(SHAPE_COLOR);
        cutOutRegion.setStrokeWidth(CUTOUT_LINEWIDTH);
        cutOutRegion.setFill(Color.TRANSPARENT);
        cutOutRegion.setMouseTransparent(true);

        ImageView reloadIcon = new ImageView(new Image("file:icons/reload-icon.png"));
        reloadIcon.setFitWidth(20);
        reloadIcon.setFitHeight(reloadIcon.getFitWidth());
        reloadBtn = new Button("", reloadIcon);
        reloadBtn.setOnMousePressed(reloadHandler);

        ImageView prepIcon = new ImageView(new Image("file:icons/prep-icon.png"));
        prepIcon.setFitWidth(20);
        prepIcon.setFitHeight(reloadIcon.getFitWidth());
        prepBtn = new Button("", prepIcon);
        prepBtn.setOnMousePressed(prepHandler);
    }

    private void checkDirs() {
        if (!originDir.exists() || !originDir.isDirectory()) {
            originDir.mkdir();
        }
        if (!roiDir.exists() || !roiDir.isDirectory()) {
            roiDir.mkdir();
        }
        if (!prepDir.exists() || !prepDir.isDirectory()) {
            prepDir.mkdir();
        }
        if (!prepHistoryDir.exists() || !prepHistoryDir.isDirectory()) {
            prepHistoryDir.mkdir();
        }
        if (!siftDir.exists() || !siftDir.isDirectory()) {
            siftDir.mkdir();
        }
        if (!surfDir.exists() || !surfDir.isDirectory()) {
            surfDir.mkdir();
        }
    }

    @Override
    public void start(Stage stage) {
        Rectangle2D displayBounds = Screen.getPrimary().getVisualBounds();

        ImageView header = new ImageView(new Image("file:icons/header_low.png"));
        header.setPreserveRatio(true);
        header.setFitWidth(displayBounds.getWidth()/2);
        header.setX(displayBounds.getWidth() - header.getLayoutBounds().getWidth() - 10);
        ImageView lndf = new ImageView(new Image("file:icons/lndf.png"));
        lndf.setPreserveRatio(true);
        lndf.setFitWidth(300);
        lndf.setX(0);

        Group root = new Group();

        capturedImageSrc = getMostRecentOrigin();
        capturedImage = new ImageView();
        capturedImage.setImage(capturedImageSrc);
        capturedImage.setPreserveRatio(true);
//        capturedImage.setEffect(new DropShadow(20, Color.BLACK));
        capturedImage.setX(CAPTURED_IMAGE_OFFSET);
        capturedImage.setY(header.getY() + header.getLayoutBounds().getHeight() + capturedImage.getX());
        capturedImage.setOnMousePressed(mousePressedHandler);
        capturedImage.setOnMouseDragged(mouseDraggedHandler);
        capturedImage.setOnMouseReleased(mouseReleasedHandler);

        roiImage = new ImageView();
        prepImage = new ImageView();

        root.getChildren().add(header);
        root.getChildren().add(capturedImage);
        root.getChildren().add(lndf);
        root.getChildren().add(roiImage);
        root.getChildren().add(prepImage);
        root.getChildren().add(cutOutRegion);
        root.getChildren().add(reloadBtn);
        root.getChildren().add(prepBtn);

        Scene scene = new Scene(root, displayBounds.getWidth(), displayBounds.getHeight());
        scene.widthProperty().addListener(sceneSizeChangedListener);

        stage.setScene(scene);
        stage.show();

        stage.setWidth(displayBounds.getWidth());
        stage.setHeight(displayBounds.getHeight());
        capturedImage.setFitWidth(displayBounds.getWidth() / 3.5);

        reloadBtn.setLayoutX(capturedImage.getX() + capturedImage.getFitWidth() - reloadBtn.getWidth() - 10);
        reloadBtn.setLayoutY(capturedImage.getY() + 10);

        capturedImageWidth = capturedImage.getBoundsInLocal().getWidth();
        capturedImageHeight = capturedImage.getBoundsInLocal().getHeight();

        roiImage.setFitWidth(capturedImageWidth / 1.5);
        roiImage.setFitHeight(roiImage.getFitWidth());
        roiImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        roiImage.setX(capturedImage.getX() + capturedImageWidth/2 - roiImage.getLayoutBounds().getWidth()/2);
        roiImage.setY(capturedImage.getY() + capturedImageHeight + 40);

        prepImage.setFitWidth(capturedImageWidth / 1.5);
        prepImage.setFitHeight(roiImage.getFitWidth());
        prepImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        prepImage.setX(capturedImage.getX() + capturedImageWidth + 40);
        prepImage.setY(capturedImage.getY());

        prepBtn.setLayoutX(roiImage.getX() + roiImage.getFitWidth() - prepBtn.getWidth() - 10);
        prepBtn.setLayoutY(roiImage.getY() + 10);
        prepBtn.setVisible(false);
    }

    private Image getMostRecentOrigin() {
        File mostRecentOrigin = null;

        for (final File f : originDir.listFiles(fileNameFilter)) {
            if (mostRecentOrigin == null || f.lastModified() > mostRecentOrigin.lastModified()) {
                mostRecentOrigin = f;
            }
        }
        currOriginName = mostRecentOrigin.getName();
        currRoiName = currOriginName.replace(".", "_ROI.");
        cutOutRegion.setVisible(false);
        return new Image("file:" + ORIGIN_DIR_NAME + currOriginName);
    }


    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}