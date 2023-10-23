package FundraiserServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    
    public static ArrayList<Fundraiser> fundraisers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        int port = 6900;
        ServerSocket welcomeSocket = new ServerSocket(port);
        System.out.println("The TCP server is on. Listening for connections on port " + port + ".");

        // Create a cached thread pool
        ExecutorService executorService = Executors.newCachedThreadPool();

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();

            // Submit a new task to be executed by available worker thread in the pool
            executorService.submit(new ClientInstance(connectionSocket));
        }
    }
}
