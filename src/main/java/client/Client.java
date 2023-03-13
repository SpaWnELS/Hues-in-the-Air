package client;

import server.ServerProtocol;

import java.net.Socket;
import java.io.IOException;


public class Client {
    // Server info
    private static int SERVER_PORT;
    private static String SERVER_IP;
    private Socket socket;

    // Input and output streams
    private ServerIn inputSocket;
    private ServerOut outputSocket;
    private Thread inputThread;
    private Thread outputThread;

    // Username
    protected String username = System.getProperty("user.name");

    public void run(String[] args) throws IOException {
        start(args);
        this.connectToServer();

        this.inputSocket = new ServerIn(socket, this);
        this.outputSocket = new ServerOut(socket, this);

        this.inputThread = new Thread(this.inputSocket);
        this.outputThread = new Thread(this.outputSocket);

        inputThread.start();
        outputThread.start();

        System.out.println("[CLIENT] Connection to server established");
    }

    public static void start(String[] args) {
        System.out.println("Starting client...");
        String[] serverInfo = args[0].split(":");
        if (serverInfo.length != 2) {
            System.err.println("Start the client with the following format: java Client <serverIP>:<serverPort>");
        }

        // Set server IP
        if (serverInfo[0].equals("localhost")) SERVER_IP = "127.0.0.1";
        else SERVER_IP = serverInfo[0];

        // Set server port
        try {
            SERVER_PORT = Integer.parseInt(serverInfo[1]);
        }
        catch (NumberFormatException e) {
            System.err.println("Start the client with the following format: java Client <serverIP>:<serverPort>");
        }

    }
    private void connectToServer() {
        this.socket = new Socket();
        // Try to connect to the server, if connection is refused, retry every 2 seconds
        while (!this.socket.isConnected()) {
            try {
                this.socket = new Socket(SERVER_IP, SERVER_PORT);
            } catch (IOException e) {
                System.err.println("[CLIENT] Connection timed out. Retrying...");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void setUsername(String username) {
        String command = ClientProtocol.SET_USERNAME.toString() + ServerProtocol.SEPARATOR + username;
        this.outputSocket.sendToServer(command);
        this.username = username;
    }

    protected void sendMessageServer(String message) {
        String command = ServerProtocol.SEND_MESSAGE_SERVER.toString() + ServerProtocol.SEPARATOR + this.username + ServerProtocol.SEPARATOR + message;
        this.outputSocket.sendToServer(command);
    }

    protected void logout() {
        // Communicate with server that client is logging out
        // TODO: solve SocketException when logging out
        String command = ClientProtocol.LOGOUT.toString();
        this.outputSocket.sendToServer(command);

        // Close the socket and stop the threads
        this.inputSocket.running = false;
        this.outputSocket.running = false;
        this.inputThread.interrupt();
        this.outputThread.interrupt();
        try {
            this.socket.close();
        } catch (IOException e) {
            System.err.println("[CLIENT] Failed to close socket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This client wants to send a private message to another client.
     * <p>
     *     Protocol format: SEND_MESSAGE_CLIENT<SEPARATOR>sender.username<SEPARATOR>receiver.username<SEPARATOR>message
     * </p>
     * */
    public void sendMessageClient(String receiverName, String message) {
        String command = ServerProtocol.SEND_MESSAGE_CLIENT.toString() + ServerProtocol.SEPARATOR + this.username + ServerProtocol.SEPARATOR + receiverName + ServerProtocol.SEPARATOR + message;
        this.outputSocket.sendToServer(command);
    }
}