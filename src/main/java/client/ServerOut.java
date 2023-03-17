package client;

import static shared.Encryption.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

/** Sends commands from client to server. */
public class ServerOut implements Runnable {

  private final Socket serverSocket;
  private final PrintWriter out;
  private final BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
  private final Client client;

  protected Boolean running = true;

  /** Creates an instance of ServerOut */
  public ServerOut(Socket serverSocket, Client client) throws IOException {
    this.serverSocket = serverSocket;
    this.out = new PrintWriter(this.serverSocket.getOutputStream(), true);
    this.client = client;
  }

  /** From the Runnable interface. Runs the ServerOut thread to send commands to the server */
  @Override
  public void run() {
    try {
      while (this.running) {
        System.out.print("> ");
        String command = this.keyboard.readLine();

        if (command == null) {
          System.out.println("[CLIENT] Keyboard: command is null");
        } else {
          this.handleCommand(command);
        }
      }
      try {
        this.serverSocket.close();
        this.out.close();
      } catch (IOException e) {
        System.err.println(e.getMessage());
      }
    } catch (IOException e) {
      System.err.println("[ServerOut]: " + e.getMessage());
      e.printStackTrace();
    }
    // Close the socket and the input stream
    try {
      this.serverSocket.close();
      this.keyboard.close();
    } catch (IOException e) {
      System.err.println(
          "[CLIENT] Failed to close serverSocket and input stream: " + e.getMessage());
      e.printStackTrace();
    }
  }

  protected void sendToServer(String message) {
    this.out.println(encrypt(message));
  }

  private void handleCommand(String command) {
    String commandSymbol = ClientProtocol.COMMAND_SYMBOL.toString();

    if (command.startsWith(commandSymbol)) {
      int firstSpace = command.indexOf(" ");
      if (firstSpace == -1) {
        firstSpace = command.length();
      }

      try {
        ClientProtocol protocol =
            ClientProtocol.valueOf(
                command.substring(1, firstSpace).replace(commandSymbol, "").toUpperCase());

        // If the command has no arguments
        if (firstSpace == command.length()) {
          switch (protocol) {
            case LOGOUT -> this.client.logout();
            case WHOAMI -> this.client.whoami();
          }
        } else {
          String[] args = command.substring(firstSpace + 1).split(" ");

          switch (protocol) {
            case BROADCAST -> this.client.sendMessageServer(String.join(" ", args));
            case WHISPER -> this.client.sendMessageClient(
                args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            case SEND_MESSAGE_LOBBY -> this.client.sendMessageLobby(String.join(" ", args));
            case SET_USERNAME -> this.client.setUsername(args[0].replaceAll(" ", "_"));
            case CREATE_LOBBY -> this.client.createLobby(args[0], args[1]);
            case JOIN_LOBBY -> this.client.joinLobby(args[0], args[1]);
          }
        }
      } catch (IllegalArgumentException e) {
        System.out.println("ServerOut: command " + command + " not recognized");
      }

    } else {
      System.out.println("[CLIENT] ServerOut: command does not start with command symbol");
    }
  }
}
