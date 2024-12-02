import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.util.prefs.Preferences;

/**
 * Jordle game implementation using JavaFX.
 *
 * @author Nawaf Alturayif
 * @version 1.0
 */
public class Jordle extends Application {
    private Backend backend;
    private Label statusLabel;
    private GridPane gameGrid;
    private int currentRow = 0;
    private int currentCol = 0;
    private Stage primaryStage;
    private Preferences prefs;
    private int totalGames = 0;
    private int gamesWon = 0;
    private double winPercentage = 0;
    private int currentStreak = 0;
    private int maxStreak = 0;
    private boolean isDarkMode = false;
    private MediaPlayer backgroundMusicPlayer;
    private MediaPlayer correctSoundPlayer;
    private MediaPlayer incorrectSoundPlayer;

    /**
     * Main method to launch the Jordle game.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Starts the Jordle game application.
     *
     * @param primaryStage The primary stage for the application
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Jordle");

        prefs = Preferences.userNodeForPackage(Jordle.class);
        loadStatistics();
        
        setupMediaPlayers();

        Scene welcomeScene = createWelcomeScene();
        primaryStage.setScene(welcomeScene);
        primaryStage.show();

        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusicPlayer.play();
        }
    }

    /**
     * Sets up media players for sound effects and background music.
     */
    private void setupMediaPlayers() {
        try {
            Media backgroundMusic = new Media(new File("background_music.mp3").toURI().toString());
            backgroundMusicPlayer = new MediaPlayer(backgroundMusic);

            Media correctSound = new Media(new File("correct_sound.mp3").toURI().toString());
            correctSoundPlayer = new MediaPlayer(correctSound);

            Media incorrectSound = new Media(new File("incorrect_sound.mp3").toURI().toString());
            incorrectSoundPlayer = new MediaPlayer(incorrectSound);
        } catch (Exception e) {
            System.err.println("Could not load media files: " + e.getMessage());
        }
    }

    /**
     * Creates the welcome scene with theme toggle and statistics.
     *
     * @return The welcome scene
     */
    private Scene createWelcomeScene() {
        VBox welcomeLayout = new VBox(20);
        welcomeLayout.setAlignment(Pos.CENTER);
        welcomeLayout.setPadding(new Insets(50));
        welcomeLayout.setStyle(getThemeStyle());

        Label titleLabel = new Label("Jordle");
        titleLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: green;");

        ImageView backgroundImage = loadBackgroundImage();

        HBox bottomBox = new HBox(20);
        bottomBox.setAlignment(Pos.CENTER);

        Button playButton = new Button("Play");
        playButton.setStyle("-fx-font-size: 18px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 20;");
        playButton.setOnAction(e -> showGameScene());

        ToggleButton themeToggle = new ToggleButton(isDarkMode ? "Light Mode" : "Dark Mode");
        themeToggle.setStyle("-fx-font-size: 18px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 20;");
        themeToggle.setOnAction(e -> toggleTheme(themeToggle));

        VBox statsBox = createStatisticsDisplay();

        bottomBox.getChildren().addAll(statsBox,themeToggle, playButton);


        welcomeLayout.getChildren().addAll(titleLabel, backgroundImage, bottomBox);

        return new Scene(welcomeLayout, 600, 800);
    }

    private void showGameScene() {
        backend = new Backend();
        VBox gameLayout = new VBox(20);
        gameLayout.setAlignment(Pos.CENTER);
        gameLayout.setPadding(new Insets(20));

        Label titleLabel = new Label("Jordle");
        titleLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: green;");

        gameGrid = createGameGrid();

        HBox buttonBox = createGameButtons();

        gameLayout.getChildren().addAll(titleLabel,gameGrid, buttonBox);
        gameLayout.setStyle(getThemeStyle());

        Scene gameScene = new Scene(gameLayout, 600, 800);
        gameScene.setOnKeyPressed(this::handleKeyPress);

        gameGrid.requestFocus();


        primaryStage.setScene(gameScene);
    }
    
    /**
     * Creates the game grid for letter input and display.
     *
     * @return GridPane representing the game grid
     */
    private GridPane createGameGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                Label cell = new Label();
                cell.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-min-width: 60; -fx-min-height: 60; -fx-alignment: center; -fx-background-color: white;");
                grid.add(cell, col, row);
            }
        }

        return grid;
    }

    /**
     * Creates game control buttons (Instructions and Restart).
     *
     * @return HBox containing game control buttons
     */
    private HBox createGameButtons() {
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

         // Status Label
        statusLabel = new Label("Try guessing a word!");
        statusLabel.setStyle("-fx-font-size: 18px;");
        
        Button instructionsButton = new Button("Instructions");
        instructionsButton.setStyle("-fx-font-size: 18px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 20;");
        instructionsButton.setOnAction(e -> showInstructions());

        Button restartButton = new Button("Restart");
        restartButton.setStyle("-fx-font-size: 18px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 20;");
        restartButton.setOnAction(e -> restartGame());

        buttonBox.getChildren().addAll(statusLabel, restartButton, instructionsButton);
        return buttonBox;
    }

    
    /**
     * Handles key press events during the game.
     *
     * @param event Key press event
     */
    private void handleKeyPress(KeyEvent event) {
        if (currentRow >= 6) return;


        switch (event.getCode()) {
            case BACK_SPACE:
                handleBackspace();
                break;
            case ENTER:
                handleEnter();
                break;
            default:
                handleLetterInput(event);
        }
    }

    /**
     * Handles backspace key press to remove letters.
     */
    private void handleBackspace() {
        if (currentCol > 0) {
            currentCol--;
            Label cell = (Label) gameGrid.getChildren().get(currentRow * 5 + currentCol);
            cell.setText("");
        }
    }

    /**
     * Handles letter input for the game grid.
     *
     * @param event Key press event
     */
    private void handleLetterInput(KeyEvent event) {
        String input = event.getText().toLowerCase();
        if (input.matches("[a-z]") && currentCol < 5) {
            Label cell = (Label) gameGrid.getChildren().get(currentRow * 5 + currentCol);
            cell.setText(input.toUpperCase());
            currentCol++;
        }
    }

    /**
     * Handles enter key press to evaluate guess.
     */
    private void handleEnter() {
        if (currentCol != 5) {
            showAlert("Invalid Guess", "Please enter a 5-letter word.");
            return;
        }

        String guess = getCurrentRowGuess();
        try {
            String result = backend.check(guess);
            updateGridColors(result);
            checkGameStatus(result);
        } catch (InvalidGuessException e) {
            showAlert("Invalid Guess", e.getMessage());
        }
    }

    /**
     * Gets the current row's guess from the game grid.
     *
     * @return The current row's guess as a string
     */
    private String getCurrentRowGuess() {
        StringBuilder guess = new StringBuilder();
        for (int col = 0; col < 5; col++) {
            Label cell = (Label) gameGrid.getChildren().get(currentRow * 5 + col);
            guess.append(cell.getText());
        }
        return guess.toString();
    }

    /**
     * Updates grid cell colors based on guess correctness.
     *
     * @param result Result string from Backend's check method
     */
    private void updateGridColors(String result) {
        for (int col = 0; col < 5; col++) {
            Label cell = (Label) gameGrid.getChildren().get(currentRow * 5 + col);
            switch (result.charAt(col)) {
                case 'g':
                    cell.setStyle("-fx-background-color: green; -fx-text-fill: white; -fx-border-color: black; -fx-border-width: 2; -fx-min-width: 60; -fx-min-height: 60; -fx-alignment: center;");
                    break;
                case 'y':
                    cell.setStyle("-fx-background-color: yellow; -fx-border-color: black; -fx-border-width: 2; -fx-min-width: 60; -fx-min-height: 60; -fx-alignment: center;");
                    break;
                case 'i':
                    cell.setStyle("-fx-background-color: grey; -fx-text-fill: white; -fx-border-color: black; -fx-border-width: 2; -fx-min-width: 60; -fx-min-height: 60; -fx-alignment: center;");
                    break;
            }
        }
        currentRow++;
        currentCol = 0;
    }

    /**
     * Shows game instructions in a separate window.
     */
    private void showInstructions() {
        Stage instructionsStage = new Stage();
        instructionsStage.initModality(Modality.APPLICATION_MODAL);
        instructionsStage.setTitle("Jordle Instructions");

        VBox instructionsLayout = new VBox(20);
        instructionsLayout.setPadding(new Insets(20));
        instructionsLayout.setAlignment(Pos.CENTER);

        Label instructionsTitle = new Label("How to Play Jordle");
        instructionsTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label instructions = new Label(
            "1. Guess a 5-letter word within 6 attempts.\n" +
            "2. After each guess, the color of the tiles will change:\n" +
            "   - Green: Correct letter in the correct position\n" +
            "   - Yellow: Correct letter in the wrong position\n" +
            "   - Grey: Letter not in the word\n" +
            "3. Use backspace to delete letters.\n" +
            "4. Press enter to submit your guess.\n" +
            "5. Try to guess the word in as few attempts as possible!"
        );
        instructions.setStyle("-fx-font-size: 14px;");

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> instructionsStage.close());

        instructionsLayout.getChildren().addAll(instructionsTitle, instructions, closeButton);

        Scene instructionsScene = new Scene(instructionsLayout, 400, 400);
        instructionsStage.setScene(instructionsScene);
        instructionsStage.show();
        gameGrid.requestFocus();

    }

    /**
     * Shows an alert dialog with specified title and message.
     *
     * @param title Alert dialog title
     * @param message Alert dialog message
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Loads the background image for the welcome scene.
     *
     * @return ImageView containing the Jordle background image
     */
    private ImageView loadBackgroundImage() {
        try {
            Image image = new Image(new FileInputStream("jordleImage.jpg"));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(400);
            imageView.setFitHeight(300);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (Exception e) {
            System.err.println("Could not load background image: " + e.getMessage());
            return new ImageView(); // Return empty ImageView if image fails
        }
    }

    /**
     * Creates a statistics display box.
     *
     * @return VBox containing game statistics
     */
    private VBox createStatisticsDisplay() {
        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER);

        Label statsTitle = new Label("Game Statistics");
        statsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");

        Label totalGamesLabel = new Label("Total Games: " + totalGames);
        totalGamesLabel.setStyle("-fx-text-fill: green;");

        Label gamesWonLabel = new Label("Games Won: " + gamesWon);
        gamesWonLabel.setStyle("-fx-text-fill: green;");

        Label winPercentageLabel = new Label("Win Percentage: " + String.format("%.1f%%", winPercentage));
        winPercentageLabel.setStyle("-fx-text-fill: green;");

        Label currentStreakLabel = new Label("Current Streak: " + currentStreak);
        currentStreakLabel.setStyle("-fx-text-fill: green;");

        Label maxStreakLabel = new Label("Max Streak: " + maxStreak);
        maxStreakLabel.setStyle("-fx-text-fill: green;");


        statsBox.getChildren().addAll(statsTitle, totalGamesLabel, gamesWonLabel, 
                                      winPercentageLabel, currentStreakLabel, maxStreakLabel);
        
        return statsBox;
    }

    /**
     * Toggles between light and dark themes.
     *
     * @param themeToggle The theme toggle button
     */
    private void toggleTheme(ToggleButton themeToggle) {
        isDarkMode = !isDarkMode;
        themeToggle.setText(isDarkMode ? "Light Mode" : "Dark Mode");
        
        primaryStage.getScene().getRoot().setStyle(getThemeStyle());
    }

    /**
     * Gets the current theme style based on mode.
     *
     * @return CSS style string for current theme
     */
    private String getThemeStyle() {
        return isDarkMode 
            ? "-fx-background-color: #333333; -fx-text-fill: white;" 
            : "-fx-background-color: #FFFFFF; -fx-text-fill: black;";
    }

    /**
     * Shows a result popup with an image and message.
     *
     * @param isWin Whether the game was won
     */
    private void showResultPopup(boolean isWin) {
        Stage resultStage = new Stage();
        resultStage.initModality(Modality.APPLICATION_MODAL);
        resultStage.setTitle(isWin ? "Congratulations!" : "Game Over");

        VBox resultLayout = new VBox(20);
        resultLayout.setAlignment(Pos.CENTER);
        resultLayout.setPadding(new Insets(20));

        String imagePath = isWin ? "win_image.jpg" : "lose_image.jpg";
        ImageView resultImage = new ImageView(new Image(new File(imagePath).toURI().toString()));
        resultImage.setFitWidth(200);
        resultImage.setFitHeight(200);
        resultImage.setPreserveRatio(true);

        Label resultMessage = new Label(isWin 
            ? "Congratulations! You guessed the word!" 
            : "Better luck next time! The word was " + backend.getTarget());
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> resultStage.close());

        resultLayout.getChildren().addAll(resultImage, resultMessage, closeButton);

        Scene resultScene = new Scene(resultLayout, 300, 400);
        resultStage.setScene(resultScene);
        resultStage.show();

        if (isWin && correctSoundPlayer != null) {
            correctSoundPlayer.play();
        } else if (!isWin && incorrectSoundPlayer != null) {
            incorrectSoundPlayer.play();
        }
        gameGrid.requestFocus();

    }

    /**
     * Updates game statistics after each game.
     *
     * @param isWin Whether the game was won
     */
    private void updateStatistics(boolean isWin) {
        totalGames++;
        if (isWin) {
            gamesWon++;
            currentStreak++;
            maxStreak = Math.max(currentStreak, maxStreak);
        } else {
            currentStreak = 0;
        }
        
        winPercentage = (double) gamesWon / totalGames * 100;

        prefs.putInt("totalGames", totalGames);
        prefs.putInt("gamesWon", gamesWon);
        prefs.putInt("currentStreak", currentStreak);
        prefs.putInt("maxStreak", maxStreak);
    }

    /**
     * Loads saved game statistics.
     */
    private void loadStatistics() {
        totalGames = prefs.getInt("totalGames", 0);
        gamesWon = prefs.getInt("gamesWon", 0);
        currentStreak = prefs.getInt("currentStreak", 0);
        maxStreak = prefs.getInt("maxStreak", 0);
        
        winPercentage = totalGames > 0 ? (double) gamesWon / totalGames * 100 : 0;
    }

    /**
     * Checks game status after each guess.
     *
     * @param result Result string from Backend's check method
     */
    private void checkGameStatus(String result) {
        if (result.equals("ggggg")) {
            statusLabel.setText("Congratulations! You've guessed the word!");
            updateStatistics(true);
            showResultPopup(true);
        } else if (currentRow >= 6) {
            statusLabel.setText("Game over. The word was " + backend.getTarget() + ".");
            updateStatistics(false);
            showResultPopup(false);
        }
    }

    /**
     * Restarts the game by resetting grid and backend.
     */
    private void restartGame() {
        backend.reset();
        currentRow = 0;
        currentCol = 0;
        statusLabel.setText("Try guessing a word!");

        // Reset grid colors and text
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                Label cell = (Label) gameGrid.getChildren().get(row * 5 + col);
                cell.setText("");
                cell.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-min-width: 60; -fx-min-height: 60; -fx-alignment: center; -fx-background-color: white;");
            }
        }
        gameGrid.requestFocus();
    }
}
