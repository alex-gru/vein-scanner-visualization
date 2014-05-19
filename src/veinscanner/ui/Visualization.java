package veinscanner.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

/**
 * This visualization is used in combination with the Hand Vein Scanner constructed at the Department of Computer
 * Sciences in Salzburg. The captured image is stored in the subdirectory ORIGIN_DIR_NAME. One can now obtain a squared region
 * of this image, but clicking on the image and dragging it over the image. After releasing the mouse, the chosen region
 * can now be saved for further processing in MATLAB. After MATLAB has finished it's preprocessing and feature
 * extraction routine, one can load the results into this visualization. Furthermore, one can control the subject ID
 * counter, which determines the storing routine with subject IDs in subfolder ORIGIN_WITH_ID_DIR_NAME.
 *
 * @author Alex Gru (Department of Computer Sciences, University of Salzburg)
 * @version 1.0
 */
public class Visualization extends Application {

    /* ------------------------------------------------
    DIRECTORIES
    */
    private static final String ORIGIN_DIR_NAME = "media/";
    private static final String ROI_DIR_NAME = ORIGIN_DIR_NAME + "ROI/";
    private static final String PREP_DIR_NAME = ORIGIN_DIR_NAME + "PREP/";
    private static final String SIFT_DIR_NAME = ORIGIN_DIR_NAME + "SIFT/";
    private static final String SURF_DIR_NAME = ORIGIN_DIR_NAME + "SURF/";
    private static final String ORIGIN_WITH_ID_DIR_NAME = ORIGIN_DIR_NAME + "WITH_ID/";

    private File originDir = new File(ORIGIN_DIR_NAME);
    private File originWithIDDir = new File(ORIGIN_WITH_ID_DIR_NAME);
    private File roiDir = new File(ROI_DIR_NAME);
    private File prepDir = new File(PREP_DIR_NAME);
    private File siftDir = new File(SIFT_DIR_NAME);
    private File surfDir = new File(SURF_DIR_NAME);

    /* ------------------------------------------------
    MISC
    */
    private static final double CUTOUT_LINEWIDTH = 5;
    private static final Color SHAPE_COLOR = Color.YELLOW;
    private static final Color SHAPE_COLOR_ERROR = Color.RED;
    private static final int SCREEN_INDEX = 1; //0: laptop, 1: external
    private static final String MATLAB_PROCESSING_DIR = "E:\\Dropbox\\PR\\LNDF\\Matlab_Processing\\";
    private boolean atLeastOnceDragged = false;
    private boolean auflicht = true; // true: auflicht, false: durchlicht
    private int subject_count = 0;
    private Rectangle2D displayBounds;

    private static final double BUTTON_SIZE_WIDTH = 150;
    private static final double BUTTON_SIZE_HEIGHT = 30;
    private static final double OFFSET_SMALL = 10;
    private static final double OFFSET_MEDIUM = 30;
    private static final double OFFSET_BIG = 50;
    private static final double OFFSET_HUGE = 80;
    private static final double BUTTON_SIZE = 20;
    /* ------------------------------------------------
        DISPLAYED IMAGES
         */
    private ImageView event_logo;
    private ImageView header;

    private double capturedImageWidth;
    private double capturedImageHeight;
    private String currOriginName;
    private String currRoiName;

    private Image capturedImageSrc;
    private ImageView capturedImage;

    private Image roiImageSrc;
    private ImageView roiImage;
    private BufferedImage roiImageBuffered;

    private Image prepImageSrc;
    private ImageView prepImage;

    private Image prepWaImageSrc;
    private ImageView prepWaImage;

    private Image siftImageSrc;
    private ImageView siftImage;

    private Image siftWaImageSrc;
    private ImageView siftWaImage;

    /* ------------------------------------------------
     BUTTONS, LABELS
      */
    private Button saveRoiBtn;
    private Button prepBtn;
    private Button siftBtn;
    private Button auflichtBtn;
    private Button durchlichtBtn;
    private Label subjectCountLabel;

    /* ------------------------------------------------
     CUT OUT REGION VARIABLES
      */
    private double cutOutStartX;
    private double cutOutStartY;
    private double cutOutEndX;
    private double cutOutEndY;
    private Rectangle cutOutRegion;

    /* ------------------------------------------------
     EVENT HANDLERS, LISTENERS, FILE FILTERS
      */
    private EventHandler<? super MouseEvent> mousePressedHandler;
    private EventHandler<? super MouseEvent> mouseDraggedHandler;
    private EventHandler<? super MouseEvent> mouseReleasedHandler;
    private EventHandler<MouseEvent> saveRoiHandler;
    private EventHandler<MouseEvent> prepHandler;
    private EventHandler<MouseEvent> siftHandler;
    private EventHandler<MouseEvent> subjectCountHandler;
    private EventHandler<MouseEvent> auflichtBtnHandler;
    private EventHandler<MouseEvent> durchlichtBtnHandler;

    private FilenameFilter fileNameFilter;

    /* ------------------------------------------------
    TIMER FUNCTIONS
     */
    private long lastTimerCall = System.nanoTime();
    private static final long INTERVAL = 3_000_000_000l;
    private AnimationTimer timer = buildTimer();

    /* ------------------------------------------------
   BASIC METHODS
    */

    /**
     * Contructor calls two initializing methods.
     */
    public Visualization() {
        initializeHandlersAndFilters();
        checkDirsAndFilesExistence();
    }

    /**
     * Here, most of the UI elements are initialized.
     */
    @Override
    public void init() {
        cutOutRegion = RectangleBuilder.create().build();
        cutOutRegion.setStroke(SHAPE_COLOR);
        cutOutRegion.setStrokeWidth(CUTOUT_LINEWIDTH);
        cutOutRegion.setFill(Color.TRANSPARENT);
        cutOutRegion.setMouseTransparent(true);

        ImageView saveRoiIcon = new ImageView(new Image("file:icons/saveroi-icon.png"));
        saveRoiIcon.setFitWidth(BUTTON_SIZE);
        saveRoiIcon.setFitHeight(BUTTON_SIZE);
        saveRoiBtn = new Button("START");
        saveRoiBtn.setOnMousePressed(saveRoiHandler);

        ImageView prepIcon = new ImageView(new Image("file:icons/prep-icon.png"));
        prepIcon.setFitWidth(BUTTON_SIZE);
        prepIcon.setFitHeight(BUTTON_SIZE);
        prepBtn = new Button("PREP");
        prepBtn.setOnMousePressed(prepHandler);

        ImageView siftIcon = new ImageView(new Image("file:icons/sift-icon.png"));
        siftIcon.setFitWidth(BUTTON_SIZE);
        siftIcon.setFitHeight(BUTTON_SIZE);
        siftBtn = new Button("SIFT");
        siftBtn.setOnMousePressed(siftHandler);

        subjectCountLabel = new Label(String.format("%03d", subject_count));
        subjectCountLabel.setTextAlignment(TextAlignment.CENTER);
        subjectCountLabel.setFont(new Font("Arial", 30));
        subjectCountLabel.setOnMousePressed(subjectCountHandler);

        auflichtBtn = new Button("Auflicht");
        auflichtBtn.setOnMousePressed(auflichtBtnHandler);
        durchlichtBtn = new Button("Durchlicht");
        durchlichtBtn.setOnMousePressed(durchlichtBtnHandler);

        saveRoiBtn.setStyle(Styles.buttonStyle());
        prepBtn.setStyle(Styles.buttonStyle());
        siftBtn.setStyle(Styles.buttonStyle());
        auflichtBtn.setStyle(Styles.greenButtonStyle());
        durchlichtBtn.setStyle(Styles.buttonStyle());
    }

    /**
     * Stage elements are loaded and initialized.
     */
    @Override
    public void start(Stage stage) throws Exception {
        try {
            displayBounds = Screen.getScreens().get(SCREEN_INDEX).getVisualBounds();
        } catch (IndexOutOfBoundsException e) {
            throw new Exception("Screen index invalid. Check screen setup!");
        }

        loadHeaderImages();

        capturedImage = new ImageView();
        roiImage = new ImageView();
        prepImage = new ImageView();
        prepWaImage = new ImageView();
        siftImage = new ImageView();
        siftWaImage = new ImageView();

        initializeCapturedImage();

        Group root = new Group();
        addElementsToRoot(root);

        Scene scene = new Scene(root, displayBounds.getWidth(), displayBounds.getHeight());
        stage.setScene(scene);
        stage.setX(displayBounds.getMinX());
        stage.setY(displayBounds.getMinY());
        stage.setFullScreen(true);
        stage.show();
        stage.setWidth(displayBounds.getWidth());
        stage.setHeight(displayBounds.getHeight());

        setLayoutProperties();
        timer.start();
    }

    /**
     * When closing the program, the file writer is closed and timer stopped.
     */
    @Override
    public void stop() {
        timer.stop();
    }

    /**
     * As known, the main method just launches the application. No parameters are used.
     */
    public static void main(String[] parameters) {
        launch(parameters);
    }

    /* ------------------------------------------------
    SPECIALIZED METHODS
     */

    /**
     * Captured images, ROI images, PREP images and SIFT images are stored in specified directories. In this routine,
     * the existence of those directories is checked.
     */
    private void checkDirsAndFilesExistence() {
        if (!originDir.exists() || !originDir.isDirectory()) {
            originDir.mkdir();
        }
        if (!roiDir.exists() || !roiDir.isDirectory()) {
            roiDir.mkdir();
        }
        if (!roiDir.exists() || !roiDir.isDirectory()) {
            roiDir.mkdir();
        }
        if (!prepDir.exists() || !prepDir.isDirectory()) {
            prepDir.mkdir();
        }
        if (!siftDir.exists() || !siftDir.isDirectory()) {
            siftDir.mkdir();
        }
        if (!surfDir.exists() || !surfDir.isDirectory()) {
            surfDir.mkdir();
        }
        if (!originWithIDDir.exists() || !originWithIDDir.isDirectory()) {
            originWithIDDir.mkdir();
        }
    }

    /**
     * The timer periodically checks for new captured images and loads the results.
     */
    private AnimationTimer buildTimer() {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now > lastTimerCall + INTERVAL) {
                    System.out.print(" . ");
                    Image newOne = getMostRecentOrigin();
                    if (newOne != null) {
                        System.out.println("loading new image: " + currOriginName);
                        capturedImageSrc = newOne;
                        capturedImage.setImage(capturedImageSrc);
                        roiImage.setVisible(false);
                        saveRoiBtn.setVisible(false);
                        prepBtn.setVisible(false);
                        siftBtn.setVisible(false);
                        prepImage.setVisible(false);
                        prepWaImage.setVisible(false);
                        siftImage.setVisible(false);
                        siftWaImage.setVisible(false);
                        saveCapturedImageWithID();
                    }
                    lastTimerCall = now;
                }
            }
        };
    }

    /**
     * There are handlers for all buttons, and one filter for directory listening, which are initialized in this method.
     * mousePressedHandler: used while selecting ROI area in captured image.
     * mouseDraggedHandler: same as before, now draws chosen rectangle.
     * mouseReleasedHandler: same as before, loads extracted ROI if selected area is in bounds.
     * saveRoiHandler: when selected ROI is displayed, now the file can be saved.
     * prepHandler: The preprocessed file(MATLAB) is loaded into the interface.
     * siftHandler: The SIFT extracted file(MATLAB) is loaded into the interface.
     * subjectCountHandler: Incrementing and Decrementing ID counter is controlled.
     * fileNameFilter: Used for filtering non-image files, while checking directory contents.
     */
    private void initializeHandlersAndFilters() {
        mousePressedHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                timer.stop();
                atLeastOnceDragged = false;

                cutOutStartX = mouseEvent.getX() - capturedImage.getX();
                cutOutStartY = mouseEvent.getY() - capturedImage.getY();
            }
        };
        mouseDraggedHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                atLeastOnceDragged = true;
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


        };
        mouseReleasedHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                if (!cutOutRegion.getStroke().equals(SHAPE_COLOR_ERROR)
                        && cutOutEndX > cutOutStartX && cutOutEndY > cutOutStartY
                        && atLeastOnceDragged) {
                    saveRoiBtn.setStyle(Styles.buttonStyle());
                    prepBtn.setStyle(Styles.buttonStyle());
                    siftBtn.setStyle(Styles.buttonStyle());
                    double ratio = capturedImageSrc.getWidth() / capturedImage.getFitWidth();

                    int cutOutStartX_ABSOLUTE = (int) (cutOutStartX * ratio);
                    int cutOutStartY_ABSOLUTE = (int) (cutOutStartY * ratio);

                    // - 1, for safety against raster exceptions
                    int cutOutRegionWidth = (int) ((cutOutRegion.getBoundsInLocal().getWidth() - 1) * ratio);
                    int cutOutRegionHeight = cutOutRegionWidth;
                    try {
                        showROI(cutOutStartX_ABSOLUTE, cutOutStartY_ABSOLUTE, cutOutRegionWidth, cutOutRegionHeight);
                    } catch (Exception e) {
                        System.out.println("An error occured. Details: " + e);
                    }
                    saveRoiBtn.setVisible(true);
                    prepBtn.setVisible(true);
                    siftBtn.setVisible(true);
                }
                timer.start();
            }
        };

        saveRoiHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                try {
                    saveRoi();
                    saveRoiBtn.setStyle(Styles.greenButtonStyle());
                } catch (Exception e) {
                    System.out.println("An error occured. Details: " + e);
                }
            }
        };

        prepHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                boolean success = false;

                try {
                    success = loadPrepImagesFromCurrentROI();
                    if (!success) {
                        System.out.println("Problems while loading currentPrepImages.");
                        prepImage.setVisible(false);
                        prepWaImage.setVisible(false);
                    } else {
                        prepImage.setVisible(true);
                        prepWaImage.setVisible(true);
                    }
                } catch (Exception e) {
                    System.out.println("An error occured. Details: " + e);
                }

                if (success) {
                    prepBtn.setStyle(Styles.greenButtonStyle());
                } else {
                    prepBtn.setStyle(Styles.buttonStyle());
                }
            }
        };

        siftHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                boolean success = false;

                try {
                    success = loadSiftImagesFromCurrentROI();
                    if (!success) {
                        System.out.println("Problems while loading currentSiftImages.");
                        siftImage.setVisible(false);
                        siftWaImage.setVisible(false);
                    } else {
                        siftImage.setVisible(true);
                        siftWaImage.setVisible(true);
                    }
                } catch (Exception e) {
                    System.out.println("An error occured. Details: " + e);
                }

                if (success) {
                    siftBtn.setStyle(Styles.greenButtonStyle());
                } else {
                    siftBtn.setStyle(Styles.buttonStyle());

                }
            }
        };

        subjectCountHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                if (mouseEvent.isPrimaryButtonDown()) {
                    subject_count++;
                } else if (mouseEvent.isSecondaryButtonDown()) {
                    if (subject_count > 0) {
                        subject_count--;
                    }
                }
                subjectCountLabel.setText(String.format("%03d", subject_count));
                System.out.println("Current: #" + subject_count);
            }
        };

        auflichtBtnHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                durchlichtBtn.setDisable(false);
                auflicht = true;
                durchlichtBtn.setStyle(Styles.buttonStyle());
                auflichtBtn.setStyle(Styles.greenButtonStyle());
            }
        };

        durchlichtBtnHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                auflichtBtn.setDisable(false);
                auflicht = false;
                auflichtBtn.setStyle(Styles.buttonStyle());
                durchlichtBtn.setStyle(Styles.greenButtonStyle());
            }
        };

        fileNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        };
    }

    /**
     * This helper method checks, if the selected squared area in the captured image is within the bounds of the image.
     * The result of this check determines the color of the rectangle shown in the image. The color is later checked
     * in the processing routine.
     *
     * @param cutOutRegionWidth  The width of the selected squared area
     * @param cutOutRegionHeight The height of the selected squared area
     */
    private void checkBounds(double cutOutRegionWidth, double cutOutRegionHeight) {
        if (cutOutStartX + cutOutRegionWidth > capturedImageWidth ||
                cutOutStartY + cutOutRegionHeight > capturedImageHeight) {
            cutOutRegion.setStroke(SHAPE_COLOR_ERROR);
        } else {
            cutOutRegion.setStroke(SHAPE_COLOR);
        }
    }

    /**
     * When clicking on the specified button, the current preprocessed image (identified by the name of the current
     * captured image's name) is loaded.
     */
    private boolean loadPrepImagesFromCurrentROI() {
        String fileString = PREP_DIR_NAME + currRoiName + "_prep.jpg";
        String fileStringWa = PREP_DIR_NAME + currRoiName + "_prep_wa.jpg";
        File prepFile = new File(fileString);
        File prepWaFile = new File(fileStringWa);

        if (!prepFile.exists() || !prepWaFile.exists()) {
            return false;
        } else {
            prepImageSrc = new Image("file:" + fileString);
            prepImage.setImage(prepImageSrc);
            prepImage.setVisible(true);
            prepWaImageSrc = new Image("file:" + fileStringWa);
            prepWaImage.setImage(prepWaImageSrc);
            prepWaImage.setVisible(true);

            return true;
        }
    }

    /**
     * When clicking on the specified button, the current SIFT extracted image (identified by the name of the current
     * captured image's name) is loaded.
     */
    private boolean loadSiftImagesFromCurrentROI() {
        String fileString = SIFT_DIR_NAME + currRoiName + "_xtr_sift.jpg";
        String fileStringWa = SIFT_DIR_NAME + currRoiName + "_xtr_sift_wa.jpg";
        File siftFile = new File(fileString);
        File siftWaFile = new File(fileStringWa);

        if (!siftFile.exists() || !siftWaFile.exists()) {
            return false;
        } else {
            siftImageSrc = new Image("file:" + fileString);
            siftImage.setImage(siftImageSrc);
            siftImage.setVisible(true);

            siftWaImageSrc = new Image("file:" + fileStringWa);
            siftWaImage.setImage(siftWaImageSrc);
            siftWaImage.setVisible(true);
            return true;
        }
    }

    /**
     * This method loads the previously saved ROI image into the interface.
     *
     * @param x      Start-x of the chosen ROI area
     * @param y      Start-y of the chosen ROI area
     * @param width  Width of the chosen ROI area
     * @param height Height of the chosen ROI area
     */
    private void showROI(int x, int y, int width, int height) throws IOException {
        roiImageBuffered = ImageIO.read(new File(ORIGIN_DIR_NAME + currOriginName)).getSubimage(x, y, width, height);
        roiImageSrc = SwingFXUtils.toFXImage(roiImageBuffered, null);
        roiImage.setImage(roiImageSrc);
        roiImage.setVisible(true);
        saveRoiBtn.setVisible(true);
    }

    /**
     * The previously chosen squared region in the captured image gets saved, if the ROI bounds are within the image
     * (checked in button listener).
     */
    private void saveRoi() throws IOException {
        File roiFile = new File(ROI_DIR_NAME + currRoiName);
        ImageIO.write(roiImageBuffered, "jpg", roiFile);
    }

    /**
     * This method is periodically called by the timer object. It checks, if there is a new image in the directory, by
     * comparing the current name of the captured image with the newest file in the directory.
     */
    private Image getMostRecentOrigin() {
        File mostRecentOrigin = null;

        for (final File f : originDir.listFiles(fileNameFilter)) {
            if (mostRecentOrigin == null || f.lastModified() > mostRecentOrigin.lastModified()) {
                mostRecentOrigin = f;
            }
        }

        if (mostRecentOrigin != null && !mostRecentOrigin.getName().equals(currOriginName)) {
            currOriginName = mostRecentOrigin.getName();
            currRoiName = currOriginName.replace(".", "_ROI.");
            cutOutRegion.setVisible(false);
            return new Image("file:" + ORIGIN_DIR_NAME + currOriginName);
        } else {
            return null;
        }
    }

    /**
     * This method is called every time a new captured image arrives. It's important to know that the subject ID chosen
     * in the interface determines the name of this stored image. For example, if the current subject ID displayed in
     * the interface is '3', and a new image arrives, e.g. 'IMG_0000.JPG' and 'IMG_0000.CR2' respectively, the new image
     * is loaded and the resulting file name generated is 'IMG_0000_ID3.JPG' and 'IMG_0000_ID3.CR2'.
     */
    private void saveCapturedImageWithID() {

        File originFile = new File(ORIGIN_DIR_NAME + "\\" + currOriginName);
        File originFileRaw = new File(ORIGIN_DIR_NAME + "\\" + currOriginName.replace(".JPG", ".CR2"));

        File fileWithID = null;
        File fileWithIDRaw = null;

        if (auflicht) {
            fileWithID = new File(ORIGIN_WITH_ID_DIR_NAME + "\\" + currOriginName.replace(".JPG", "_AUFLICHT" + "_ID" + subject_count + ".JPG"));
            fileWithIDRaw = new File(ORIGIN_WITH_ID_DIR_NAME + "\\" + currOriginName.replace(".JPG", "_AUFLICHT" + "_ID" + subject_count + ".CR2"));
        } else {
            fileWithID = new File(ORIGIN_WITH_ID_DIR_NAME + "\\" + currOriginName.replace(".JPG", "_DURCHLICHT" + "_ID" + subject_count + ".JPG"));
            fileWithIDRaw = new File(ORIGIN_WITH_ID_DIR_NAME + "\\" + currOriginName.replace(".JPG", "_DURCHLICHT" + "_ID" + subject_count + ".CR2"));
        }

        try {
            Files.copy(originFile.toPath(), fileWithID.toPath());
            System.out.println("Saved ID file.");
        } catch (FileAlreadyExistsException e) {
            System.out.println("ID file already exists.");
        } catch (Exception e) {
            System.out.println("Could not save ID file.");
        }

        try {
            while (!originFileRaw.exists()) {
                Thread.sleep(1000);
            }
            Files.copy(originFileRaw.toPath(), fileWithIDRaw.toPath());
            System.out.println("Saved ID raw file.");
        } catch (FileAlreadyExistsException e) {
            System.out.println("ID raw file already exists.");
        } catch (Exception e) {
            System.out.println("Could not save ID raw file.");
            e.printStackTrace();
        }
    }

    /**
     * All UI elements previously defined are loaded into the interface.
     */
    private void addElementsToRoot(Group root) {
        root.getChildren().add(header);
        root.getChildren().add(capturedImage);
        root.getChildren().add(event_logo);
        root.getChildren().add(roiImage);
        root.getChildren().add(prepImage);
        root.getChildren().add(prepWaImage);
        root.getChildren().add(siftImage);
        root.getChildren().add(siftWaImage);
        root.getChildren().add(cutOutRegion);
        root.getChildren().add(saveRoiBtn);
        root.getChildren().add(prepBtn);
        root.getChildren().add(siftBtn);
        root.getChildren().add(auflichtBtn);
        root.getChildren().add(durchlichtBtn);
        root.getChildren().add(subjectCountLabel);
    }

    /**
     * All properties of the captured image displayed are properly set.
     */
    private void initializeCapturedImage() {
        capturedImageSrc = getMostRecentOrigin();
        if (currOriginName != null) {
            saveCapturedImageWithID();
            capturedImage.setImage(capturedImageSrc);
        }
        capturedImage.setPreserveRatio(true);
        capturedImage.setX(OFFSET_BIG);
        capturedImage.setY(OFFSET_MEDIUM + header.getY() + header.getLayoutBounds().getHeight() + capturedImage.getX());

        capturedImage.setOnMousePressed(mousePressedHandler);
        capturedImage.setOnMouseDragged(mouseDraggedHandler);
        capturedImage.setOnMouseReleased(mouseReleasedHandler);
    }

    /**
     * A nice header is loaded, with some fancy logos.
     */
    private void loadHeaderImages() {
        header = new ImageView(new Image("file:icons/header_low.png"));
        header.setPreserveRatio(true);
        header.setFitWidth(displayBounds.getWidth() / 2);
        header.setX(displayBounds.getWidth() - header.getLayoutBounds().getWidth() - 10);

        event_logo = new ImageView(new Image("file:icons/tdot_logo_low.JPG"));
        event_logo.setX(OFFSET_SMALL);
        event_logo.setY(OFFSET_SMALL);
//      LNDF Settings
//      event_logo = new ImageView(new Image("file:icons/event_logo.png"));
        event_logo.setPreserveRatio(true);
        event_logo.setFitWidth(displayBounds.getWidth() / 6);
    }

    /**
     * Based on the layout of the captured image (view), all other elements are oriented according to it, by specifying
     * all related properties.
     */
    private void setLayoutProperties() {
        capturedImage.setFitWidth(displayBounds.getWidth() / 2.7);
        capturedImageWidth = capturedImage.getBoundsInLocal().getWidth();
        capturedImageHeight = capturedImage.getBoundsInLocal().getHeight();

        roiImage.setFitWidth(capturedImageWidth / 2);
        roiImage.setFitHeight(roiImage.getFitWidth());
        roiImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        roiImage.setX(capturedImage.getX() + capturedImageWidth - roiImage.getLayoutBounds().getWidth());
        roiImage.setY(displayBounds.getHeight() - roiImage.getLayoutBounds().getHeight() - OFFSET_BIG);

        prepWaImage.setFitWidth(displayBounds.getWidth() / 4);
        prepWaImage.setFitHeight(prepWaImage.getFitWidth());
        prepWaImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        prepWaImage.setX(displayBounds.getWidth() - prepWaImage.getLayoutBounds().getWidth() - OFFSET_BIG);
        prepWaImage.setY(capturedImage.getY());
        prepWaImage.setVisible(false);

        prepImage.setFitWidth(displayBounds.getWidth() / 4);
        prepImage.setFitHeight(prepImage.getFitWidth());
        prepImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        prepImage.setX(prepWaImage.getX() - prepImage.getLayoutBounds().getWidth() - OFFSET_BIG / 2);
        prepImage.setY(capturedImage.getY());
        prepImage.setVisible(false);

        siftImage.setFitWidth(prepImage.getLayoutBounds().getWidth());
        siftImage.setFitHeight(prepImage.getLayoutBounds().getHeight() * 0.75);
        siftImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        siftImage.setX(prepImage.getX());
        siftImage.setY(prepImage.getY() + prepImage.getLayoutBounds().getHeight() + OFFSET_BIG);

        siftWaImage.setFitWidth(prepWaImage.getLayoutBounds().getWidth());
        siftWaImage.setFitHeight(prepWaImage.getLayoutBounds().getHeight() * 0.75);
        siftWaImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        siftWaImage.setX(prepWaImage.getX());
        siftWaImage.setY(siftImage.getY());

        saveRoiBtn.setPrefSize(BUTTON_SIZE_WIDTH, BUTTON_SIZE_HEIGHT);
        prepBtn.setPrefSize(BUTTON_SIZE_WIDTH, BUTTON_SIZE_HEIGHT);
        siftBtn.setPrefSize(BUTTON_SIZE_WIDTH, BUTTON_SIZE_HEIGHT);
        subjectCountLabel.setPrefSize(BUTTON_SIZE_WIDTH, BUTTON_SIZE_HEIGHT);
        auflichtBtn.setPrefSize(BUTTON_SIZE_WIDTH, BUTTON_SIZE_HEIGHT);
        durchlichtBtn.setPrefSize(BUTTON_SIZE_WIDTH, BUTTON_SIZE_HEIGHT);

        saveRoiBtn.setLayoutX(OFFSET_BIG);
        saveRoiBtn.setLayoutY(roiImage.getY());
        saveRoiBtn.setVisible(false);

        prepBtn.setLayoutX(saveRoiBtn.getLayoutX());
        prepBtn.setLayoutY(saveRoiBtn.getLayoutY() + OFFSET_BIG);
        prepBtn.setVisible(false);

        siftBtn.setLayoutX(prepBtn.getLayoutX());
        siftBtn.setLayoutY(prepBtn.getLayoutY() + OFFSET_BIG);
        siftBtn.setVisible(false);

        subjectCountLabel.setLayoutX(capturedImage.getX() + capturedImageWidth - BUTTON_SIZE_WIDTH / 2 - subjectCountLabel.getWidth() / 2);
        subjectCountLabel.setLayoutY(capturedImage.getY() - OFFSET_HUGE);

        auflichtBtn.setLayoutX(capturedImage.getX() + capturedImageWidth - BUTTON_SIZE_WIDTH);
        auflichtBtn.setLayoutY(subjectCountLabel.getLayoutY() - auflichtBtn.getPrefHeight() - OFFSET_SMALL);
        auflichtBtn.setVisible(true);

        durchlichtBtn.setLayoutX(auflichtBtn.getLayoutX());
        durchlichtBtn.setLayoutY(subjectCountLabel.getLayoutY() + subjectCountLabel.getPrefHeight() + OFFSET_SMALL);
        durchlichtBtn.setVisible(true);

    }
}