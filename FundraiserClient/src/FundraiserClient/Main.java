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

    public static void main(String[] args) {
        String serverIP;
        int serverPort;
        DatagramSocket clientSocket = null;
        InetAddress IPAddress = null;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        while (true) { //Main loop
            while (true) {
                try {
                    System.out.println("Enter the server IP:");
                    System.out.print(">> ");
                    serverIP = inFromUser.readLine();
                    if (isValidIP(serverIP)) {
                        IPAddress = InetAddress.getByName(serverIP);
                        break;
                    }
                    System.out.println("Invalid IP address. Please enter a valid IPv4 address.");
                } catch (UnknownHostException e) {
                    System.out.println("Error with the provided IP address: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Error reading from user: " + e.getMessage());
                }
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
                } catch (NumberFormatException | IOException e) {
                    System.out.println("Invalid port number. Please enter a number between 0 and 65535.");
                }
            }

            try {
                clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(5000); //5 second timeout

                System.out.println("Type exit or quit at any time to disconnect.");

                // Send initial connection message to the server
                String initialMessage = "<<START>>";
                byte[] sendData = initialMessage.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
                clientSocket.send(sendPacket);

                // Buffer for receiving data
                byte[] receiveData = new byte[1024];

                // Continuously listen for messages from the server
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    try {
                        clientSocket.receive(receivePacket); // This call will block for 5 seconds if no packet is received
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        
                        // Check for the <<TIMEOUT>> message
                        if (response.contains("<<TIMEOUT>>")) {
                            System.out.println("Sorry, your session was closed for 30 minutes of inactivity. You will need to reconnect to the server.");
                            break;
                        }

                        // Split the response into lines
                        String[] lines = response.split("\n");

                        for (String line : lines) {
                            line = line.trim();  // Trim to remove any leading/trailing whitespace

                            if (line.contains("<<READY>>")) {
                                System.out.print(">> ");
                                String userInput = inFromUser.readLine();

                                // Check if the user wants to exit
                                if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                                    String exitMessage = "<<EXIT>>";
                                    sendData = exitMessage.getBytes();
                                    sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
                                    clientSocket.send(sendPacket);

                                    System.out.println("Exiting....");
                                    clientSocket.close();
                                    return;
                                }

                                // Send user input to server
                                sendData = userInput.getBytes();
                                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
                                clientSocket.send(sendPacket);
                            } else {
                                System.out.println(line);
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("No response received within 5 seconds. Please check if the server IP & port are correct then try again.");
                        break; // Break out of the inner loop to re-prompt IP and port
                    }
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            }
        }
    }
}
