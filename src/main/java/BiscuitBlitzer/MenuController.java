package BiscuitBlitzer;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MenuController {
    @FXML private Pane pane;
    @FXML private Button newButton;
    @FXML private Button loadButton;
    @FXML private Button quitButton;
    @FXML private ImageView imageView;
    @FXML private Pane loadPane;
    @FXML private HBox hBox;
    @FXML private VBox vBox;
    @FXML ScrollPane scrollPane;

    public static boolean darkMode = false;
    public static String backgroundColor = "FFFFFF";

    private static File saveDir;
    boolean sortedReverse = false;

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

            hBox.layoutXProperty().bind(loadPane.widthProperty().subtract(hBox.widthProperty()).divide(2.0));

            scrollPane.setStyle("-fx-background-color: #" + backgroundColor);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setPrefHeight(pane.getHeight() - hBox.heightProperty().getValue());

            scrollPane.layoutXProperty().bind(pane.widthProperty().subtract(scrollPane.widthProperty()).divide(2.0));
            scrollPane.layoutYProperty().bind(hBox.heightProperty());

            vBox.setStyle("-fx-background-color: #" + backgroundColor);
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/game.fxml"));

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

    private void findAndShowGames(File[] saveFiles) {
        vBox.getChildren().removeAll(vBox.getChildren());

        boolean first = true;
        for (File saveFile : saveFiles) {
            Button button = new Button("Open");
            button.setOnAction(e -> {
                try {
                    loadSave(saveFile);
                }
                catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            GridPane grid = new GridPane();
            grid.setPadding(new Insets(20));

            if (darkMode) {
                if (!first)
                    grid.setStyle("-fx-border-color: white; -fx-border-width: 0 2 2 2");
                else
                    grid.setStyle("-fx-border-color: white; -fx-border-width: 2 2 2 2");
            }
            else {
                if (!first)
                    grid.setStyle("-fx-border-color: black; -fx-border-width: 0 2 2 2");
                else
                    grid.setStyle("-fx-border-color: black; -fx-border-width: 2 2 2 2");
            }
            first = false;

            grid.setPrefWidth(loadPane.getPrefWidth()/2.0);
            grid.setVgap(20);

            BorderPane borderPane = makeBorderPane(saveFile, grid);
            grid.add(borderPane, 0, 0);
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(saveFile.lastModified()), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
            grid.addRow(1, new Label("Last modified: " + dateTime.format(formatter)));

            borderPane = new BorderPane();
            borderPane.setPrefWidth(grid.getPrefWidth() + 20);
            borderPane.setLeft(new Label( "Some information about this save"));
            borderPane.setRight(button);
            grid.add(borderPane, 0, 2);

            HBox hBox = new HBox(grid);
            hBox.setAlignment(Pos.CENTER);
            hBox.setPrefWidth(loadPane.getWidth()/2.0);

            vBox.getChildren().add(hBox);
        }

        // avoids bug that makes text within ScrollPane blurry
        for (Node n : scrollPane.getChildrenUnmodifiable()) {
            n.setCache(false);
        }
    }

    private static BorderPane makeBorderPane(File saveFile, GridPane grid) {
        Label saveName = new Label(saveFile.getName().replaceFirst("[.][^.]+$", ""));
        saveName.setStyle("-fx-font-size: 24; -fx-font-weight: bold");
        Menu menu = new Menu("...");
        MenuItem m1 = new MenuItem("Rename");
        MenuItem m2 = new MenuItem("Delete");
        menu.getItems().add(m1);
        menu.getItems().add(m2);
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(menu);
        menuBar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        BorderPane borderPane = new BorderPane();
        borderPane.setPrefWidth(grid.getPrefWidth() + 20);
        borderPane.setLeft(saveName);
        borderPane.setRight(menuBar);
        return borderPane;
    }

    @FXML private void onLoadButtonClick() {
        sortFilesByActivity();
        loadPane.setVisible(true);
    }

    @FXML private void sortFilesAlphabetically() {
        File[] saveFiles = Objects.requireNonNull(saveDir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt")));
        Arrays.sort(saveFiles);

        if (!sortedReverse)
            Arrays.sort(saveFiles, Collections.reverseOrder());
        sortedReverse = !sortedReverse;

        findAndShowGames(saveFiles);
    }

    @FXML private void sortFilesByActivity() {
        File[] saveFiles = Objects.requireNonNull(saveDir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt")));
        Arrays.sort(saveFiles, (file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));

        if (!sortedReverse)
            Collections.reverse(Arrays.asList(saveFiles));
        sortedReverse = !sortedReverse;

        findAndShowGames(saveFiles);
    }

    @FXML private void onChooseDirButtonClick() throws IOException {java.awt.Desktop.getDesktop().open(saveDir);}

    @FXML private void onQuitButtonClick() {Platform.exit();}
}