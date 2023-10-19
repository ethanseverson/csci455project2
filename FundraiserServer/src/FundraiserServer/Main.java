package FundraiserServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Main {
    
    public static ArrayList<Fundraiser> fundraisers = new ArrayList<>();
    
    public static void main(String[] args) throws IOException {
        ServerSocket welcomeSocket = new ServerSocket(6900);
        System.out.println("The TCP server is on.");

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();

            // Create a new thread for each incoming client
            Thread clientThread = new Thread(new ClientHandler(connectionSocket));
            clientThread.start();
        }
    }
}
