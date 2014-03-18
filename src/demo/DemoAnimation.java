package demo;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Created by alexgru-mobile on 18.03.14.
 */
public class DemoAnimation extends Application {

    @Override
    public void init() {
    }

    @Override
    public void start(Stage stage) {
        Rectangle rect = new Rectangle(10, 50, 25, 25);

        // Create the KeyValue for the xProperty and value of 300px
        KeyValue keyValue = new KeyValue(rect.xProperty(), 300,
                Interpolator.EASE_BOTH);

        // Create the KeyFrame with the KeyValue and the time of 2 sec
        KeyFrame keyFrame = new KeyFrame(Duration.millis(2000),
                keyValue);

        // Create the timeline and add the KeyFrame
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(keyFrame);

        // Add the rectangle to a layout container and create a scene
        Pane pane = new Pane();
        pane.getChildren().add(rect);

        Scene scene = new Scene(pane, 335, 125);

        stage.setTitle("Animation Demo");
        stage.setScene(scene);
        stage.show();

        // Play the timeline
        timeline.play();
    }

    @Override
    public void stop() {
    }

    public static void main(String[] parameters) {
        launch(parameters);
    }
}
