package FundraiserServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
            
            //Log incoming request
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
            String dateStr = sdf.format(new Date());
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            int clientPort = clientSocket.getPort();        
            System.out.println("[" + dateStr + "] Client connected >> " + clientIP + ":" + clientPort);

            String clientSentence = inFromClient.readLine();

            // For now, echo back the received sentence
            outToClient.writeBytes(clientSentence + '\n');
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
