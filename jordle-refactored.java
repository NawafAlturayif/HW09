import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import java.io.File;
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
    private Stage mainStage;
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
     * @param stage The primary stage for the application
     */
    @Override
    public void start(Stage stage) {
        this.mainStage = stage;
        mainStage.setTitle("Jordle");

        prefs = Preferences.userNodeForPackage(Jordle.class);
        loadStatistics();

        setupMediaPlayers();

        Scene welcomeScene = createWelcomeScene();
        mainStage.setScene(welcomeScene);
        mainStage.show();

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
            Media backgroundMusic =
                new Media(new File("background_music.mp3").toURI().toString());
            backgroundMusicPlayer = new MediaPlayer(backgroundMusic);

            Media correctSound =
                new Media(new File("correct_sound.mp3").toURI().toString());
            correctSoundPlayer = new MediaPlayer(correctSound);

            Media incorrectSound =
                new Media(new File("incorrect_sound.mp3").toURI().toString());
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
        titleLabel.setStyle(
            "-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: green;"
        );

        ImageView backgroundImage = loadBackgroundImage();

        HBox bottomBox = new HBox(20);
        bottomBox.setAlignment(Pos.CENTER);

        Button playButton = new Button("Play");
        playButton.setStyle(
            "-fx-font-size: 18px; -fx-background-color: #4CAF50; "
            + "-fx-text-fill: white; -fx-background-radius: 20;"
        );
        playButton.setOnAction(e -> showGameScene());

        ToggleButton themeToggle = new ToggleButton(
            isDarkMode ? "Light Mode" : "Dark Mode"
        );
        themeToggle.setStyle(
            "-fx-font-size: 18px; -fx-background-color: #4CAF50; "
            + "-fx-text-fill: white; -fx-background-radius: 20;"
        );
        themeToggle.setOnAction(e -> toggleTheme(themeToggle));

        VBox statsBox = createStatisticsDisplay();

        bottomBox.getChildren().addAll(statsBox, themeToggle, playButton);

        welcomeLayout.getChildren().addAll(titleLabel, backgroundImage, bottomBox);

        return new Scene(welcomeLayout, 600, 800);
    }

    /**
     * Handles key press events during the game.
     *
     * @param event Key press event
     */
    private void handleKeyPress(KeyEvent event) {
        if (currentRow >= 6) {
            return;
        }

        switch (event.getCode()) {
        case BACK_SPACE:
            handleBackspace();
            break;
        case ENTER:
            handleEnter();
            break;
        default:
            handleLetterInput(event);
            break;
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
     * Toggles between light and dark themes.
     *
     * @param themeToggle The theme toggle button
     */
    private void toggleTheme(ToggleButton themeToggle) {
        isDarkMode = !isDarkMode;
        themeToggle.setText(isDarkMode ? "Light Mode" : "Dark Mode");

        mainStage.getScene().getRoot().setStyle(getThemeStyle());
    }


}
