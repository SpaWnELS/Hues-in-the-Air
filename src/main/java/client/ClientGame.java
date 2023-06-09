package client;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import game.Block;
import game.Colours;
import game.GameConstants;
import game.Level;
import game.Vector2D;
import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/** The game class which the client uses to handle the game logic. */
public class ClientGame {
  /** The keys that are pressed. */
  public HashMap<KeyCode, Boolean> keys = new HashMap<>();

  private final Pane appRoot;
  private Pane gameRoot;
  private Vector2D playerScreenPosition;
  private double screenScale = 1;

  /** Whether a jump request has been sent to the server or not. */
  public boolean jumpRequestSent = false;

  private final Client client;

  /** The level that is currently being played. */
  public Level level;
  private ClientCube player;

  private ControllerManager controllers;

  /**
   * Creates a new game.
   *
   * @param client the client that is playing the game
   * @param backgroundPane the pane on which the game is displayed
   */
  public ClientGame(Client client, Pane backgroundPane) {
    this.client = client;
    this.appRoot = backgroundPane;
  }

  /**
   * Called every frame and handles the game logic.
   *
   * @param dt the time between the last frame and the current frame
   */
  public void update(double dt) {
    // Possibility to add a pause method
    ControllerState currState = controllers.getState(0);

    if (currState.isConnected) {
      if (currState.aJustPressed || currState.bJustPressed || currState.xJustPressed || currState.yJustPressed) {
        client.sendGameCommand(ClientProtocol.SPACE_BAR_PRESSED.toString());
      }
    }
    this.gameUpdate(dt);
  }

  /** The update method that is called if the game is not paused. Handles the game logic. */
  private void gameUpdate(double dt) {
    this.analyseKeys();

    if (player != null) {
      Block[] neighbourBlocks =
          this.level.getNeighbourBlocks(player.getPosition().getX(), player.getPosition().getY());
      player.move(neighbourBlocks, dt);
    }
  }

  /**
   * Called every frame. If the key ESCAPE is pressed, the game is paused. Otherwise, the game logic
   * is handled.
   */
  private void analyseKeys() {
    // Possibility to add a pause method
    if (spaceBarPressed()) {
      if (!jumpRequestSent) {
        client.sendGameCommand(ClientProtocol.SPACE_BAR_PRESSED.toString());
        jumpRequestSent = true;
      }
    }
  }
  /**
   * update the position of the player
   *
   * @param positionX - the x component of the cube's position
   * @param positionY - the y component of the cube's position
   * @param velocityX - the x component of the cube's velocity
   * @param velocityY - the y component of the cube's velocity
   * @param accelerationAngle - the angle of the acceleration
   */
  protected void updatePosition(
      String positionX,
      String positionY,
      String velocityX,
      String velocityY,
      String accelerationAngle) {
    player.setPositionTo(Double.parseDouble(positionX), Double.parseDouble(positionY));
    player.setVelocityTo(Double.parseDouble(velocityX), Double.parseDouble(velocityY));
    player.onlySetAccelerationAngle(Integer.parseInt(accelerationAngle));
  }

  /** Returns whether a key has been pressed by the user or not. */
  private boolean spaceBarPressed() {
    return keys.getOrDefault(KeyCode.SPACE, false);
  }

  /**
   * Initialises the content of the game, Loads the level data and creates the platforms Creates the
   * player Creates the stars Will create the coin to finish the game.
   */
  public void initialiseContent() {
    gameRoot = new Pane();

    // The camera is always centered on the player (middle of the screen)
    playerScreenPosition =
        new Vector2D(
            this.appRoot.getWidth() / 2,
            this.appRoot.getHeight() / 2); // Sets the player's position
    this.appRoot
        .widthProperty()
        .addListener(
            (obs, old, newValues) -> this.setGameRootScale());
    this.appRoot
        .heightProperty()
        .addListener(
            (obs, old, newValues) -> this.setGameRootScale());

    Rectangle bg =
        new Rectangle(
            this.gameRoot.getWidth(), this.gameRoot.getHeight()); // Creates the background
    bg.setFill(Colours.BLACK.getHex()); // Sets the background colour
    appRoot.getChildren().addAll(bg, gameRoot); // Adds the background and gameRoot to the appRoot
  }

  /**
   * The client has received the level path from the server and can now load the level.
   *
   * @param levelPath the path to the level to load from the resources folder
   */
  public void loadLevel(String levelPath) {
    this.gameRoot.getChildren().clear();

    this.level = new Level(levelPath, 50, gameRoot);
    this.client.requestCriticalBlocks();

    try {
      this.loadCoin();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Vector2D playerSpawn =
        new Vector2D(
            level.playerSpawnIdx[0] * level.blockWidth, level.playerSpawnIdx[1] * level.blockWidth);
    load_player(playerSpawn);

    // Scale level size to fit the screen
    this.setGameRootScale();
  }

  /** Loads the player */
  private void load_player(Vector2D spawnPosition) {
    player = new ClientCube(gameRoot, spawnPosition); // creates the player

    player
        .rectangle
        .translateXProperty()
        .addListener(
            (obs,
                old,
                newValue) -> { // Listens for changes in the player's x position and moves the
              // camera
              updateCameraPosition();
            });

    player
        .rectangle
        .translateYProperty()
        .addListener(
            (obs,
                old,
                newValue) -> { // Listens for changes in the player's y-position and moves the
              // camera
              updateCameraPosition();
            });

    player.blockSize = GameConstants.BLOCK_SIZE.getValue();
  }

  /**
   * Gets the coin position from the level and adds it to the game root. If no coin is found, the
   * coin is placed outside the level (top left corner).
   */
  private void loadCoin() throws IOException {
    URL coinURL = getClass().getResource("/images/Coin.png");
    assert coinURL != null;
    Image coinImage = new Image(coinURL.openStream());
    ImageView coin = new ImageView(coinImage);
    coin.setFitHeight(GameConstants.BLOCK_SIZE.getValue());
    coin.setFitWidth(GameConstants.BLOCK_SIZE.getValue());
    coin.setX(level.coinIdx[0] * level.blockWidth);
    coin.setY(level.coinIdx[1] * level.blockWidth);
    this.gameRoot.getChildren().add(coin);
  }

  /** Launches the application. */
  public void run() {
    this.initialiseContent();
    controllers =  new ControllerManager();
    controllers.initSDLGamepad();

    // Called every frame
    // Time since last frame in seconds
    AnimationTimer timer =
        new AnimationTimer() {
          long previousTime = System.nanoTime();
          final int FPS = 120;
          double dt;

          @Override
          public void handle(long now) { // Called every frame
            dt = (now - previousTime) * 1e-9; // Time since last frame in seconds

            if (dt > (double) 1 / FPS) {
              previousTime = now;

              update(dt);
            }
          }
        };

    timer.start();
  }

  /**
   * Set the colour of a block and its neighbours to a given colour.
   *
   * @param x the x index in the grid
   * @param y the y index in the grid
   * @param colour the colour to set the block to
   */
  public void setBlockColour(int x, int y, Color colour) {
    this.level.setNeighbourColours(x, y, colour);
  }

  /** The player has moved. Get the position at which the level should be drawn. */
  private void updateCameraPosition() {
    Vector2D offset = new Vector2D(player.rectangle.getTranslateX(), player.rectangle.getTranslateY());
    offset.multiplyInPlace(-screenScale);
    offset.addInPlace(playerScreenPosition);

    gameRoot.setLayoutX(offset.getX());
    gameRoot.setLayoutY(offset.getY());
  }

  /**
   * The cube has successfully jumped. The client has now just been informed of the coordinates of
   * the point around which the cube should rotate.
   *
   * @param rotationPointX the x coordinate of the rotation point
   * @param rotationPointY the y coordinate of the rotation point
   */
  public void updateJump(String rotationPointX, String rotationPointY) {
    player.rotationPoint =
        new Vector2D(Double.parseDouble(rotationPointX), Double.parseDouble(rotationPointY));
    player.jumping = true;
    player.rotating = true;
    player.canRotate = true;
  }

  /** Sets the game root size such that a fixed number of blocks are seen on the screen. */
  private void setGameRootScale() {
    screenScale = 1;

    if (this.level != null) {
      screenScale = (appRoot.getWidth()
          / GameConstants.BLOCKS_SEEN_HORIZONTAL.getValue()
          * level.getBlockWidth())
          / this.level.getPixelWidth();
    }

    Scale scale = new Scale(screenScale, screenScale, 0, 0);
    this.gameRoot.getTransforms().setAll(scale);

    this.playerScreenPosition =
        new Vector2D(
            this.appRoot.getWidth() / 2,
            this.appRoot.getHeight() / 2); // Sets the player's position
  }
}
