package BiscuitBlitzer;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MenuController {
    @FXML private Pane pane;
    @FXML private Button newButton;
    @FXML private Button loadButton;
    @FXML private Button quitButton;
    @FXML private ImageView imageView;
    @FXML private Pane loadPane;
    @FXML private Button openSaveDir;
    @FXML private VBox vBox;

//    private TextInputDialog inputDialog;

    public static boolean darkMode = false;
    public static String backgroundColor = "FFFFFF";

    private static File saveDir;

    public static boolean showAlert(String title, String content, Pane pane) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Window owner = pane.getScene().getWindow();
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);

        if (title.equals("Player key")) {
            return alert.showAndWait().map(response -> response == ButtonType.OK).orElse(false);
        }
        else {
            alert.show();

            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(event -> alert.close());
            pause.play();
            return true;
        }
    }

    public void initialize() {
        saveDir = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "BiscuitBlitzer");
        boolean ignore = saveDir.mkdir();

        if (!saveDir.exists()) {
            System.out.println("Error: Cannot create save directory.");
            Platform.exit();
        }

        pane.setStyle("-fx-background-color: #" + backgroundColor);
        pane.heightProperty().addListener((observable, oldValue, newValue) -> updatePositions());
    }

    private void updatePositions() {
        if (pane != null) {
            loadPane.setVisible(false);

            double paneWidth = pane.getWidth();
            double paneHeight = pane.getHeight();

            double buttonWidth = newButton.prefWidth(-1);
            double buttonHeight = newButton.prefHeight(-1);

            double centerX = paneWidth / 2 - buttonWidth / 2;
            double centerY = paneHeight / 2 - (3 * buttonHeight / 2);

            newButton.setLayoutX(centerX);
            newButton.setLayoutY(centerY);

            loadButton.setLayoutX(centerX);
            loadButton.setLayoutY(centerY + buttonHeight + 10);

            quitButton.setLayoutX(centerX);
            quitButton.setLayoutY(centerY + 2 * (buttonHeight + 10));

            imageView.setLayoutX(paneWidth / 2 - imageView.prefWidth(-1) / 2);
            imageView.setLayoutY(paneHeight / 8);

            loadPane.setStyle("-fx-background-color: #" + backgroundColor);
            Scene scene = loadPane.getScene();
            loadPane.prefWidthProperty().bind(scene.widthProperty());
            loadPane.prefHeightProperty().bind(scene.heightProperty());

            checkForEscapeKey();

            openSaveDir.layoutXProperty().bind(pane.widthProperty().subtract(openSaveDir.widthProperty()).divide(2.0));
        }
    }

    @FXML private String parseKey(String input) {
        String playerKey;
        try {
            playerKey = new String(Base64.getDecoder().decode(input));

            String[] numberStrings = playerKey.split(",");
            for (int i = 0; i < 13; i++) {
                if (i == 6 || i == 7)
                    continue;

                Long.parseLong(numberStrings[i]);
            }
        }
        catch (Exception e) {
            return "";
        }

        return playerKey;
    }

    @FXML private void onNewButtonClick() throws IOException {openGame("");}

    private void openGame(String playerKey) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(BiscuitBlitzer.class.getResource("/game.fxml"));

        Stage stage = (Stage) newButton.getScene().getWindow();

        Scene scene = new Scene(fxmlLoader.load());

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/lightStyles.css")).toExternalForm());
        GameController.darkMode = false;
        GameController.backgroundColor = "FFFFFF";

        stage.setTitle("Biscuit Blitzer");

        stage.setScene(scene);
        stage.setResizable(false);
        stage.setFullScreen(true);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/biscuit.png"))));
        stage.show();

        if (!playerKey.isEmpty()) {
            GameController gameController = fxmlLoader.getController();
            gameController.setData(playerKey);
        }
    }

    private void checkForEscapeKey() {
        Scene scene = pane.getScene();

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                loadPane.setVisible(false);
        });
    }

    private void loadSave(File saveFile) throws IOException {
        List<String> lines = Files.readAllLines(saveFile.toPath());

        if (lines.size() != 1) {
            showAlert("Player key", "Invalid player key", pane);
            return;
        }

        String parsedKey = parseKey(lines.get(0));
        if (parsedKey.isEmpty())
            showAlert("Player key", "Invalid player key", pane);
        else
            openGame(parsedKey);
    }

    private void findAndShowGames() {
        vBox.getChildren().removeAll(vBox.getChildren());
        File[] saveFiles = Objects.requireNonNull(saveDir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt")));

        Arrays.sort(saveFiles, (file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));

        for (File saveFile : saveFiles) {
            Button button = new Button(saveFile.getName().replaceFirst("[.][^.]+$", ""));

            button.setOnAction(e -> {
                try {
                    loadSave(saveFile);
                }
                catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            vBox.getChildren().add(button);
        }
    }

    @FXML private void onLoadButtonClick() {
        findAndShowGames();
        loadPane.setVisible(true);
    }

    @FXML private void onChooseDirButtonClick() throws IOException {java.awt.Desktop.getDesktop().open(saveDir);}

    @FXML private void onQuitButtonClick() {Platform.exit();}
}