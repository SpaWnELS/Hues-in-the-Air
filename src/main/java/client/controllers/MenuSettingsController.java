package client.controllers;

import client.Client;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

/** The controller for the menu window. */
public class MenuSettingsController {
  @FXML private VBox settingsTab;

  @FXML private ToggleButton buttonMusic;
  @FXML private Button buttonPen;
  @FXML private ToggleButton buttonSound;

  @FXML private Label labelMusic;
  @FXML private Label labelSfx;
  @FXML private Label labelUsername;

  @FXML private TextField textUsername;
  @FXML private Slider sliderMusic;
  @FXML private Slider sliderSfx;

  @FXML private TextArea creditsArea;

  // Sound
  private double lastMusicVolume = 1;
  private double lastSfxVolume = 1;

  /**
   * Initializes the controller class. This method is automatically called after the fxml file has
   * been loaded.
   */
  public void initialize() {
    this.setFontBehaviour();

    this.setInteractions();

    this.setCredits();

    this.setUsernameField();

    this.setButtonBehaviour();
  }

  /**
   * Fills the username field with the username of the current user.
   */
  public void setUsernameField() {
    this.textUsername.setText(Client.getInstance().getUsername());
  }
  /**
   * Sets the text of the credits area to the credits text.
   * */
  private void setCredits() {
    String credits =
        """
            The JEJN Team Presents:
            Designed by humans, tested by robots, loved by gamers: Hues in the Air!

            Starring:
            Jiri (the Cube Whisperer), Eliot (the Java Magician), Jennifer (the Level Designer Extraordinaire) and Nils (the Block Master).

            Special Thanks to:
            The Coffee Beans, for keeping us awake during those all-nighters
            The Pizza Delivery Guy, for always knowing when we needed a break
            The Stack Overflow community, for having all the answers, always, to everything.

            And Finally, a Big Shoutout to:
            YOU, for playing Hues in the Air and making our coding dreams come true!
            Thank you for playing!""";
    this.creditsArea.setText(credits);
  }

  /**
   * Binds the font sizes of various elements to the width of the tab pane.
   */
  private void setFontBehaviour() {
    this.labelMusic
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(20)));
    this.labelSfx
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(20)));
    this.labelUsername
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(20)));
    this.textUsername
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(35)));
    this.buttonMusic
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(30)));
    this.buttonSound
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(30)));
    this.buttonPen
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(30)));
    this.creditsArea
        .styleProperty()
        .bind(Bindings.concat("-fx-font-size: ", this.settingsTab.widthProperty().divide(40)));
  }

  /**
   * Binds the interactions of various elements in the pane to the corresponding methods in client.
   * This includes changing the username, toggling music on and off...
   */
  private void setInteractions() {
    // Username field
    this.textUsername.setOnKeyPressed(
        e -> {
          if (e.getCode() == KeyCode.ENTER) {
            this.textUsername.setText(this.textUsername.getText().trim());
            if (!this.textUsername.getText().isEmpty()) {
              Client.getInstance().setUsername(this.textUsername.getText().replaceAll(" ", "_"));
              this.setUsernameField();
              this.settingsTab.requestFocus();
            }
          }
        });

    // Music and SFX sliders
    this.sliderMusic
        .valueProperty()
        .addListener(
            (observable, oldValue, newValue) ->
                Client.getInstance().setMusicVolume(newValue.doubleValue()));
    this.sliderSfx
        .valueProperty()
        .addListener(
            (observable, oldValue, newValue) ->
                Client.getInstance().setSfxVolume(newValue.doubleValue()));

    this.sliderSfx.setValue(1);
    this.sliderMusic.setValue(1);
  }

  /**
   * Sets the behaviour of the buttons in the settings pane. This includes playing a sound when the
   * mouse hovers over the button and what the buttons do when they are clicked.
   */
  private void setButtonBehaviour() {
    buttonPen.setOnMouseEntered(e -> Client.getInstance().clickSound());
    buttonMusic.setOnMouseEntered(e -> Client.getInstance().clickSound());
    buttonSound.setOnMouseEntered(e -> Client.getInstance().clickSound());

    this.buttonMusic
        .selectedProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (sliderMusic.getValue() == 0) {
                sliderMusic.setValue(lastMusicVolume);
                buttonMusic.setText("\uD83D\uDD0A");
              } else {
                lastMusicVolume = sliderMusic.getValue();
                sliderMusic.setValue(0);
                buttonMusic.setText("\uD83D\uDD07");
              }
            });

    this.buttonSound
        .selectedProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (sliderSfx.getValue() == 0) {
                sliderSfx.setValue(lastSfxVolume);
                buttonSound.setText("\uD83D\uDD0A");
              } else {
                lastSfxVolume = sliderSfx.getValue();
                sliderSfx.setValue(0);
                buttonSound.setText("\uD83D\uDD07");
              }
            });

    // Pen button
    this.buttonPen.setOnAction(
        e -> {
          this.textUsername.clear();
          this.textUsername.requestFocus();
        });
  }
}
