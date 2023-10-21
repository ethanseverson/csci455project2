package FundraiserServer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class ClientInstance implements Runnable {
    private Socket clientSocket;
    private DataOutputStream outToClient;
    private BufferedReader in;
    private String clientIP;
    private int clientPort;

    public ClientInstance(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.clientIP = this.clientSocket.getInetAddress().getHostAddress();
        this.clientPort = this.clientSocket.getPort();
    }
    
    
    @Override
    public void run() {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                    + " >> Connected to server.");


        try {
            outToClient = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                    + " >> Starting client instance...");
            displayWelcome();

            mainMenu();

            outToClient.close();
            
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Connection closed.");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void mainMenu() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Displaying current fundraisers main menu.");

        int count = displayFundraisers(true); //Display current fundraisers

        displayMainOptions();

        int result;
        do {
            // Notify the client that it's ready for the next input
            outToClient.writeBytes("<<READY>>\n");
            outToClient.flush();

            //Wait for User Input
            String userInput = in.readLine();

            result = handleMainUserInput(userInput, count); // Returns 0 if valid input, 1 if need retry
        } while (result != 0);
    }
    
    private void pastMenu() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Displaying past fundraisers menu.");
        int count = displayFundraisers(false); //Display past fundraisers
        displayPastOptions();
        int result;
        do {
            // Notify the client that it's ready for the next input
            outToClient.writeBytes("<<READY>>\n");
            outToClient.flush();

            //Wait for User Input
            String userInput = in.readLine();

            result = handlePastUserInput(userInput, count); // Returns 0 if valid input, 1 if need retry
        } while (result != 0);
    }

    private void displayWelcome() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Displaying welcome message.");
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
        String currentTime = sdf.format(new Date());
        outToClient.writeBytes("You are connected to " + clientSocket.getRemoteSocketAddress().toString().substring(1) + ". The current time is " + currentTime + '\n');
        outToClient.flush();
    }

    private int displayFundraisers(boolean isCurrent) throws IOException {
        String title = isCurrent ? "Current Fundraisers" : "Past Fundraisers";
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Grabbing " + title.toLowerCase() + " for table.");
        int indexCounter = 1;
        StringBuilder sb = new StringBuilder();
        ArrayList<String[]> fundraisers = new ArrayList<>();
        
        // Sort the fundraisers by deadline
        Main.fundraisers.sort(Comparator.comparing(Fundraiser::getDeadline));
        
        for (Fundraiser fundraiser : Main.fundraisers) {
            if (fundraiser.isCurrent() == isCurrent) {
                String[] row = {
                    String.format("%d", indexCounter++),
                    fundraiser.getEventName(),
                    String.format("$%.2f", fundraiser.getAmountRaised()),
                    String.format("$%.2f", fundraiser.getTargetAmount()),
                    fundraiser.getDeadline().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                    String.format("%d", fundraiser.getDonationLog().size())
                };
                fundraisers.add(row);
            }
        }
        
        if (fundraisers.isEmpty()) {
            String[][] message = {{"There are no " + title.toLowerCase()}};
            ASCIITableCreator.print(message, 2, false, title, null, false, false, sb);
        } else {
            String[][] fundraisersArray = fundraisers.toArray(new String[0][]);
            ASCIITableCreator.print(fundraisersArray, 2, true, title, new String[] {"#", "Event Name", "Amount Raised", "Target Amount", "Deadline", "Donations"}, true, false, sb);
        }

        outToClient.writeBytes(sb.toString());
        outToClient.flush();
        return indexCounter - 1;
    }

    private void displayMainOptions() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Promping user for input at current fundraisers menu.");
        String options = "Type \"create\" to create a new fundraiser.";
        boolean hasPastFundraisers = false;

        if (!Main.fundraisers.isEmpty()) {
            for (Fundraiser pfr : Main.fundraisers) {  // Now directly iterating through the list
                if (!pfr.isCurrent()) {
                    hasPastFundraisers = true;
                    break;
                }
            }

            if (hasPastFundraisers) {
                options += "\nType \"past\" to view past fundraisers.";
            }
            options += "\nOtherwise, type the number corresponding to the fundraiser above to open it.";
        }
        outToClient.writeBytes(options + '\n');
        outToClient.flush();
    }
    
    private void displayPastOptions() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Prompting user for input at past fundraisers menu.");
        String options = "Type \"back\" or \"menu\" to go back to current fundraisers.";
        boolean hasPastFundraisers = false;

        if (!Main.fundraisers.isEmpty()) {
            for (Fundraiser pfr : Main.fundraisers) {  // Now directly iterating through the list
                if (!pfr.isCurrent()) {
                    hasPastFundraisers = true;
                    break;
                }
            }

            if (hasPastFundraisers) {
                options += "\nOtherwise, type the number corresponding to the fundraiser above to open it.";
            }
            //options += "\nOtherwise, type the number corresponding to the fundraiser above to open it.";
        }
        outToClient.writeBytes(options + '\n');
        outToClient.flush();
    }

    private int handleMainUserInput(String userInput, int count) throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Received user input for current fundraisers: \"" + userInput + "\"");
        if (userInput == null) {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Since user input was null, thread will close.");
            return 0;
        } else if (userInput.equalsIgnoreCase("create")) {
            // Call a method to handle fundraiser creation
            return 0;
        } else if (userInput.equalsIgnoreCase("past")) {
            pastMenu();
            return 0;
        } else {
            try {
                int index = Integer.parseInt(userInput) - 1;  // Convert to 0-based index
                if (index >= 0 && index < count) {
                    viewFundraiser(index, true);
                    return 0;
                } else {
                    // Index out of range
                    outToClient.writeBytes("Invalid selection. Please try again.\n");
                    outToClient.flush();
                    return 1;
                }
            } catch (NumberFormatException e) {
                // Invalid input (not a number and not "create")
                outToClient.writeBytes("Invalid input. Please try again.\n");
                outToClient.flush();
                return 1;
            }
        }
    }
    
    private int handlePastUserInput(String userInput, int count) throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Received user input for past fundraisers: \"" + userInput + "\"");
        if (userInput == null) {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Since user input was null, thread will close.");
            return 0;
        } else if (userInput.equalsIgnoreCase("back") || userInput.equalsIgnoreCase("menu")) {
            mainMenu();
            return 0;
        } else {
            try {
                int index = Integer.parseInt(userInput) - 1;  // Convert to 0-based index
                if (index >= 0 && index < count) {
                    viewFundraiser(index, false);
                    return 0;
                } else {
                    // Index out of range
                    outToClient.writeBytes("Invalid selection. Please try again.\n");
                    outToClient.flush();
                    return 1;
                }
            } catch (NumberFormatException e) {
                // Invalid input (not a number or string above)
                outToClient.writeBytes("Invalid input. Please try again.\n");
                outToClient.flush();
                return 1;
            }
        }
    }
    
    private int viewFundraiser(int index, boolean isCurrent) throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Viewing fundraiser information for index " + index + ", isCurrent: " + isCurrent);
        ArrayList<Fundraiser> filteredFundraisers = new ArrayList<>();
        for (Fundraiser fundraiser : Main.fundraisers) {
            if (fundraiser.isCurrent() == isCurrent) {
                filteredFundraisers.add(fundraiser);
            }
        }

        if (index < 0 || index >= filteredFundraisers.size()) {
            outToClient.writeBytes("Invalid selection. Please try again.\n");
            outToClient.flush();
            return 1;
        }

        Fundraiser fundraiser = filteredFundraisers.get(index);

        ArrayList<Donation> donations = fundraiser.getDonationLog();
        StringBuilder sb = new StringBuilder();
        
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Grabbed fundraiser: " + fundraiser.getEventName());

        if (donations.isEmpty()) {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> " + fundraiser.getEventName() + " contains no donation information.");
            String[][] message = {{"There are no donations for this fundraiser."}};
            ASCIITableCreator.print(message, 1, false, "Fundraiser: " + fundraiser.getEventName(),
                    null, false, false, sb);
        } else {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> " + fundraiser.getEventName() + " contains " + donations.size() + " donations totaling in $" + String.format("$%.2f", fundraiser.getAmountRaised()));
            ArrayList<String[]> rows = new ArrayList<>();
            int counter = 1;
            for (Donation donation : donations) {
                String[] row = {
                    String.valueOf(counter++),
                    donation.getUsername(),
                    donation.getIpAddress(),
                    String.format("$%.2f", donation.getAmount()),
                    donation.getDonationTime().format(DateTimeFormatter.ofPattern("M/d/yyyy h:mm a"))
                };
                rows.add(row);
            }
            String[][] rowsArray = rows.toArray(new String[0][]);
            ASCIITableCreator.print(rowsArray, 2, true,
                    "Fundraiser: " + fundraiser.getEventName() + ", " + String.format("$%.2f",fundraiser.getAmountRaised()) + " raised." ,
                    new String[] {"#", "Username", "IP Address", "Amount", "Date/Time"}, true, false, sb);
    }
    
    outToClient.writeBytes(sb.toString());
    outToClient.flush();
    
    return 0;
}

}
