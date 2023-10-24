package FundraiserServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    
    public static ArrayList<Fundraiser> fundraisers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        int serverPort = 6900;
        while (true) {
            System.out.println("Enter the server port: ");
            System.out.print(">> ");
            try {
                serverPort = Integer.parseInt(inFromUser.readLine());
                if (serverPort >= 0 && serverPort <= 65535) {
                    break;
                }
                System.out.println("Invalid port number. Please enter a number between 0 and 65535.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Please enter a number between 0 and 65535.");
            }
        }

        ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("The TCP server is on. Listening for connections on port " + serverPort + ".");

        // Create a cached thread pool using ExecuterService
        ExecutorService executorService = Executors.newCachedThreadPool();

        
        while (true) {
            //Accept incoming connections
            Socket connectionSocket = welcomeSocket.accept();

            //Start a new thread on the server to handle the ClientInstance
            executorService.submit(new ClientInstance(connectionSocket));
        }
    }
}
