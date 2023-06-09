package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import javafx.application.Platform;
import server.ServerProtocol;

/** Handles the input from the server. */
public class ServerIn implements Runnable {

  private final Socket serverSocket;
  private final BufferedReader in;
  private final Client client;

  /** Used for the while loop in the run method. */
  protected Boolean running = true;

  /**
   * Creates an instance of ServerConnection.
   *
   * @param serverSocket The socket to the server
   * @param client The client which has created this instance
   * @throws IOException If an I/O error occurs when creating the input stream, the socket is
   *     closed.
   */
  public ServerIn(Socket serverSocket, Client client) throws IOException {
    this.serverSocket = serverSocket;
    this.in = new BufferedReader(new InputStreamReader(this.serverSocket.getInputStream()));
    this.client = client;
  }

  /** From the Runnable interface. Runs the ServerIn thread to receive commands from the server */
  @Override
  public void run() {
    try {
      while (this.running) {
        String[] command = this.receiveFromServer();
        if (command != null) {
          this.protocolSwitch(command);
        }
      }
      this.serverSocket.close();
      this.in.close();
    } catch (IOException e) {
      System.err.println("[ServerIn]: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Receives a command from the server via the socket and returns it as a string array. If the
   * received string is null, the method returns null. This however only happens when the connection
   * to the server is lost. Called by {@link #run()}.
   */
  private String[] receiveFromServer() throws IOException {
    try {
      String input = this.in.readLine();
      if (input != null) {
        return input.split(ServerProtocol.SEPARATOR.toString());
      } else {
        this.client.receivedNullCounter++;
        // Received null from server
        return null;
      }
    } catch (SocketException e) {
      // Connection to server lost
      this.client.exit();
      return null;
    }
  }

  /**
   * Client receives a command from the server and runs the appropriate method.
   *
   * <p>The command is split into an array of strings. The first string is the protocol and the rest
   * of the strings are the arguments. See {@link ServerProtocol} for possible protocols.
   */
  private void protocolSwitch(String[] command) {
    try {
      Platform.runLater(
          () -> {
            ServerProtocol protocol = ServerProtocol.valueOf(command[0]);

            if (protocol.getNumArgs() == command.length - 1) {
              switch (protocol) {
                case SEND_PUBLIC_MESSAGE -> this.receiveMessage(
                    Arrays.copyOfRange(command, 1, command.length), "Public");
                case SEND_PRIVATE_MESSAGE -> this.receiveMessage(
                    Arrays.copyOfRange(command, 1, command.length), "Private");
                case SEND_LOBBY_MESSAGE -> this.receiveMessage(
                    Arrays.copyOfRange(command, 1, command.length), "Lobby");
                case NO_USER_FOUND -> this.client.noUserFound(command[1]);
                case SERVER_PING -> this.client.pong();
                case SERVER_PONG -> this.resetClientStatus();
                case USERNAME_SET_TO -> this.client.usernameSetTo(command[1]);
                case LOBBY_JOINED -> this.client.enterLobby(command[1]);
                case LOBBY_EXITED -> this.client.lobbyExited(command[1]);
                case UPDATE_FULL_LIST -> this.client.updateLobbyInfo(command[1]);
                case UPDATE_CLIENT_LIST -> this.client.updateClientInfo(command[1]);
                case UPDATE_LOBBY_LIST -> this.client.updateLobbyList(command[1]);
                case UPDATE_GAME_LIST -> this.client.updateGameList(command[1]);
                case TOGGLE_READY_STATUS -> this.client.setToggleReady(Boolean.parseBoolean(command[1]));
                case START_GAME -> {
                  try {
                    this.client.loadGameScreen();
                  } catch (IOException ex) {
                    this.client.LOGGER.error("Couldn't load lobby screen. Shutting down.");
                    this.client.exit();
                  }
                }
                case POSITION_UPDATE -> this.client
                    .gameController
                    .getGame()
                    .updatePosition(command[1], command[2], command[3], command[4], command[5]);
                case JUMP_UPDATE -> this.client.gameController.getGame().updateJump(command[1], command[2]);
                case GAME_STATUS_UPDATE -> this.client.gameController.updateGameStatus(command[1], command[2]);
                case LOAD_LEVEL -> this.client.loadLevel(command[1]);
                case SEND_CRITICAL_BLOCKS -> {
                  if (this.client.gameController != null) {
                    this.client.gameController.setBlockColours(command[1]);
                  }
                }
                case GAME_ENDED -> {
                  try {
                    this.client.loadLobbyScreen();
                  } catch (IOException e) {
                    client.LOGGER.error("Couldn't load lobby screen. Shutting down.");
                    client.exit();
                  }
                }
                default -> client.LOGGER.error("ServerIn: Unknown protocol: " + command[0]);
              }
            }
          });
    } catch (IllegalArgumentException e) {
      System.out.println("ServerIn: Unknown protocol: " + command[0]);
    }
  }

  /**
   * Resets the client status to "connected to server" and "noAnswerCounter" to 0. Called by {@link
   * #protocolSwitch(String[])} upon receiving a PONG from the server.
   */
  private void resetClientStatus() {
    this.client.serverHasPonged = true;
    this.client.noAnswerCounter = 0;
  }

  /**
   * Prints a message received from another client to the console by formatting it. Called by {@link
   * #protocolSwitch(String[])}.
   */
  private void receiveMessage(String[] command, String privacy) {
    String sender = command[0];
    String message = String.join(" ", Arrays.copyOfRange(command, 1, command.length));
    this.client.receiveMessage(message, sender, privacy);
  }
}
