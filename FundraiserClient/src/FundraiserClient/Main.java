package FundraiserClient;

import java.io.*;
import java.net.*;
import java.util.regex.Pattern;

public class Main {

    // Validate IPv4 address using regex
    // Regex from https://stackoverflow.com/questions/5284147/validating-ipv4-addresses-with-regexp
    public static boolean isValidIP(String ip) {
        String regex = "^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])\\.){3}"
                     + "(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])$";
        return Pattern.matches(regex, ip);
    }

    public static void main(String[] args) throws Exception {
        String sentence;
        String response;
        String serverIP;
        int serverPort;

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = null;

        // Prompt and validate server IP and port
        while (true) {
            System.out.println("Enter the server IP:");
            System.out.print(">> ");
            serverIP = inFromUser.readLine();
            if (isValidIP(serverIP)) {
                break;
            }
            System.out.println("Invalid IP address. Please enter a valid IPv4 address.");
        }

        while (true) {
            System.out.println("Enter the port:");
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

        int attempts = 0;
        while (attempts < 4) {
            try {
                clientSocket = new Socket(serverIP, serverPort);
                break; // Connection successful, exit loop
            } catch (IOException e) {
                attempts++;
                System.out.println("Failed to connect. Attempt " + attempts + " of 4.");
                if (attempts < 4) {
                    Thread.sleep(1000); // Wait for 1 second before next attempt
                }
            }
        }

        if (clientSocket == null) {
            System.out.println("Could not connect to the server after 4 attempts. Exiting.");
            return;
        }

        System.out.println("Connecting to server...");
        System.out.println("Type exit or quit at any time to disconnect.");

        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        while (true) {
            response = inFromServer.readLine();
            if (response == null) {
                System.out.println("Server disconnected.");
                break; // Server disconnected
            }
            
            if ("<<READY>>".equals(response)) {
                System.out.print(">> ");
                sentence = inFromUser.readLine();
                if (sentence.equalsIgnoreCase("exit") || sentence.equalsIgnoreCase("quit")) {
                    System.out.println("Disconnected from server.");
                    break;
                }
                outToServer.writeBytes(sentence + '\n');
                outToServer.flush();
            } else {
                System.out.println(response);
            }
        }
        clientSocket.close();
    }
}
