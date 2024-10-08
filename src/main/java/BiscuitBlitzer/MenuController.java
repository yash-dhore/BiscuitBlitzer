package BiscuitBlitzer;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public class MenuController {
    @FXML private Pane pane;
    @FXML private Button newButton;
    @FXML private Button loadButton;
    @FXML private Button quitButton;
    @FXML private ImageView imageView;

    private TextInputDialog inputDialog;

    public static boolean darkMode = false;

    public static void showAlert(String title, String content, Pane pane) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Window owner = pane.getScene().getWindow();
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);

        if (title.equals("Player key"))
            alert.showAndWait();
        else {
            alert.show();

            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(event -> alert.close());
            pause.play();
        }
    }

     public void initialize() {
        pane.heightProperty().addListener((observable, oldValue, newValue) -> updatePositions());
    }

    private void updatePositions() {
        if (pane != null) {
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

            inputDialog = new TextInputDialog("");
            inputDialog.setContentText("Player key: ");
            inputDialog.setHeaderText("Enter your player key");
            Window owner = pane.getScene().getWindow();
            inputDialog.initOwner(owner);
            inputDialog.initModality(Modality.APPLICATION_MODAL);

            inputDialog.getDialogPane().getStyleClass().add("custom-alert");
        }
    }

    @FXML private String parseKey(String input) {
        String playerKey;
        try {
            playerKey = new String(Base64.getDecoder().decode(input));

            String[] numberStrings = playerKey.split(",");
            for (int i = 0; i < 11; i++) {
                Long.parseLong(numberStrings[i]);
            }
        }
        catch (Exception e) {
            return "";
        }

        return playerKey;
    }

    @FXML private void onNewButtonClick() throws IOException {
        openGame("");
    }

    private void openGame(String playerKey) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(BiscuitBlitzer.class.getResource("/game.fxml"));

        Stage stage = (Stage) newButton.getScene().getWindow();

        Scene scene = new Scene(fxmlLoader.load());

        String cssFile;
        if (darkMode) {
            GameController.darkMode = true;
            cssFile = "/darkStyles.css";
        }
        else
            cssFile = "/lightStyles.css";

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(cssFile)).toExternalForm());

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

    @FXML private void onLoadButtonClick() {
        Optional<String> result = inputDialog.showAndWait();
        result.ifPresent(input -> {
            String parsedKey = parseKey(input);
            if (parsedKey.isEmpty()) {
                showAlert("Player key", "Invalid player key", pane);
            }
            else {
                try {
                    openGame(parsedKey);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @FXML private void onQuitButtonClick() {Platform.exit();}
}