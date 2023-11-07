package FundraiserServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Main {

    public static ArrayList<Fundraiser> fundraisers = new ArrayList<>();
    public static ConcurrentMap<String, ClientInstance> clientInstances = new ConcurrentHashMap<>();
    private static ExecutorService pool1 = Executors.newCachedThreadPool(); //For each received message
    private static ExecutorService pool2 = Executors.newCachedThreadPool(); //For client instances
    private static ScheduledExecutorService pool3 = Executors.newScheduledThreadPool(1); // For cleanup service

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
        
        // Start the scheduler to check for inactive client instances every 15 seconds
        ScheduledFuture<?> inactivityHandler = pool3.scheduleAtFixedRate(() -> {
            System.out.println(getCurrentTimeStamp() + " Running cleanup service.");

            LocalDateTime now = LocalDateTime.now();
            ArrayList<String> removedKeys = new ArrayList<>(); // To store the keys of the removed sessions

            clientInstances.forEach((key, client) -> {
                if (!client.isAwaitingResumeResponse()) {
                    Duration duration = Duration.between(client.getLastActiveTime(), LocalDateTime.now());
                    if (duration.getSeconds() > 1800) { //Kill sessions last interacted with over 30 minutes ago 
                        // Send timeout message before terminating
                        sendResponse(client.getDatagramSocket(), client.getClientIP(), client.getClientPort(), "<<TIMEOUT>>\n");
                        client.terminate();
                        removedKeys.add(key);  // Add the key to the list of sessions to be removed
                    }
                }
            });

            // Remove the sessions after sending <<TIMEOUT>> messages
            removedKeys.forEach(key -> {
                ClientInstance client = clientInstances.remove(key);
                System.out.println(getCurrentTimeStamp() + " Session with key " + key + " closed.");
            });

            int sessionsClosed = removedKeys.size();
            int remainingSessions = clientInstances.size(); // Updated count after removals

            if (sessionsClosed > 0) {
                System.out.println(getCurrentTimeStamp() + " " + remainingSessions + " sessions open. Closed " + sessionsClosed + " sessions for inactivity.");
            } else {
                System.out.println(getCurrentTimeStamp() + " " + remainingSessions + " sessions open, no sessions were closed.");
            }
        }, 120, 120, TimeUnit.SECONDS);
        
        try (DatagramSocket serverSocket = new DatagramSocket(serverPort)) {
            System.out.println("The UDP server is running. Listening for datagrams on port " + serverPort + ".");

            while (true) {
                DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
                serverSocket.receive(request);
                InetAddress clientIP = request.getAddress();
                int clientPort = request.getPort();
                String clientKey = clientIP.getHostAddress();
                String receivedMessage = new String(request.getData(), 0, request.getLength()).trim();
                
                System.out.println(getCurrentTimeStamp() + " Datagram received from " + clientIP + ":" + clientPort + " with message: " + receivedMessage);
                pool1.submit(() -> {
                    // Debug message indicating the start of handling in the current thread
                    System.out.println(getCurrentTimeStamp() + " Handling datagram from " + clientIP + ":" + clientPort + " with message: " + receivedMessage);

                    if ("<<START>>".contains(receivedMessage)) {
                        ClientInstance clientInstance = getClientInstance(clientKey);
                        // Debug message for <<START>> condition
                        System.out.println(getCurrentTimeStamp() + " <<START>> condition met for " + clientIP + ":" + clientPort);

                        if (clientInstance != null) {
                            String timeSinceLastActive;
                            String resumeMessage;

                            // Calculate the duration since the last activity
                            Duration durationSinceLastActive = Duration.between(clientInstance.getLastActiveTime(), LocalDateTime.now());
                            long secondsSinceLastActive = durationSinceLastActive.getSeconds();

                            // Determine the time unit and value to display
                            if (secondsSinceLastActive < 60) {
                                // If less than 60 seconds, show in seconds
                                timeSinceLastActive = secondsSinceLastActive + " seconds";
                            } else {
                                // Otherwise, show in minutes
                                long minutesSinceLastActive = durationSinceLastActive.toMinutes();
                                timeSinceLastActive = minutesSinceLastActive + " minutes";
                            }

                            // Construct the message with the time since last interaction
                            resumeMessage = String.format(
                                "It appears you already have a session open, last interacted with %s ago.\nWould you like to resume this session? (yes/no)\n<<READY>>\n", 
                                timeSinceLastActive
                            );

                            // Set the flag to indicate that the server is awaiting a resume response
                            clientInstance.setAwaitingResumeResponse(true);

                            // Send the message outside of the synchronized block to avoid holding the lock during IO operations
                            sendResponse(serverSocket, clientIP, clientPort, resumeMessage);
                        } else {
                            System.out.println(getCurrentTimeStamp() + " No session found for " + clientIP + ":" + clientPort + ". Starting a new session.");

                            // No session exists, start a new one
                            clientInstance = new ClientInstance(serverSocket, clientIP, clientPort);
                            addClientInstance(clientKey, clientInstance);
                            pool2.submit(clientInstance);

                            System.out.println(getCurrentTimeStamp() + " Submitted new ClientInstance to pool2 for " + clientIP + ":" + clientPort);
                        }
                    } else {
                        System.out.println(getCurrentTimeStamp() + " Handling message for " + clientIP + ":" + clientPort);

                        // Handle regular messages
                        ClientInstance clientInstance = getClientInstance(clientKey);

                        if (clientInstance != null) {
                            // Debug message if client is awaiting resume response
                            System.out.println(getCurrentTimeStamp() + " ClientInstance found for " + clientIP + ":" + clientPort);
                            if (clientInstance.isAwaitingResumeResponse()) {
                                // The client was waiting for a response to the resume question
                                clientInstance.setAwaitingResumeResponse(false); // Reset the flag

                                if ("no".equalsIgnoreCase(receivedMessage.trim())) {
                                    // User does not want to resume, terminate and start new
                                    clientInstance.terminate(); // Terminate the old instance
                                    sendResponse(serverSocket, clientIP, clientPort, "Old session terminated.\n");
                                    clientInstance = new ClientInstance(serverSocket, clientIP, clientPort);
                                    addClientInstance(clientKey, clientInstance); // Replace with new instance
                                    pool2.submit(clientInstance);

                                    System.out.println(getCurrentTimeStamp() + " ClientInstance terminated and new session started for " + clientIP + ":" + clientPort);
                                } else {
                                    // User wants to resume, update the port and continue
                                    sendResponse(serverSocket, clientIP, clientPort, "There is where you left off. Session resumed.\n");
                                    clientInstance.updateClientPort(clientPort);
                                    clientInstance.enqueueMessage("<<PRINT>>");
                                    System.out.println(getCurrentTimeStamp() + " ClientInstance resume confirmed for " + clientIP + ":" + clientPort);
                                }
                            } else {
                                // Not waiting for a resume response, handle the message normally
                                if (clientPort != clientInstance.getClientPort()) {
                                    clientInstance.updateClientPort(clientPort);
                                }
                                clientInstance.enqueueMessage(receivedMessage);
                                System.out.println(getCurrentTimeStamp() + " Sent message to clientInstance: " + receivedMessage);
                            }
                        } else {
                            System.out.println(getCurrentTimeStamp() + " No session found for " + clientIP + ":" + clientPort);
                        }
                    }
                });
            }
        }
    }
    
    public static synchronized void addClientInstance(String key, ClientInstance instance) {
        clientInstances.put(key, instance);
    }

    public static synchronized void removeClientInstance(String key) {
        clientInstances.remove(key);
    }
    
    public static synchronized ClientInstance getClientInstance(String key) {
        return clientInstances.get(key);
    }

    
    public static void sendResponse(DatagramSocket serverSocket, InetAddress clientIP, int clientPort, String response) {
        try {
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientIP, clientPort);
            serverSocket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("IOException when sending response: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String getCurrentTimeStamp() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + ">";
    }
}
