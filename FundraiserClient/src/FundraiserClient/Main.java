package FundraiserClient;
import java.io.*;
import java.net.*;

public class Main {

    public static void main(String[] args) throws Exception {
        String sentence;
        String response;

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = new Socket("localhost", 6900); // Replace with your server's address and port

        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        System.out.println("Connecting to server...");

        while (true) {
            response = inFromServer.readLine();
            if (response == null) {
                break; // Server disconnected
            }
            
            if ("<<READY>>".equals(response)) {
                System.out.print(">> ");
                sentence = inFromUser.readLine();
                outToServer.writeBytes(sentence + '\n');
                outToServer.flush();
            } else {
                System.out.println(response);
            }
        }

        clientSocket.close();
    }
}
