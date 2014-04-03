package lndf;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
 * Created by alexgru-mobile on 18.03.14.
 */
public class LNDF extends Application {

    /* ------------------------------------------------
    DIRECTORIES
    */
    private static final String ORIGIN_DIR_NAME = "media/";
    private static final String ROI_DIR_NAME = ORIGIN_DIR_NAME + "ROI/";
    private static final String PREP_DIR_NAME = ORIGIN_DIR_NAME + "PREP/";
    private static final String PREP_HISTORY_DIR_NAME = ORIGIN_DIR_NAME + "PREP/HISTORY/";
    private static final String SIFT_DIR_NAME = ORIGIN_DIR_NAME + "SIFT/";
    private static final String SURF_DIR_NAME = ORIGIN_DIR_NAME + "SURF/";
    private static final String AGREED_DIR_NAME = ORIGIN_DIR_NAME + "AGREED/";

    private File originDir = new File(ORIGIN_DIR_NAME);
    private File roiDir = new File(ROI_DIR_NAME);
    private File prepDir = new File(PREP_DIR_NAME);
    private File prepHistoryDir = new File(PREP_HISTORY_DIR_NAME);
    private File siftDir = new File(SIFT_DIR_NAME);
    private File surfDir = new File(SURF_DIR_NAME);
    private File agreedDir = new File(AGREED_DIR_NAME);

    /* ------------------------------------------------
    MISC
    */
    private static final double CUTOUT_LINEWIDTH = 5;
    private static final Color SHAPE_COLOR = Color.YELLOW;
    private static final Color SHAPE_COLOR_ERROR = Color.RED;
    private static final double CAPTURED_IMAGE_OFFSET = 50;
    private static final String MATLAB_BIN_DIR = "C:\\Program Files\\MATLAB\\R2012a\\bin\\";
    private static final boolean RELOAD_BUTTON_VISIBLE = false;
    private static final double BUTTON_SIZE = 20;
    private static final int SCREEN_INDEX = 0; //0: laptop, 1: external
    private static final String MATLAB_PROCESSING_DIR = "C:\\Users\\alexgru-mobile\\Dropbox\\PR\\LNDF\\Matlab_Processing\\";
    private boolean atLeastOnceDragged = false;

    private int subject_count = 0;
    private Rectangle2D displayBounds;
    private boolean prepAndSiftReadyToLoad = false;
    /* ------------------------------------------------
    DISPLAYED IMAGES
     */
    private ImageView lndf;
    private ImageView header;

    private double capturedImageWidth;
    private double capturedImageHeight;
    private String currOriginName;
    private String currRoiName;
    private String currPrepName;
    private String currSiftName;

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

    private Image surfImageSrc;
    private ImageView surfImage;

    /* ------------------------------------------------
     BUTTONS, LABELS
      */
    private Button reloadBtn;
    private Button saveRoiBtn;
    private Button siftBtn;
    private Button agreeBtn;
    private Button incrementSubjectBtn;
    private Button decrementSubjectBtn;
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
    private ChangeListener<? super Number> sceneSizeChangedListener;
    private EventHandler<? super MouseEvent> reloadHandler;
    private EventHandler<MouseEvent> saveRoiHandler;
    private EventHandler<MouseEvent> siftHandler;
    private EventHandler<MouseEvent> agreeHandler;
    private EventHandler<MouseEvent> subjectHandler;
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

    public LNDF() {
        initializeHandlers();
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
        reloadIcon.setFitWidth(BUTTON_SIZE);
        reloadIcon.setFitHeight(reloadIcon.getFitWidth());
        reloadBtn = new Button("", reloadIcon);
        reloadBtn.setOnMousePressed(reloadHandler);
        reloadBtn.setVisible(RELOAD_BUTTON_VISIBLE);

        ImageView prepIcon = new ImageView(new Image("file:icons/prep-icon.png"));
        prepIcon.setFitWidth(BUTTON_SIZE);
        prepIcon.setFitHeight(reloadIcon.getFitWidth());
        saveRoiBtn = new Button("", prepIcon);
        saveRoiBtn.setOnMousePressed(saveRoiHandler);
        siftBtn = new Button("", reloadIcon);
//        siftBtn.setOnMousePressed(siftHandler);

        ImageView agreeIcon = new ImageView(new Image("file:icons/agree-icon.png"));
        agreeIcon.setFitWidth(BUTTON_SIZE);
        agreeIcon.setFitHeight(agreeIcon.getFitWidth());

        agreeBtn = new Button("", agreeIcon);
        agreeBtn.setOnMousePressed(agreeHandler);

        subjectCountLabel = new Label("#" + subject_count);
        incrementSubjectBtn = new Button("+");
        incrementSubjectBtn.setOnMousePressed(subjectHandler);
        decrementSubjectBtn = new Button("-");
        decrementSubjectBtn.setOnMousePressed(subjectHandler);

        saveRoiBtn.setStyle(Styles.buttonStyle());
        reloadBtn.setStyle(Styles.buttonStyle());
        siftBtn.setStyle(Styles.buttonStyle());
        agreeBtn.setStyle(Styles.buttonStyle());

        incrementSubjectBtn.setStyle(Styles.buttonStyle());
        decrementSubjectBtn.setStyle(Styles.buttonStyle());
        decrementSubjectBtn.setStyle(Styles.buttonStyle());
    }

    @Override
    public void start(Stage stage) throws Exception {
//        Rectangle2D displayBounds = Screen.getPrimary().getVisualBounds();
        try {
            displayBounds = Screen.getScreens().get(SCREEN_INDEX).getVisualBounds();
        } catch (IndexOutOfBoundsException e) {
            throw new Exception("Screen index invalid. Check screen setup!");
        }

        // load header images
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
        scene.widthProperty().addListener(sceneSizeChangedListener);

        stage.setScene(scene);
        stage.show();

        stage.setWidth(displayBounds.getWidth());
        stage.setHeight(displayBounds.getHeight());

        setLayoutProperties();

        timer.start();
    }

    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }

    /* ------------------------------------------------
    SPECIALIZED METHODS
     */
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

        if (!agreedDir.exists() || !agreedDir.isDirectory()) {
            agreedDir.mkdir();
        }
    }

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
                        siftBtn.setVisible(false);
                        prepImage.setVisible(false);
                        prepWaImage.setVisible(false);
                        siftImage.setVisible(false);
                        siftWaImage.setVisible(false);
                    }
                    if (prepAndSiftReadyToLoad) {
                        if (cutOutRegion.getWidth() > 0 && cutOutRegion.getHeight() > 0) {
                            boolean success = loadPrepImagesFromCurrentROI();
                            if (!success) {
                                System.out.println("currentPrepImages not available yet, will try again in a few seconds.");
                                prepImage.setVisible(false);
                                prepWaImage.setVisible(false);
                            } else {
                                prepImage.setVisible(true);
                                prepWaImage.setVisible(true);
//                            System.out.println("loading currentPrepImages");
                            }

                            try {
                                success = loadSiftImagesFromCurrentROI();
                                if (!success) {
                                    System.out.println("currentSiftImages not available yet, will try again in a few seconds.");
                                    siftImage.setVisible(false);
                                    siftWaImage.setVisible(false);
                                } else {
//                                System.out.println("loading currentSiftImages");
                                    siftImage.setVisible(true);
                                    siftWaImage.setVisible(true);
                                }
                            } catch (Exception e) {
                                System.out.println("An error occured. Details: " + e);
                            }
                        }
                    }
                    lastTimerCall = now;
                }
            }
        };
    }

    private void initializeHandlers() {
        mousePressedHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                //            System.out.println("CLICK!");
                timer.stop();
                prepAndSiftReadyToLoad = false;
                atLeastOnceDragged = false;

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
                }
                timer.start();
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
                saveRoiBtn.setVisible(false);
                prepImage.setVisible(false);
                prepWaImage.setVisible(false);
                Image newOne = getMostRecentOrigin();
                if (newOne != null) {
                    capturedImageSrc = newOne;
                    capturedImage.setImage(capturedImageSrc);
                }
            }
        };
        saveRoiHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
//                System.out.println("Save ROI!");
                try {
                    saveRoi();
                    saveRoiBtn.setStyle(Styles.greenButtonStyle());
                } catch (Exception e) {
                    System.out.println("An error occured. Details: " + e);
                }
                prepAndSiftReadyToLoad = true;
            }
        };

        siftHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
//                System.out.println("PREP!");
                try {
                    // should do matlab on its own
                    // runMatlabPreprocessing();
                    boolean success = loadSiftImagesFromCurrentROI();
                    if (!success) {
                        System.out.println("Problems while loading currentSiftImage.");
                        siftImage.setVisible(false);
                        siftWaImage.setVisible(false);
                    } else {
                        siftImage.setVisible(true);
                        siftWaImage.setVisible(true);
                    }
                } catch (Exception e) {
                    System.out.println("An error occured. Details: " + e);
                }
            }
        };

        agreeHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                saveAgreedCapturedImage();
            }
        };

        subjectHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (((Button) mouseEvent.getSource()).getText().equals("+")) {
                    subject_count++;
                } else {
                    if (subject_count > 0) {
                        subject_count--;
                    }
                }
                subjectCountLabel.setText("#" + subject_count);
            }
        };
        fileNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        };
    }

    private void checkBounds(double cutOutRegionWidth, double cutOutRegionHeight) {
        if (cutOutStartX + cutOutRegionWidth > capturedImageWidth ||
                cutOutStartY + cutOutRegionHeight > capturedImageHeight) {
            cutOutRegion.setStroke(SHAPE_COLOR_ERROR);
        } else {
            cutOutRegion.setStroke(SHAPE_COLOR);
        }
    }

    private boolean loadPrepImagesFromCurrentROI() {
        String fileString = PREP_DIR_NAME + currRoiName + "_prep.jpg";
        String fileStringWa = PREP_DIR_NAME + currRoiName + "_prep_wa.jpg";
//        System.out.println(fileString);
//        System.out.println(fileStringWa);
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

            siftImage.setVisible(false);
            siftWaImage.setVisible(false);
            return true;
        }
    }

    private boolean loadSiftImagesFromCurrentROI() {
        String fileString = SIFT_DIR_NAME + currRoiName + "_xtr_sift.jpg";
        String fileStringWa = SIFT_DIR_NAME + currRoiName + "_xtr_sift_wa.jpg";
//        System.out.println("SIFT: " + fileString);
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

    private void showROI(int x, int y, int width, int height) throws IOException {
        roiImageBuffered = ImageIO.read(new File(ORIGIN_DIR_NAME + currOriginName)).getSubimage(x, y, width, height);
        roiImageSrc = SwingFXUtils.toFXImage(roiImageBuffered, null);
        roiImage.setImage(roiImageSrc);
        roiImage.setVisible(true);
        saveRoiBtn.setVisible(true);
    }

    private void saveRoi() throws IOException {
        File roiFile = new File(ROI_DIR_NAME + currRoiName);
        ImageIO.write(roiImageBuffered, "jpg", roiFile);
    }

    private Image getMostRecentOrigin() {
        File mostRecentOrigin = null;

        for (final File f : originDir.listFiles(fileNameFilter)) {
            if (mostRecentOrigin == null || f.lastModified() > mostRecentOrigin.lastModified()) {
                mostRecentOrigin = f;
            }
        }

//        System.out.println("mostRecentOrigin " + mostRecentOrigin.getName() + " vs " + currOriginName);
        if (mostRecentOrigin != null && !mostRecentOrigin.getName().equals(currOriginName)) {
            currOriginName = mostRecentOrigin.getName();
            currRoiName = currOriginName.replace(".", "_ROI.");
            cutOutRegion.setVisible(false);
            agreeBtn.setEffect(null);
            return new Image("file:" + ORIGIN_DIR_NAME + currOriginName);
        } else {
//            agreeBtn.setEffect(new DropShadow(20, Color.GREEN));
            return null;
        }
    }

    private void saveAgreedCapturedImage() {

        File originFile = new File(ORIGIN_DIR_NAME + "\\" + currOriginName);
        File originFileRaw = new File(ORIGIN_DIR_NAME + "\\" + currOriginName.replace(".JPG", ".CR2"));
//        System.out.println("AFTER: " + currOriginName);
        File agreedFile = new File(AGREED_DIR_NAME + "\\" + currOriginName.replace(".JPG", "_" + subject_count + ".JPG"));
        File agreedFileRaw = new File(AGREED_DIR_NAME + "\\" + currOriginName.replace(".JPG", "_" + subject_count + ".CR2"));
        try {
            Files.copy(originFile.toPath(), agreedFile.toPath());
            System.out.println("Saved agreed file.");
        } catch (FileAlreadyExistsException e) {
            System.out.println("Agreed file already exists.");
        } catch (Exception e) {
            System.out.println("Could not save agreed file. Reason: ");
            e.printStackTrace();
        }

        try {
            Files.copy(originFileRaw.toPath(), agreedFileRaw.toPath());
            System.out.println("Saved agreed raw file.");
        } catch (FileAlreadyExistsException e) {
            System.out.println("Agreed raw file already exists.");
        } catch (Exception e) {
            System.out.println("Could not save agreed raw file. Reason: ");
            e.printStackTrace();
        }

        if (agreedFile.exists() && agreedFileRaw.exists()) {
            agreeBtn.setStyle(Styles.greenButtonStyle());
        } else {
            agreeBtn.setStyle(Styles.buttonStyle());
        }
    }

    private void addElementsToRoot(Group root) {
        root.getChildren().add(header);
        root.getChildren().add(capturedImage);
        root.getChildren().add(lndf);
        root.getChildren().add(roiImage);
        root.getChildren().add(prepImage);
        root.getChildren().add(prepWaImage);
        root.getChildren().add(siftImage);
        root.getChildren().add(siftWaImage);
        root.getChildren().add(cutOutRegion);
        root.getChildren().add(reloadBtn);
        root.getChildren().add(saveRoiBtn);
        root.getChildren().add(siftBtn);
        root.getChildren().add(agreeBtn);
        root.getChildren().add(incrementSubjectBtn);
        root.getChildren().add(decrementSubjectBtn);
        root.getChildren().add(subjectCountLabel);
    }

    private void initializeCapturedImage() {
        capturedImageSrc = getMostRecentOrigin();
        capturedImage.setImage(capturedImageSrc);
        capturedImage.setPreserveRatio(true);
        capturedImage.setX(CAPTURED_IMAGE_OFFSET);
        capturedImage.setY(header.getY() + header.getLayoutBounds().getHeight() + capturedImage.getX());

        capturedImage.setOnMousePressed(mousePressedHandler);
        capturedImage.setOnMouseDragged(mouseDraggedHandler);
        capturedImage.setOnMouseReleased(mouseReleasedHandler);
    }

    private void loadHeaderImages() {
        header = new ImageView(new Image("file:icons/header_low.png"));
        header.setPreserveRatio(true);
        header.setFitWidth(displayBounds.getWidth() / 2);
        header.setX(displayBounds.getWidth() - header.getLayoutBounds().getWidth() - 10);
        lndf = new ImageView(new Image("file:icons/lndf.png"));
        lndf.setPreserveRatio(true);
        lndf.setFitWidth(displayBounds.getWidth() / 6);
        lndf.setX(0);
    }

    private void setLayoutProperties() {
        capturedImage.setFitWidth(displayBounds.getWidth() / 2.7);

        reloadBtn.setLayoutX(capturedImage.getX() + capturedImage.getFitWidth() - reloadBtn.getWidth() - 10);
        reloadBtn.setLayoutY(capturedImage.getY() + 10);

        capturedImageWidth = capturedImage.getBoundsInLocal().getWidth();
        capturedImageHeight = capturedImage.getBoundsInLocal().getHeight();

        roiImage.setFitWidth(capturedImageWidth / 2);
        roiImage.setFitHeight(roiImage.getFitWidth());
        roiImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        roiImage.setX(capturedImage.getX() + capturedImageWidth / 2 - roiImage.getLayoutBounds().getWidth() / 2);
        roiImage.setY(displayBounds.getHeight() - roiImage.getLayoutBounds().getHeight() - CAPTURED_IMAGE_OFFSET);

        prepWaImage.setFitWidth(displayBounds.getWidth() / 4);
        prepWaImage.setFitHeight(prepWaImage.getFitWidth());
        prepWaImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        prepWaImage.setX(displayBounds.getWidth() - prepWaImage.getLayoutBounds().getWidth() - CAPTURED_IMAGE_OFFSET);
        prepWaImage.setY(capturedImage.getY());
        prepWaImage.setVisible(false);

        prepImage.setFitWidth(displayBounds.getWidth() / 4);
        prepImage.setFitHeight(prepImage.getFitWidth());
        prepImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        prepImage.setX(prepWaImage.getX() - prepImage.getLayoutBounds().getWidth() - CAPTURED_IMAGE_OFFSET / 2);
        prepImage.setY(capturedImage.getY());
        prepImage.setVisible(false);

        siftImage.setFitWidth(prepImage.getLayoutBounds().getWidth());
        siftImage.setFitHeight(prepImage.getLayoutBounds().getHeight() * 0.8);
        siftImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        siftImage.setX(prepImage.getX());
        siftImage.setY(prepImage.getY() + prepImage.getLayoutBounds().getHeight() + CAPTURED_IMAGE_OFFSET);

        siftWaImage.setFitWidth(prepWaImage.getLayoutBounds().getWidth());
        siftWaImage.setFitHeight(prepWaImage.getLayoutBounds().getHeight() * 0.8);
        siftWaImage.setEffect(new DropShadow(20, SHAPE_COLOR));
        siftWaImage.setX(prepWaImage.getX());
        siftWaImage.setY(siftImage.getY());

        saveRoiBtn.setLayoutX(roiImage.getX() + roiImage.getFitWidth() - saveRoiBtn.getWidth() - 10);
        saveRoiBtn.setLayoutY(roiImage.getY() + 10);
        saveRoiBtn.setVisible(false);

        siftBtn.setLayoutX(roiImage.getX() + roiImage.getFitWidth() - siftBtn.getWidth() - 10);
        siftBtn.setLayoutY(roiImage.getY() + 50);
        siftBtn.setVisible(false);

        agreeBtn.setLayoutX(capturedImage.getX() + capturedImage.getFitWidth() - agreeBtn.getWidth() - 10);
        agreeBtn.setLayoutY(capturedImage.getY() + 10);
        agreeBtn.setVisible(true);

        subjectCountLabel.setLayoutX(capturedImage.getX() + capturedImageWidth - 2 * CAPTURED_IMAGE_OFFSET);
        subjectCountLabel.setLayoutY(capturedImage.getY() - subjectCountLabel.getHeight() - 20);

        incrementSubjectBtn.setLayoutX(subjectCountLabel.getLayoutX() + subjectCountLabel.getWidth() + 20);
        incrementSubjectBtn.setLayoutY(subjectCountLabel.getLayoutY());
        incrementSubjectBtn.setVisible(true);

        decrementSubjectBtn.setLayoutX(subjectCountLabel.getLayoutX() - decrementSubjectBtn.getWidth() - 20);
        decrementSubjectBtn.setLayoutY(subjectCountLabel.getLayoutY());
        decrementSubjectBtn.setVisible(true);
    }
}