import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client implements Runnable{

    private Socket client;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean done;
    private String hostAddress;

    @Override
    public void run() {
        try {
            client = new Socket(hostAddress, 4000);
            out = new DataOutputStream(client.getOutputStream()); // stream from client to server
            in = new DataInputStream(client.getInputStream());
            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();
            String inMessage;
            
            while((inMessage = in.readUTF())!= null){
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            System.out.println("Could not create client.");
            e.printStackTrace();
            shutdown();
        }
        
    }

    public void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if(!client.isClosed()){
                client.close();
            }
        } catch (IOException e) {

        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    if(message.startsWith("/quit")){
                        out.writeUTF(message);
                        inReader.close();
                        System.exit(0);
                    } else {
                        out.writeUTF(message);
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }

    }
    
    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
    
}

