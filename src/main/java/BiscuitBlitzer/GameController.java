package BiscuitBlitzer;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;

public class GameController {
    private static final Random random = new Random();
    private static final DecimalFormat DF = new DecimalFormat("#.###");
    private static final int SECONDS_IN_A_MINUTE = 60;
    private static final int SECONDS_IN_AN_HOUR = 3600;
    private static final int SECONDS_IN_A_DAY = 86400;

    @FXML private Button biscuitButton;
    @FXML private Button bps;
    @FXML private Button multiplier;
    @FXML private Pane pane;
    @FXML private Label text;
    @FXML private Label eventText;
    @FXML private Button darkModeToggle;
    @FXML private Pane transparentPane;
    @FXML private Button achievementsButton;
    @FXML private Button backToGameButton;
    @FXML private Button statsButton;
    @FXML private Button optionsButton;
    @FXML private Button quitAndSaveButton;
    @FXML private Button quitButton;
    @FXML private Pane achievementsPane;
    @FXML private VBox vBox;
    @FXML private Pane optionsPane;
    @FXML private Pane statsPane;
    @FXML private Label totalBiscuitStat;
    @FXML private Label biscuitsClickedStat;
    @FXML private Label totalTimeStat;
    @FXML private Label timeOpenStat;
    @FXML private Label totalTimeOpenStat;
    @FXML private Label eventsTriggeredStat;
    @FXML private ColorPicker hexChooser;

    // statistics
    private long numBiscuits = 0;
    private long totalBiscuits = 0;
    private long startTime;
    private long sessionsOpenTime;
    private long sessionStartTime;
    private int biscuitsBlitzed = 0;
    private int eventsTriggered = 0;
    private int timesBackgroundChanged = 0;

    /* Event types and corresponding value
    0: no event
    1: double
    2: bonus
    3: spam key
    4: phase                            */
    private byte eventType = 0;
    private byte eventMultiplier = 1;
    private byte eventSecondsRemaining = 0;
    private Timeline countdownTimeline;
    private Timeline eventTimeline;
    private TranslateTransition translateTransition;

    // game functioning
    private Timeline gameTimeline;
    private UpgradeButton bpsNums;
    private UpgradeButton multiNums;

    // visual
    public static boolean darkMode = false;
    public static String backgroundColor = "FFFFFF";

    // saving games
    private TextInputDialog inputDialog;
    private File saveFile;

    // achievements
    Achievements achievements;
    private byte darkModeToggleCount = 0;
    private boolean counting = false;

    @FXML private void achievementsScreen() {
        updateAchievements();
        achievementsPane.setVisible(true);
    }

    @FXML private void blitzBiscuit() {
        double newX = random.nextDouble() * (pane.getWidth() - biscuitButton.getWidth());
        double newY = random.nextDouble() * (pane.getHeight() - biscuitButton.getHeight());

        if (eventType == 4) {
            translateTransition.setToX(newX - biscuitButton.getLayoutX());
            translateTransition.setToY(newY - biscuitButton.getLayoutY());
            translateTransition.play();
        }
        else {
            biscuitButton.setLayoutX(newX);
            biscuitButton.setLayoutY(newY);
        }

        long addedBiscuits = 0;
        if (eventType == 2) {
            addedBiscuits += (long) 3600 * bpsNums.getValue() * multiNums.getValue();
            endEvent();
        }

        if (eventType == 3)
            addedBiscuits += (long) eventMultiplier * bpsNums.getValue() * multiNums.getValue();
        else
            addedBiscuits += (long) eventMultiplier * multiNums.getValue();

        numBiscuits += addedBiscuits;
        totalBiscuits += addedBiscuits;

        text.setText("Biscuits: " + formatNumber(numBiscuits));

        biscuitsBlitzed++;

        if (biscuitsBlitzed >= achievements.getThreshold("Master Blitzer") && achievements.isLocked("Master Blitzer")) {
            achievements.unlock("Master Blitzer");
            showAchievement("Master Blitzer");
            updateAchievements();
        }
    }

    @FXML private void changeDarkMode() {
        darkMode = !darkMode;
        switchStylesheet();

        if (!counting) {
            counting = true;
            darkModeToggleCount = 0;

            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> counting = false));

            timeline.setCycleCount(1);
            timeline.play();
        }

        darkModeToggleCount++;

        if (darkModeToggleCount >= achievements.getThreshold("Flashbang") && achievements.isLocked("Flashbang")) {
            achievements.unlock("Flashbang");
            showAchievement("Flashbang");
            updateAchievements();
        }
    }

    @FXML private void handleEscapeKey() {
        if (achievementsPane.isVisible())
            achievementsPane.setVisible(false);
        else if (statsPane.isVisible())
            statsPane.setVisible(false);
        else if (optionsPane.isVisible())
            optionsPane.setVisible(false);
        else
            transparentPane.setVisible(!transparentPane.isVisible());
    }

    @FXML private void onBPSClick() { attemptUpgrade(bpsNums, true); }

    @FXML private void onMultiplierClick() { attemptUpgrade(multiNums, false); }

    @FXML private void optionsScreen() { optionsPane.setVisible(true); }

    @FXML private void quitAndSave() throws IOException {
        if (saveFile == null || !saveFile.exists()) {
            inputDialog.getEditor().clear();
            Optional<String> fileName = inputDialog.showAndWait();
            if (fileName.isPresent()) {
                String message = MenuController.isValidFilename(fileName.orElse(null));

                if (!message.equals("Valid filename")) {
                    MenuController.showAlert("Invalid file name", message, pane, false);
                    return;
                }

                File saveDir = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "BiscuitBlitzer");
                saveFile = new File(saveDir, fileName.orElse(null) + ".txt");

                boolean createSuccess;
                try {
                    createSuccess = saveFile.createNewFile();
                }
                catch (IOException e) {
                    MenuController.showAlert("Invalid file name", "Exception occurred", pane, false);
                    saveFile = new File("");
                    return;
                }

                if (!createSuccess) {
                    MenuController.showAlert("Invalid file name", "Save with that name already exists", pane, false);
                    saveFile = new File("");
                    return;
                }
            }
            else
                return;
        }
        else {
            boolean proceed = MenuController.showAlert("Save game", "Do you want to save your game?", pane, true);

            if (!proceed)
                return;
        }

        gameTimeline.stop();
        if (countdownTimeline != null)
            countdownTimeline.stop();
        if (eventTimeline != null)
            eventTimeline.stop();

        long currentSeconds = Instant.now().getEpochSecond();

        String playerKey = numBiscuits + "," + multiNums.getValue() + "," + totalBiscuits + "," + bpsNums.getValue()
                + "," + multiNums.getUpgradeCost() + "," + bpsNums.getUpgradeCost()
                + "," + darkMode + "," + backgroundColor + "," + startTime
                + "," + (sessionsOpenTime + currentSeconds - sessionStartTime) + "," + biscuitsBlitzed
                + "," + eventsTriggered + "," + currentSeconds;

        String playerKeyEncoded = Base64.getEncoder().encodeToString(playerKey.getBytes());

        boolean setWritable = saveFile.setWritable(true);
        if (!setWritable) {
            System.out.println("Error: Missing permission to change write access to the save file.");
            Platform.exit();
        }

        FileWriter myWriter = new FileWriter(saveFile);
        myWriter.write(playerKeyEncoded);
        myWriter.close();

        setWritable = saveFile.setWritable(false);
        if (!setWritable) {
            System.out.println("Error: Missing permission to change write access to the save file.");
            Platform.exit();
        }

        loadMenu();
    }

    @FXML private void quitNoSave() throws IOException {
        boolean proceed = MenuController.showAlert("Quit without saving", "Are you sure you want to quit without saving your progress?", pane, true);

        if (!proceed)
            return;

        gameTimeline.stop();
        if (countdownTimeline != null)
            countdownTimeline.stop();
        if (eventTimeline != null)
            eventTimeline.stop();

        loadMenu();
    }

    @FXML private void statsScreen() {
        updateStats(Instant.now().getEpochSecond());
        statsPane.setVisible(true);
    }

    private void attemptUpgrade(UpgradeButton nums, boolean isBPS) {
        if (numBiscuits < nums.getUpgradeCost()) {
            if (nums.getUpgradeCost() - numBiscuits > 1)
                MenuController.showAlert("Not enough biscuits", "You need " + formatNumber(nums.getUpgradeCost() - numBiscuits) + " more biscuits to buy this upgrade.", pane, false);
            else
                MenuController.showAlert("Almost there!", "You’re on the brink of unlocking this upgrade—just one more biscuit!", pane, false);
            return;
        }

        numBiscuits -= nums.getUpgradeCost();

        if (isBPS) {
            if (nums.getValue() == 0)
                nums.setValue(1);
            else
                nums.setValue(nums.getValue() * 2);
        }
        else
            nums.setValue(nums.getValue() + 1);

        nums.setUpgradeCost(nums.getUpgradeCost() * 10);

        if (isBPS)
            bps.setText("Buy " + (bpsNums.getValue() * 2) + " BPS for " + formatNumber(nums.getUpgradeCost()) + " biscuits");
        else
            multiplier.setText("Buy " + (multiNums.getValue() + 1) + "x multiplier for " + formatNumber(nums.getUpgradeCost()) + " biscuits");

        text.setText("Biscuits: " + formatNumber(numBiscuits));
    }

    private void bindButton(Region button, double xDiv, DoubleBinding yOffset) {
        button.layoutXProperty().bind(pane.widthProperty().subtract(button.widthProperty()).divide(xDiv));
        button.layoutYProperty().bind(pane.heightProperty().subtract(button.heightProperty()).divide(2.0).add(yOffset));
    }

    private void bindSideBySideButton(Region button, boolean left, DoubleBinding yOffset) {
        if (left)
            button.layoutXProperty().bind(pane.widthProperty().divide(2.0).subtract(button.widthProperty()));
        else
            button.layoutXProperty().bind(pane.widthProperty().divide(2.0));
        button.layoutYProperty().bind(pane.heightProperty().subtract(button.heightProperty()).divide(2.0).add(yOffset));
    }

    private void changePaneColors() {
        pane.setStyle("-fx-background-color: #" + backgroundColor);
        biscuitButton.setStyle("-fx-background-color: #" + backgroundColor);
        achievementsPane.setStyle("-fx-background-color: #" + backgroundColor);
        statsPane.setStyle("-fx-background-color: #" + backgroundColor);
        optionsPane.setStyle("-fx-background-color: #" + backgroundColor);
    }

    private void checkForEscapeKey() {
        Scene scene = pane.getScene();

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                handleEscapeKey();
        });
    }

    private void configureUpgradeButton(UpgradeButton upgradeButton, Button button, int upgradeCost, int value, String text) {
        upgradeButton.setUpgradeCost(upgradeCost);
        upgradeButton.setValue(value);
        button.setText(text);
        button.setFocusTraversable(false);
    }

    private void endEvent() {
        eventMultiplier = 1;
        biscuitButton.setFocusTraversable(false);
        pane.requestFocus();
        eventType = 0;
        countdownTimeline.stop();
        eventTimeline.stop();
        eventSecondsRemaining = 0;
        eventText.setText("");
    }

    private VBox getAchievementBox(int i) {
        String achievementName = getAchievementName(i);
        String achievementDescription = achievements.getAchievementList().get(i).isLocked() && achievements.getAchievementList().get(i).isHidden() ? "This achievement is hidden until you unlock it" : achievements.getAchievementList().get(i).getDescription();
        Label achievementNameLabel = new Label(achievementName);
        Label achievementDescriptionLabel = new Label(achievementDescription);
        achievementNameLabel.setStyle("-fx-font-weight: bold");

        VBox vBox = new VBox(achievementNameLabel, achievementDescriptionLabel);
        vBox.setPadding(new Insets(10));

        if (achievements.getAchievementList().get(i).isLocked()) {
            achievementNameLabel.getStyleClass().add("locked-achievement-label");
            achievementDescriptionLabel.getStyleClass().add("locked-achievement-label");
        }

        vBox.setPrefWidth(achievementsPane.getWidth()/5.0);
        vBox.getStyleClass().add("achievement-box");

        return vBox;
    }

    private String getAchievementName(int i) {
        String achievementName;

        achievementName = achievements.getAchievementList().get(i).isLocked() && achievements.getAchievementList().get(i).isHidden() ? "???" : achievements.getAchievementList().get(i).getName();

        if (achievements.getAchievementList().get(i).isLocked())
            achievementName += " (\uD83D\uDD12)"; // add lock emoji

        return achievementName;
    }

    private void initialBiscuitPosition() {
        biscuitButton.setLayoutX((pane.getWidth() - biscuitButton.getWidth()) / 2);
        biscuitButton.setLayoutY((pane.getHeight() - biscuitButton.getHeight()) / 2);
        biscuitButton.setFocusTraversable(false);
    }

    public void initialize() {
        startTime = Instant.now().getEpochSecond();
        sessionStartTime = startTime;

        setLayout(text, 100);
        text.setText("Biscuits: " + 0);

        setLayout(eventText, 25);
        updateStats(sessionStartTime);

        pane.widthProperty().addListener((obs, oldVal, newVal) -> initialBiscuitPosition());
        pane.heightProperty().addListener((obs, oldVal, newVal) -> initialBiscuitPosition());

        translateTransition = new TranslateTransition();
        translateTransition.setDuration(Duration.millis(100));
        translateTransition.setNode(biscuitButton);
        translateTransition.setCycleCount(1);
        translateTransition.setAutoReverse(false);

        bpsNums = new UpgradeButton();
        configureUpgradeButton(bpsNums, bps, 25, 0, "Buy 1 BPS for 25 biscuits");
        bindButton(bps, 100, pane.heightProperty().divide(-50));

        multiNums = new UpgradeButton();
        configureUpgradeButton(multiNums, multiplier, 100, 1, "Buy 2x multiplier for 100 biscuits");
        bindButton(multiplier, 100, pane.heightProperty().divide(50));

        gameTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> runEverySecond()));
        gameTimeline.setCycleCount(Timeline.INDEFINITE);
        gameTimeline.play();

        pane.heightProperty().addListener((obs, oldVal, newVal) -> {
            setAdditionalPanes();
            checkForEscapeKey();
            changePaneColors();
            inputDialog = MenuController.createInputDialog(pane, "Create a new save", "Save name:", "Save your progress");
        });

        hexChooser.setOnAction(e -> updateBackgroundColors());

        achievements = new Achievements();
        vBox.setSpacing(5);
        vBox.layoutYProperty().bind(achievementsPane.heightProperty().divide(10));
    }

    private void loadMenu() throws IOException {
        Stage stage = (Stage) quitAndSaveButton.getScene().getWindow();

        String cssFile;
        if (darkMode) {
            cssFile = "/darkStyles.css";
        }
        else
            cssFile = "/lightStyles.css";

        MenuController.darkMode = darkMode;
        MenuController.backgroundColor = backgroundColor;

        BiscuitBlitzer.launchMenu(stage, cssFile);
    }

    private void runEverySecond() {
        long currentSeconds = Instant.now().getEpochSecond();

        if (eventSecondsRemaining == 0 && random.nextInt(900) == 0)
            startAnEvent();

        long addedBiscuits = (long) eventMultiplier * multiNums.getValue() * bpsNums.getValue();
        numBiscuits += addedBiscuits;
        totalBiscuits += addedBiscuits;

        text.setText("Biscuits: " + formatNumber(numBiscuits));

        if (statsPane.isVisible())
            updateStats(currentSeconds);

        if ((sessionsOpenTime + currentSeconds - sessionStartTime) >= achievements.getThreshold("Grinder") && achievements.isLocked("Grinder")) {
            achievements.unlock("Grinder");
            showAchievement("Grinder");
            updateAchievements();
        }

    }

    private void setAdditionalPanes() {
        transparentPane.setVisible(false);
        achievementsPane.setVisible(false);
        statsPane.setVisible(false);
        optionsPane.setVisible(false);

        Scene scene = transparentPane.getScene();
        transparentPane.prefWidthProperty().bind(scene.widthProperty());
        transparentPane.prefHeightProperty().bind(scene.heightProperty());

        backToGameButton.prefWidthProperty().bind(scene.widthProperty().divide(7.5));
        achievementsButton.prefWidthProperty().bind(scene.widthProperty().divide(15));
        statsButton.prefWidthProperty().bind(scene.widthProperty().divide(15));
        optionsButton.prefWidthProperty().bind(scene.widthProperty().divide(7.5));
        quitAndSaveButton.prefWidthProperty().bind(scene.widthProperty().divide(15));
        quitButton.prefWidthProperty().bind(scene.widthProperty().divide(15));

        bindButton(backToGameButton, 2, transparentPane.heightProperty().divide(-12.5));
        bindSideBySideButton(achievementsButton, true, transparentPane.heightProperty().divide(-25));
        bindSideBySideButton(statsButton, false, transparentPane.heightProperty().divide(-25));
        bindButton(optionsButton, 2, transparentPane.heightProperty().divide(25));
        bindSideBySideButton(quitButton, true, transparentPane.heightProperty().divide(12.5));
        bindSideBySideButton(quitAndSaveButton, false, transparentPane.heightProperty().divide(12.5));

        achievementsPane.prefWidthProperty().bind(scene.widthProperty());
        achievementsPane.prefHeightProperty().bind(scene.heightProperty());

        statsPane.prefWidthProperty().bind(scene.widthProperty());
        statsPane.prefHeightProperty().bind(scene.heightProperty());

        optionsPane.prefWidthProperty().bind(scene.widthProperty());
        optionsPane.prefHeightProperty().bind(scene.heightProperty());
        bindButton(darkModeToggle, 2, transparentPane.heightProperty().divide(-50));
        bindButton(hexChooser, 2, transparentPane.heightProperty().divide(50));

        setStatLayout(totalBiscuitStat, false, 5);
        setStatLayout(biscuitsClickedStat, false, 3);
        setStatLayout(totalTimeStat, false, 1);
        setStatLayout(timeOpenStat, true, 1);
        setStatLayout(totalTimeOpenStat, true, 3);
        setStatLayout(eventsTriggeredStat, true, 5);
    }

    public void setData(String data, File saveFile) {
        long currentSeconds = Instant.now().getEpochSecond();

        String[] numberStrings = data.split(",");

        numBiscuits = Long.parseLong(numberStrings[0]);
        multiNums.setValue(Integer.parseInt(numberStrings[1]));
        totalBiscuits = Long.parseLong(numberStrings[2]);
        bpsNums.setValue(Integer.parseInt(numberStrings[3]));
        multiNums.setUpgradeCost(Long.parseLong(numberStrings[4]));
        bpsNums.setUpgradeCost(Long.parseLong(numberStrings[5]));
        darkMode = Boolean.parseBoolean(numberStrings[6]);
        backgroundColor = numberStrings[7];
        startTime = Long.parseLong(numberStrings[8]);
        sessionsOpenTime = Long.parseLong(numberStrings[9]);
        biscuitsBlitzed = Integer.parseInt(numberStrings[10]);
        eventsTriggered = Integer.parseInt(numberStrings[11]);

        if (bpsNums.getValue() != 0) {
            long addedBiscuits = (long) multiNums.getValue() * bpsNums.getValue() * (currentSeconds - Long.parseLong(numberStrings[12]));
            numBiscuits += addedBiscuits;
            totalBiscuits += addedBiscuits;

            MenuController.showAlert("Passive income", "You made " + formatNumber(addedBiscuits) + " biscuits while you were away!", pane, false);

            bps.setText("Buy " + (bpsNums.getValue() * 2) + " BPS for " + formatNumber(bpsNums.getUpgradeCost()) + " biscuits");
        }
        else
            bps.setText("Buy " + 1 + " BPS for " + formatNumber(bpsNums.getUpgradeCost()) + " biscuits");

        multiplier.setText("Buy " + (multiNums.getValue() + 1) + "x multiplier for " + formatNumber(multiNums.getUpgradeCost()) + " biscuits");

        text.setText("Biscuits: " + formatNumber(numBiscuits));

        switchStylesheet();
        changePaneColors();

        hexChooser.setValue(javafx.scene.paint.Color.web("#" + backgroundColor));

        this.saveFile = saveFile;
    }

    private void setLayout(Region component, double yDiv) {
        component.layoutXProperty().bind(pane.widthProperty().subtract(component.widthProperty()).divide(2.0));
        component.layoutYProperty().bind(pane.heightProperty().subtract(component.heightProperty()).divide(yDiv));
    }

    private void setStatLayout(Label stat, boolean add, int hMultiplier) {
        stat.layoutXProperty().bind(
                statsPane.widthProperty().subtract(stat.widthProperty()).divide(2)
        );

        var statVHeight = totalBiscuitStat.heightProperty();
        var statVBase = statsPane.heightProperty().subtract(statVHeight).divide(2);

        if (add) {
            stat.layoutYProperty().bind(
                    statVBase.add(statVHeight.multiply(hMultiplier))
            );
        }
        else {
            stat.layoutYProperty().bind(
                    statVBase.subtract(statVHeight.multiply(hMultiplier))
            );
        }
    }

    private void showAchievement(String achievementName) {
        Label exclamation = new Label("Achievement Unlocked!");
        exclamation.setStyle("-fx-font-weight: bold");
        VBox achievementPopup = new VBox(exclamation, new Label(achievementName));
        achievementPopup.getStyleClass().add("achievement-popup");
        pane.getChildren().add(achievementPopup);
        achievementPopup.setTranslateX(5);
        achievementPopup.setTranslateY(5);
        achievementPopup.toFront();

        PauseTransition hidePopup = new PauseTransition(Duration.seconds(5));
        hidePopup.setOnFinished(e -> pane.getChildren().remove(achievementPopup));
        hidePopup.play();
    }

    private void startAnEvent() {
        eventsTriggered++;

        if (eventsTriggered >= achievements.getThreshold("Event Horizon") && achievements.isLocked("Event Horizon")) {
            achievements.unlock("Event Horizon");
            showAchievement("Event Horizon");
            updateAchievements();
        }

        eventSecondsRemaining = 60;

        int randomInt = random.nextInt(48);
        if (randomInt < 3)
            startPhaseEvent();
        else if (randomInt < 8)
            startSpamKeyEvent();
        else if (bpsNums.getValue() != 0 && random.nextInt(48) == 47)
            startBonusEvent();
        else
            startDoubleEvent();
    }

    private void startBonusEvent() {
        startTheEvent(2, "Big biscuit bonus",
                "Press the biscuit button to instantly earn a huge bonus worth one hour of passive income!");
    }

    private void startDoubleEvent() {
        startTheEvent(1, "2x biscuit",
                "You get double the biscuits for the next minute!");

        eventMultiplier = 2;
    }

    private void startPhaseEvent() {
        startTheEvent(4, "Phase Biscuit Event",
                "The biscuit, upon being blitzed, will phase across the screen, allowing for better tracking and greater biscuit blitzing.");
    }

    private void startSpamKeyEvent() {
        startTheEvent(3, "Spam key",
                "Spam the space bar/return key to get biscuits!");

        biscuitButton.requestFocus();
        biscuitButton.setFocusTraversable(true);
    }

    private void startTheEvent(int eventType, String eventName, String eventDescription) {
        MenuController.showAlert(eventName, eventDescription, pane, false);

        this.eventType = (byte) eventType;

        eventText.setText(eventName + " event in progress: " + eventSecondsRemaining + " seconds remaining");

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> updateEventCountdown(eventName))
        );
        countdownTimeline.setCycleCount(60);
        countdownTimeline.play();

        eventTimeline = new Timeline(
                new KeyFrame(Duration.seconds(60), event -> endEvent())
        );
        eventTimeline.setCycleCount(1);
        eventTimeline.play();
    }

    private void switchStylesheet() {
        Scene scene = pane.getScene();
        if (scene != null) {
            String style1 = Objects.requireNonNull(getClass().getResource("/lightStyles.css")).toExternalForm();
            String style2 = Objects.requireNonNull(getClass().getResource("/darkStyles.css")).toExternalForm();

            if (darkMode) {
                scene.getStylesheets().remove(style1);
                scene.getStylesheets().add(style2);
            }
            else {
                scene.getStylesheets().remove(style2);
                scene.getStylesheets().add(style1);
            }
        }
    }

    private void updateAchievements() {
        vBox.getChildren().removeAll(vBox.getChildren());
        for (int i = 0; i < achievements.getAchievementList().size(); i++) {
            VBox achievement = getAchievementBox(i);
            if (i % 4 == 0) {
                HBox hBox = new HBox(achievement);
                hBox.setSpacing(5);
                vBox.getChildren().add(hBox);
            }
            else {
                HBox hBox = (HBox) vBox.getChildren().get(vBox.getChildren().size() - 1);
                hBox.getChildren().add(achievement);
            }
        }

        vBox.layoutXProperty().bind(achievementsPane.widthProperty().subtract(vBox.widthProperty()).divide(2));
    }

    private void updateBackgroundColors() {
        backgroundColor = hexChooser.getValue().toString().substring(2);
        changePaneColors();
        MenuController.backgroundColor = backgroundColor;
        timesBackgroundChanged++;

        if (timesBackgroundChanged >= achievements.getThreshold("Personalizer") && achievements.isLocked("Personalizer")) {
            achievements.unlock("Personalizer");
            showAchievement("Personalizer");
            updateAchievements();
        }
    }

    private void updateEventCountdown(String eventName) {
        eventSecondsRemaining--;
        eventText.setText(eventName + " event in progress: " + eventSecondsRemaining + " seconds remaining");
    }

    private void updateStats(long currentSeconds) {
        totalBiscuitStat.setText("Total biscuits: " + formatNumber(totalBiscuits));
        totalTimeStat.setText("Time since game save creation: " + formatTime(currentSeconds - startTime));
        timeOpenStat.setText("Current session has been open for: " + formatTime(currentSeconds - sessionStartTime));
        totalTimeOpenStat.setText("Total session open time: " + formatTime(sessionsOpenTime + currentSeconds - sessionStartTime));
        biscuitsClickedStat.setText("Biscuits blitzed: " + formatNumber(biscuitsBlitzed));
        eventsTriggeredStat.setText("Events triggered: " + formatNumber(eventsTriggered));
    }

    public static String formatNumber(long number) {
        if (number >= 1_000_000_000_000L)
            return DF.format(number / 1_000_000_000_000.0) + "T";
        else if (number >= 1_000_000_000)
            return DF.format(number / 1_000_000_000.0) + "B";
        else if (number >= 1_000_000)
            return DF.format(number / 1_000_000.0) + "M";
        else if (number >= 1_000)
            return DF.format(number / 1_000.0) + "K";
        else
            return String.valueOf(number);
    }

    private static String formatTime(long totalSeconds) {
        long days = totalSeconds / SECONDS_IN_A_DAY;
        long hours = (totalSeconds % SECONDS_IN_A_DAY) / SECONDS_IN_AN_HOUR;
        long minutes = (totalSeconds % SECONDS_IN_AN_HOUR) / SECONDS_IN_A_MINUTE;
        long seconds = totalSeconds % SECONDS_IN_A_MINUTE;

        StringBuilder result = new StringBuilder();
        boolean hasPrevious = false;

        if (days > 0) {
            result.append(days).append(" day").append(days > 1 ? "s" : "");
            hasPrevious = true;
        }
        if (hours > 0 || hasPrevious) {
            if (hasPrevious) result.append(", ");
            result.append(hours).append(" hour").append(hours > 1 ? "s" : "");
            hasPrevious = true;
        }
        if (minutes > 0 || hasPrevious) {
            if (hasPrevious) result.append(", ");
            result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
            hasPrevious = true;
        }
        if (seconds >= 0) {
            if (hasPrevious) result.append(", ");
            result.append(seconds).append(" second").append(seconds != 1 ? "s" : "");
        }

        return result.toString();
    }

    static class UpgradeButton {
        private int value;
        private long upgradeCost;

        public int getValue() { return value; }

        public void setValue(int value) { this.value = value; }

        public long getUpgradeCost() { return upgradeCost; }

        public void setUpgradeCost(long upgradeCost) { this.upgradeCost = upgradeCost; }
    }
}
