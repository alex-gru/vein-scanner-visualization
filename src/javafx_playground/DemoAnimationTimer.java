package javafx_playground;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class DemoAnimationTimer extends Application {
    private static final long INTERVAL = 1_000_000_000l;
    private static final int DIM_X = 600;
    private static final int DIM_Y = 600;

    Group root = new Group();
    private Circle circle1 = new Circle(50);
    private Circle circle2 = new Circle(50);
    private Circle circle3 = new Circle(50);
    private Circle circle4 = new Circle(50);
    private Circle circle5 = new Circle(50);
    private long lastTimerCall = System.nanoTime();
    private boolean toggle = false;
    private AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (now > lastTimerCall + INTERVAL) {
                toggle ^= true;
                circle1.setVisible(toggle);
                circle2.setVisible(!toggle);
                circle3.setVisible(toggle);
                circle4.setVisible(!toggle);
                circle5.setVisible(toggle);

                setPositionRandomly(circle1);
                setPositionRandomly(circle2);
                setPositionRandomly(circle3);
                setPositionRandomly(circle4);
                setPositionRandomly(circle5);
                lastTimerCall = now;
            }
        }
    };

    private void setPositionRandomly(Circle circle) {
        circle.setCenterX((int) (Math.random() * DIM_X));
        circle.setCenterY((int)(Math.random()*DIM_Y));
    }

    @Override
    public void start(Stage stage) {
        StackPane pane = new StackPane();
        pane.setPadding(new Insets(10, 10, 10, 10));

        setPositionRandomly(circle1);
        setPositionRandomly(circle2);
        setPositionRandomly(circle3);
        setPositionRandomly(circle4);
        setPositionRandomly(circle5);

        root.getChildren().add(circle1);
        root.getChildren().add(circle2);
        root.getChildren().add(circle3);
        root.getChildren().add(circle4);
        root.getChildren().add(circle5);

        pane.getChildren().add(root);

        Scene scene = new Scene(pane, DIM_X, DIM_Y);

        stage.setScene(scene);
        stage.show();

        circle1.getStyleClass().add("circle");
        circle2.getStyleClass().add("circle");
        circle3.getStyleClass().add("circle");
        circle4.getStyleClass().add("circle");
        circle5.getStyleClass().add("circle");



        // Add the stylesheet to the scene
        scene.getStylesheets().add(getClass()
                .getResource("DemoAnimationTimer.css").toExternalForm());

        // Start the timer
        timer.start();
    }

    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}
