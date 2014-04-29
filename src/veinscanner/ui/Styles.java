package veinscanner.ui;

/**
 * Not really the fancy way, but this class provides styles used in the visualization. CSS not really working, so this
 * is a small workaround.
 *
 *
 * @author Alex Gru (Department of Computer Sciences, University of Salzburg)
 * @version 1.0
 */
public class Styles {
    public static String buttonStyle() {
        return "-fx-background-color: \n" +
                "        rgba(0,0,0,0.08),\n" +
                "        linear-gradient(#9a9a9a, #909090),\n" +
                "        linear-gradient(white 0%, #f3f3f3 50%, #ececec 51%, #f2f2f2 100%);\n" +
                "    -fx-background-insets: 0 0 -1 0,0,1;\n" +
                "    -fx-background-radius: 5,5,4;\n" +
                "    -fx-padding: 3 30 3 30;\n" +
                "    -fx-text-fill: #242d35;\n" +
                "    -fx-font-size: 14px;";
    }
    public static String greenButtonStyle() {
        return "-fx-background-color: \n" +
                "        rgba(47, 165, 104, 1);\n" +
                "    -fx-background-insets: 0 0 -1 0,0,1;\n" +
                "    -fx-background-radius: 5,5,4;\n" +
                "    -fx-padding: 3 30 3 30;\n" +
                "    -fx-text-fill: #242d35;\n" +
                "    -fx-font-size: 14px;";
    }
}
