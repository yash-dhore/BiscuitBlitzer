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
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    boolean sortedAlphabetically = false;
    private TextInputDialog inputDialog;

    public static boolean showAlert(String title, String content, Pane pane) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Window owner = pane.getScene().getWindow();
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);

        if (title.equals("Player key") || title.equals("Delete save")) {
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

            inputDialog = new TextInputDialog("");
            inputDialog.setHeaderText("Rename selected save");
            inputDialog.setContentText("New save name: ");
            inputDialog.titleProperty().set("Rename save");
            Window owner = pane.getScene().getWindow();
            inputDialog.initOwner(owner);
            inputDialog.initModality(Modality.APPLICATION_MODAL);
            inputDialog.getDialogPane().getStyleClass().add("custom-alert");

            ImageView view = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/biscuit.png"))));
            view.setFitHeight(32);
            view.setFitWidth(32);
            view.setPreserveRatio(true);

            inputDialog.setGraphic(view);
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

    private String getKey(File saveFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(saveFile.toPath());
        }
        catch (IOException e) {
            return "";
        }

        if (lines.size() != 1)
            return "";

        return parseKey(lines.get(0));
    }

    private void loadSave(File saveFile) throws IOException {
        String parsedKey = getKey(saveFile);

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
            Menu menu = new Menu("...");
            MenuItem m1 = getRenameMenuItem(saveFile);
            MenuItem m2 = getDeleteMenuItem(saveFile);
            menu.getItems().add(m1);
            menu.getItems().add(m2);
            MenuBar menuBar = new MenuBar();
            menuBar.getMenus().add(menu);
            menuBar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            borderPane.setRight(menuBar);
            grid.add(borderPane, 0, 0);
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(saveFile.lastModified()), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
            grid.addRow(1, new Label("Last modified: " + dateTime.format(formatter)));

            borderPane = new BorderPane();
            borderPane.setPrefWidth(grid.getPrefWidth() + 20);
            borderPane.setLeft(new Label(getInfoText(saveFile)));
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

    private MenuItem getRenameMenuItem(File saveFile) {
        MenuItem menuItem = new MenuItem("Rename");
        menuItem.setOnAction(e -> {
            inputDialog.getEditor().clear();
            Optional<String> fileName = inputDialog.showAndWait();
            fileName.ifPresent(event -> {
                String message = isValidFilename(fileName.orElse(null));

                if (!message.equals("Valid filename")) {
                    showAlert("Invalid file name", message, loadPane);
                    return;
                }

                File newName = new File(saveFile.getParentFile(), fileName.orElse(null)+".txt");
                boolean renameSuccess = saveFile.renameTo(newName);

                if (!renameSuccess) {
                    showAlert("Invalid file name", message, loadPane);
                    return;
                }

                sortedReverse = !sortedReverse;
                if (sortedAlphabetically)
                    sortFilesAlphabetically();
                else
                    sortFilesByActivity();
            });
        });

        return menuItem;
    }

    private MenuItem getDeleteMenuItem(File saveFile) {
        MenuItem menuItem = new MenuItem("Delete");
        menuItem.setOnAction(e -> {
            boolean deleteSave = showAlert("Delete save", "Are you sure you want to delete '" + saveFile.getName().replaceFirst("[.][^.]+$", "") + "'?", loadPane);

            if (deleteSave) {
                boolean successfulDelete = saveFile.delete();

                if (successfulDelete) {
                    sortedReverse = !sortedReverse;
                    if (sortedAlphabetically)
                        sortFilesAlphabetically();
                    else
                        sortFilesByActivity();
                }
            }
        });

        return menuItem;
    }

    private String getInfoText(File saveFile) {
        String data = getKey(saveFile);

        if (data.isEmpty())
            return "Invalid player key";

        String[] numberStrings = data.split(",");
        String numBiscuits = GameController.formatNumber(Long.parseLong(numberStrings[0]));
        int multiNums = Integer.parseInt(numberStrings[1]);
        int bpsNums = Integer.parseInt(numberStrings[3]);
        String biscuitGain = GameController.formatNumber(((Instant.now().getEpochSecond() - Long.parseLong(numberStrings[12]))*bpsNums*multiNums));

        return "Biscuits: " + numBiscuits + " | Multiplier: " + multiNums + " | BPS: " + bpsNums + " | Estimated biscuit gain: " + biscuitGain;
    }

    private static BorderPane makeBorderPane(File saveFile, GridPane grid) {
        Label saveName = new Label(saveFile.getName().replaceFirst("[.][^.]+$", ""));
        saveName.setStyle("-fx-font-size: 24; -fx-font-weight: bold");
        BorderPane borderPane = new BorderPane();
        borderPane.setPrefWidth(grid.getPrefWidth() + 20);
        borderPane.setLeft(saveName);
        return borderPane;
    }

    @FXML private void onLoadButtonClick() {
        sortedReverse = true;
        sortFilesByActivity();
        loadPane.setVisible(true);
    }

    @FXML private void sortFilesAlphabetically() {
        File[] saveFiles = Objects.requireNonNull(saveDir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt")));
        Arrays.sort(saveFiles);

        if (!sortedReverse)
            Arrays.sort(saveFiles, Collections.reverseOrder());
        sortedReverse = !sortedReverse;
        sortedAlphabetically = true;
        findAndShowGames(saveFiles);
    }

    @FXML private void sortFilesByActivity() {
        File[] saveFiles = Objects.requireNonNull(saveDir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt")));
        Arrays.sort(saveFiles, (file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));

        if (!sortedReverse)
            Collections.reverse(Arrays.asList(saveFiles));
        sortedReverse = !sortedReverse;
        sortedAlphabetically = false;
        findAndShowGames(saveFiles);
    }
    public static String isValidFilename(String filename) {
        if (filename == null || filename.isEmpty())
            return "Filename cannot be empty";

        String invalidChars = "[\\\\/:*?\"<>|]";
        if (filename.matches(".*" + invalidChars + ".*"))
            return "Filename cannot have extraneous characters";

        if (filename.length() > 16)
            return "Filename cannot have more than 16 characters";

        if (filename.endsWith(".") || filename.endsWith(" "))
            return "Filename cannot end with a period or whitespace";

        try {
            Paths.get(filename);
        }
        catch (Exception e) {
            return "Invalid filename path";
        }

        return "Valid filename";
    }

    @FXML private void onChooseDirButtonClick() throws IOException {java.awt.Desktop.getDesktop().open(saveDir);}

    @FXML private void onQuitButtonClick() {Platform.exit();}
}