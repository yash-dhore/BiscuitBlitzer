package BiscuitBlitzer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class BiscuitBlitzer extends Application {
    public static void main(String[] args) { launch(); }

    @Override public void start(Stage stage) throws IOException { launchMenu(stage, "/lightStyles.css"); }

    public static void launchMenu(Stage stage, String cssFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(BiscuitBlitzer.class.getResource("/menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        scene.getStylesheets().add(Objects.requireNonNull(BiscuitBlitzer.class.getResource(cssFile)).toExternalForm());
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        stage.setTitle("Biscuit Blitzer V" + getGameVersion());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setFullScreen(true);
        stage.getIcons().add(new Image(Objects.requireNonNull(BiscuitBlitzer.class.getResourceAsStream("/images/biscuit.png"))));
        stage.show();
    }

    public static String getGameVersion() {
        String version = "";

        File myObj = new File("pom.xml");
        try {
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                if (line.contains("<version>")) {
                    version = line.substring(13,17);
                    break;
                }
            }
            myReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
        }

        return version;
    }
}