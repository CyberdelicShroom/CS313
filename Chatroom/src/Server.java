import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static java.lang.System.out;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections = new ArrayList<>();
    private ArrayList<String> clients = new ArrayList<>();
    private boolean done = false;
    private ExecutorService threadPool;
    private ServerSocket server;

    @Override
    public void run() {
        try {
            server = new ServerSocket(4000);
            System.out.println("Serving is running on port 4000");
            threadPool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                threadPool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }

    }

    public void bCast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void whisper(String whisper, String recipient, String sender) {
        for (ConnectionHandler ch : connections) {
            if (ch.username.equals(recipient)) {
                ch.whisperMessage(whisper, sender);
            }
            if (ch.username.equals(sender)) {
                ch.whisperMessage(whisper, sender);
            }
        }
    }

    public void shutdown() {
        done = true;
        threadPool.shutdown();
        if (!server.isClosed()) {
            try {
                server.close();
            } catch (IOException e) {
            }
        }
        for (ConnectionHandler ch : connections) {
            ch.shutdown();
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private DataInputStream in;
        private DataOutputStream out;
        private String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        public String requestUniqueName(String username, DataInputStream in) {
            try {
                while (clients.contains(username)) {
                    out.writeUTF("That username is already in use, try another: ");
                    username = in.readUTF();
                }
            } catch (IOException e) {
                
            }
            return username;
        }

        // Prints to user that called it and to the server
        public void printConnectedUsers() {
            System.out.println("\nConnected users:");
            try {
                out.writeUTF("\nConnected users:");
                for (String username : clients) {
                    System.out.println(username);
                    out.writeUTF(username);
                }
                System.out.println();

                out.writeUTF("");
            } catch (IOException e) {
                
            }
        }

        @Override
        public void run() {
            try {
                out = new DataOutputStream(client.getOutputStream());
                in = new DataInputStream(client.getInputStream());
                username = in.readUTF();
                
                if (username != null) {
                    if (clients.contains(username)) {
                        username = requestUniqueName(username, in);
                        clients.add(username);
                    } else {
                        clients.add(username);
                    }
                    System.out.println(username + " has connected.");
                    bCast(username + " has entered the chat.");
                    String message;
                    
                    while ((message = in.readUTF()) != null) {
                        if (message.startsWith("/name ")) {
                            String[] messageSplit = message.split(" ", 2);
                            String originalName = username;
                            if (messageSplit.length == 2) {
                                if (clients.contains(messageSplit[1])) {
                                    username = requestUniqueName(messageSplit[1], in);
                                    clients.add(username);
                                    int index = clients.indexOf(originalName);
                                    clients.remove(index);
                                } else {
                                    username = messageSplit[1];
                                    clients.add(username);
                                    int index = clients.indexOf(originalName);
                                    clients.remove(index);
                                }
                                bCast(originalName + " renamed themself to " + messageSplit[1]);
                                System.out.println(originalName + " renamed themself to " + messageSplit[1]);
                                out.writeUTF("Successfully changed username to: " + username);
                            } else {
                                out.writeUTF("No username provided.");
                            }

                        } else if (message.startsWith("/quit")) {
                            int index = clients.indexOf(username);
                            clients.remove(index);
                            System.out.println(username + " has disconnected.");
                            bCast(username + " has left the chat.");
                            shutdown();
                        } else if (message.startsWith("/users")) {
                            printConnectedUsers();
                        } else if (message.startsWith("/whisper ")) {
                            
                            String[] messageSplit = message.split(" ", 3);
                            String whisper = messageSplit[2];
                            String recipient = messageSplit[1];
                            if (clients.contains(recipient)) {
                                whisper(whisper, recipient, username);
                            } else {
                                out.writeUTF("'"+recipient+"'" + " does not exist in this chat room.");
                                printConnectedUsers();
                            }
                        } else {
                            bCast(username + ": " + message);
                        }

                    }

                }

            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            try {
                out.writeUTF(message);
            } catch (IOException e) {
                
            }
            
        }

        public void whisperMessage(String whisper, String sender) {
            try {
                out.writeUTF(sender + " (whisper): " + whisper);
            } catch (IOException e) {
                
            }
            
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}

