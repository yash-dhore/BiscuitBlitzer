package BiscuitBlitzer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;

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
    @FXML private Button backToGame;
    @FXML private Button quitButton;
    @FXML private Button optionsButton;
    @FXML private Button statsButton;
    @FXML private Pane optionsPane;
    @FXML private Pane statsPane;
    @FXML private Label totalBiscuitStat;
    @FXML private Label biscuitsClickedStat;
    @FXML private Label totalTimeStat;
    @FXML private Label timeOpenStat;
    @FXML private Label totalTimeOpenStat;
    @FXML private Label eventsTriggeredStat;
    @FXML private ColorPicker hexChooser;

    private long numBiscuits = 0;
    private long totalBiscuits = 0;
    private long startTime;
    private long sessionsOpenTime;
    private long sessionStartTime;
    private int biscuitsBlitzed = 0;
    private int eventsTriggered = 0;

    private short eventMultiplier = 1;
    private boolean spamKeyEventActive = false;
    private boolean bonusEventActive = false;
    private short eventSecondsRemaining = 0;
    private Timeline countdownTimeline;
    private Timeline eventTimeline;

    public static boolean darkMode = false;
    public static String backgroundColor = "FFFFFF";

    private UpgradeButton bpsNums;
    private UpgradeButton multiNums;

    private static String formatNumber(long number) {
        if (number >= 1_000_000_000_000L)
            return DF.format(number / 1_000_000_000_000.0) + "T";
        else if (number >= 1_000_000_000)
            return DF.format(number / 1_000_000_000.0) + "B";
        else if (number >= 1_000_000)
            return DF.format(number / 1_000_000.0) +"M";
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

    public void setData(String data) {
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

            MenuController.showAlert("Passive income", "You made " + formatNumber(addedBiscuits) + " biscuits while you were away!", pane);

            bps.setText("Buy " + (bpsNums.getValue() * 2) + " BPS for " + formatNumber(bpsNums.getUpgradeCost()) + " biscuits");
        }
        else
            bps.setText("Buy " + 1 + " BPS for " + formatNumber(bpsNums.getUpgradeCost()) + " biscuits");

        multiplier.setText("Buy " + (multiNums.getValue() + 1) + "x multiplier for " + formatNumber(multiNums.getUpgradeCost()) + " biscuits");

        text.setText("Biscuits: " + formatNumber(numBiscuits));

        switchStylesheet();
        changePaneColors();

        hexChooser.setValue(javafx.scene.paint.Color.web("#" + backgroundColor));
    }

    private void updateStats(long currentSeconds) {
        totalBiscuitStat.setText("Total biscuits: " + formatNumber(totalBiscuits));
        totalTimeStat.setText("Time since game save creation: " + formatTime(currentSeconds - startTime));
        timeOpenStat.setText("Current session has been open for: " + formatTime(currentSeconds - sessionStartTime));
        totalTimeOpenStat.setText("Total session open time: " + formatTime(sessionsOpenTime + currentSeconds - sessionStartTime));
        biscuitsClickedStat.setText("Biscuits blitzed: " + formatNumber(biscuitsBlitzed));
        eventsTriggeredStat.setText("Events triggered: " + formatNumber(eventsTriggered));
    }

    private void setLayout(Region component, double yDiv) {
        component.layoutXProperty().bind(pane.widthProperty().subtract(component.widthProperty()).divide(2.0));
        component.layoutYProperty().bind(pane.heightProperty().subtract(component.heightProperty()).divide(yDiv));
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

    private void configureUpgradeButton(UpgradeButton upgradeButton, Button button, int upgradeCost, int value, String text) {
        upgradeButton.setUpgradeCost(upgradeCost);
        upgradeButton.setValue(value);
        button.setText(text);
        button.setFocusTraversable(false);
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

    public void initialize() {
        startTime = Instant.now().getEpochSecond();
        sessionStartTime = startTime;

        setLayout(text, 100);
        text.setText("Biscuits: " + 0);

        setLayout(eventText, 25);
        updateStats(sessionStartTime);

        pane.widthProperty().addListener((obs, oldVal, newVal) -> initialBiscuitPosition());
        pane.heightProperty().addListener((obs, oldVal, newVal) -> initialBiscuitPosition());

        bpsNums = new UpgradeButton();
        configureUpgradeButton(bpsNums, bps,25, 0, "Buy 1 BPS for 25 biscuits");
        bindButton(bps, 100, pane.heightProperty().divide(50).multiply(-1));

        multiNums = new UpgradeButton();
        configureUpgradeButton(multiNums, multiplier, 100, 1, "Buy 2x multiplier for 100 biscuits");
        bindButton(multiplier, 100, pane.heightProperty().divide(50));

        Timeline gameTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> runEverySecond()));
        gameTimeline.setCycleCount(Timeline.INDEFINITE);
        gameTimeline.play();

        pane.heightProperty().addListener((obs, oldVal, newVal) -> {
            setAdditionalPanes();
            checkForEscapeKey();
            changePaneColors();
        });

        hexChooser.setOnAction(e -> updateBackgroundColors());
    }

    private void updateBackgroundColors() {
        backgroundColor = hexChooser.getValue().toString().substring(2);
        changePaneColors();
        MenuController.backgroundColor = backgroundColor;

    }
    private void changePaneColors() {
        optionsPane.setStyle("-fx-background-color: #" + backgroundColor);
        pane.setStyle("-fx-background-color: #" + backgroundColor);
        statsPane.setStyle("-fx-background-color: #" + backgroundColor);
        biscuitButton.setStyle("-fx-background-color: #" + backgroundColor);
    }

    private void setAdditionalPanes() {
        transparentPane.setVisible(false);
        optionsPane.setVisible(false);
        statsPane.setVisible(false);

        Scene scene = transparentPane.getScene();
        transparentPane.prefWidthProperty().bind(scene.widthProperty());
        transparentPane.prefHeightProperty().bind(scene.heightProperty());

        bindButton(backToGame, 2, transparentPane.heightProperty().divide(25).multiply(-1));
        bindSideBySideButton(optionsButton, true, transparentPane.heightProperty().multiply(0));
        bindSideBySideButton(statsButton, false, transparentPane.heightProperty().multiply(0));
        bindButton(quitButton, 2, transparentPane.heightProperty().divide(25));

        scene = optionsPane.getScene();
        optionsPane.prefWidthProperty().bind(scene.widthProperty());
        optionsPane.prefHeightProperty().bind(scene.heightProperty());
        bindButton(darkModeToggle, 2, transparentPane.heightProperty().divide(25).multiply(-1));
        bindButton(hexChooser, 2, transparentPane.heightProperty().divide(25));

        scene = statsPane.getScene();
        statsPane.prefWidthProperty().bind(scene.widthProperty());
        statsPane.prefHeightProperty().bind(scene.heightProperty());

        setStatLayout(totalBiscuitStat, false, 5);
        setStatLayout(biscuitsClickedStat, false,3);
        setStatLayout(totalTimeStat, false, 1);
        setStatLayout(timeOpenStat, true, 1);
        setStatLayout(totalTimeOpenStat, true,3);
        setStatLayout(eventsTriggeredStat, true, 5);
    }

    private void checkForEscapeKey() {
        Scene scene = pane.getScene();

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                handleEscapeKey();
        });
    }

    @FXML private void handleEscapeKey() {
        if (optionsPane.isVisible())
            optionsPane.setVisible(false);
        else if (statsPane.isVisible())
            statsPane.setVisible(false);
        else
            transparentPane.setVisible(!transparentPane.isVisible());
    }

    @FXML private void quitAndSave() throws IOException {
        boolean proceed = MenuController.showAlert("Player key", "Click 'OK' to copy your player key to your clipboard. Save it somewhere if you don't want to lose your progress!", pane);

        if (!proceed)
            return;

        long currentSeconds = Instant.now().getEpochSecond();

        String playerKey = numBiscuits + "," + multiNums.getValue() + "," + totalBiscuits + "," + bpsNums.getValue()
                + "," + multiNums.getUpgradeCost() + "," + bpsNums.getUpgradeCost()
                + "," + darkMode + "," + backgroundColor + "," + startTime
                + "," + (sessionsOpenTime + currentSeconds - sessionStartTime) + "," + biscuitsBlitzed
                + "," + eventsTriggered + "," + currentSeconds;

        String playerKeyEncoded = Base64.getEncoder().encodeToString(playerKey.getBytes());

        StringSelection sS = new StringSelection(playerKeyEncoded);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(sS, null);

        Stage stage = (Stage) quitButton.getScene().getWindow();

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
            startEvent();

        long addedBiscuits = (long) eventMultiplier * multiNums.getValue() * bpsNums.getValue();
        numBiscuits += addedBiscuits;
        totalBiscuits += addedBiscuits;

        text.setText("Biscuits: " + formatNumber(numBiscuits));

        if (statsPane.isVisible())
            updateStats(currentSeconds);
    }

    private void startEvent() {
        eventsTriggered++;
        eventSecondsRemaining = 60;

        if (bpsNums.getValue() != 0 && random.nextInt(5) == 0)
            startSpamKeyEvent();
        else if (bpsNums.getValue() != 0 && random.nextInt(48) == 0)
            startBonusEvent();
        else
            startDoubleEvent();
    }

    private void startBonusEvent() {
        String eventType = "Big biscuit bonus";
        MenuController.showAlert(eventType, "Press the biscuit button to instantly earn a huge bonus worth one hour of passive income!", pane);

        bonusEventActive = true;

        eventText.setText(eventType + "event in progress: " + eventSecondsRemaining + " seconds remaining");

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> updateEventCountdown(eventType))
        );
        countdownTimeline.setCycleCount(60);
        countdownTimeline.play();

        eventTimeline = new Timeline(
                new KeyFrame(Duration.seconds(60), event -> endBonusEvent())
        );
        eventTimeline.setCycleCount(1);
        eventTimeline.play();
    }

    private void endBonusEvent() {
        bonusEventActive = false;
        countdownTimeline.stop();
        eventTimeline.stop();
        eventSecondsRemaining = 0;
        eventText.setText("");
    }

    private void startSpamKeyEvent() {
        String eventType = "Spam key";
        MenuController.showAlert(eventType, "Spam the space bar/return key to get biscuits!", pane);

        biscuitButton.requestFocus();
        biscuitButton.setFocusTraversable(true);

        spamKeyEventActive = true;

        eventText.setText(eventType + "event in progress: " + eventSecondsRemaining + " seconds remaining");

        Timeline countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> updateEventCountdown(eventType))
        );
        countdownTimeline.setCycleCount(60);
        countdownTimeline.play();

        eventTimeline = new Timeline(
                new KeyFrame(Duration.seconds(60), event -> endSpamKeyEvent())
        );
        eventTimeline.setCycleCount(1);
        eventTimeline.play();
    }

    private void endSpamKeyEvent() {
        pane.requestFocus();
        biscuitButton.setFocusTraversable(false);
        spamKeyEventActive = false;

        eventSecondsRemaining = 0;
        eventText.setText("");
    }

    private void startDoubleEvent() {
        String eventType = "2x biscuit";
        MenuController.showAlert(eventType, "You get double the biscuits for the next minute!", pane);
        eventMultiplier = 2;

        eventText.setText(eventType + " event in progress: " + eventSecondsRemaining + " seconds remaining");

        Timeline countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> updateEventCountdown(eventType))
        );
        countdownTimeline.setCycleCount(60);
        countdownTimeline.play();

        eventTimeline = new Timeline(
                new KeyFrame(Duration.seconds(60), event -> endDoubleEvent())
        );
        eventTimeline.setCycleCount(1);
        eventTimeline.play();
    }

    private void endDoubleEvent() {
        eventMultiplier = 1;
        eventSecondsRemaining = 0;
        eventText.setText("");
    }

    private void updateEventCountdown(String eventType) {
        eventSecondsRemaining--;
        eventText.setText(eventType + " event in progress: " + eventSecondsRemaining + " seconds remaining");
    }

    private void initialBiscuitPosition() {
        biscuitButton.setLayoutX((pane.getWidth() - biscuitButton.getWidth()) / 2);
        biscuitButton.setLayoutY((pane.getHeight() - biscuitButton.getHeight()) / 2);
        biscuitButton.setFocusTraversable(false);
    }

    @FXML private void biscuitBlitzed() {
        double newX = random.nextDouble() * (pane.getWidth() - biscuitButton.getWidth());
        double newY = random.nextDouble() * (pane.getHeight() - biscuitButton.getHeight());

        biscuitButton.setLayoutX(newX);
        biscuitButton.setLayoutY(newY);

        long addedBiscuits = 0;

        if (bonusEventActive) {
            addedBiscuits += (long) 3600 * bpsNums.getValue() * multiNums.getValue();
            endBonusEvent();
        }

        if (!spamKeyEventActive)
            addedBiscuits += (long) eventMultiplier * multiNums.getValue();
        else
            addedBiscuits += (long) eventMultiplier * bpsNums.getValue() * multiNums.getValue();

        numBiscuits += addedBiscuits;
        totalBiscuits += addedBiscuits;

        text.setText("Biscuits: " + formatNumber(numBiscuits));

        biscuitsBlitzed++;
    }

    @FXML private void onBPSClick() {attemptUpgrade(bpsNums, true);}

    @FXML private void onMultiplierClick() {attemptUpgrade(multiNums, false);}

    private void attemptUpgrade(UpgradeButton nums, boolean isBPS) {
        if (numBiscuits < nums.getUpgradeCost()) {
            if (nums.getUpgradeCost() - numBiscuits > 1)
                MenuController.showAlert("Not enough biscuits", "You need " + formatNumber(nums.getUpgradeCost() - numBiscuits) + " more biscuits to buy this upgrade.", pane);
            else
                MenuController.showAlert("Almost there!", "You’re on the brink of unlocking this upgrade—just one more biscuit!", pane);
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

    @FXML private void optionsScreen() {optionsPane.setVisible(true);}

    @FXML private void statsScreen() {
        updateStats(Instant.now().getEpochSecond());
        statsPane.setVisible(true);
    }

    @FXML private void changeDarkMode() {
        darkMode = !darkMode;
        switchStylesheet();
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

    static class UpgradeButton {
        private int value;
        private long upgradeCost;

        public int getValue() {return value;}

        public void setValue(int value) {this.value = value;}

        public long getUpgradeCost() {return upgradeCost;}

        public void setUpgradeCost(long upgradeCost) {this.upgradeCost = upgradeCost;}
    }
}
