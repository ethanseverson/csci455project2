package FundraiserServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientInstance implements Runnable {
    private final InetAddress clientIP;
    private int clientPort;
    private BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
    private DatagramSocket datagramSocket;
    private boolean awaitingResumeResponse = false;
    private LocalDateTime lastActiveTime;
    
    public ClientInstance(DatagramSocket datagramSocket, InetAddress clientIP, int clientPort) {
        this.datagramSocket = datagramSocket;
        this.clientIP = clientIP;
        this.clientPort = clientPort;
        this.lastActiveTime = LocalDateTime.now();
    }
    
    public void terminate() {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Terminating process");
        Thread.currentThread().interrupt();
    }
    
    public void enqueueMessage(String message) {
        updateLastActiveTime();
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Message received from client: " + message);
        incomingMessages.offer(message);
    }
    
    private String readLineFromQueue() throws InterruptedException {
        String message = incomingMessages.take(); // Take a message from the queue

        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" 
            + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort 
            + " >> Waiting for response.");

        if (message.contains("<<EXIT>>")) {
            terminate();
            return null;
        }
        return message;
    }
    
    private void sendResponse(String response) {
        try {
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientIP, clientPort);
            datagramSocket.send(responsePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void updateClientPort(int newPort) {
        this.clientPort = newPort;
    }
    
    public synchronized void updateLastActiveTime() {
            this.lastActiveTime = LocalDateTime.now();
    }
    
    public int getClientPort() {
        return this.clientPort;
    }
    
    public DatagramSocket getDatagramSocket() {
        return this.datagramSocket;
    }

    public InetAddress getClientIP() {
        return this.clientIP;
    }
    
    public synchronized void setAwaitingResumeResponse(boolean awaiting) {
        this.awaitingResumeResponse = awaiting;
    }

    public synchronized boolean isAwaitingResumeResponse() {
        return this.awaitingResumeResponse;
    }
    
    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    private static class ObjResult {
        int returnCode;
        Object value;

        ObjResult(int returnCode, Object value) {
            this.returnCode = returnCode;
            this.value = value;
        }
    }
    
    @Override
    public void run() {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
            + " >> Connected to server.");

        try {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                    + " >> Starting session...");
            displayWelcome(); //Display connection info and time
            
            //Main loop
            while (true) {
                if (!mainMenu()) break; //If main menu returns 2, exit program
            }
            //            
        } catch (IOException ex) {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                + " >> IO Exception!");
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientInstance.class.getName()).log(Level.SEVERE, null, ex);
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Session closed.");
            // Cleanup code, runs on both normal exit and interruption
            Main.removeClientInstance(clientIP.getHostAddress());
            // Log session closed message
        }
    }
    
    private boolean mainMenu() throws IOException, InterruptedException { //Top level main menu
        boolean printMenu;
        do {
            printMenu = false; // Reset the flag for each iteration
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                    " >> Displaying current fundraisers main menu.");

            ObjResult objr = displayFundraisers(true); // Display current fundraisers
            int count = objr.returnCode;
            String[] fundraiserTitles = (objr.value != null) ? (String[]) objr.value : new String[0];
            displayMainOptions(); // Show options that user can pick
            int result;
            do {
                // Notify the client that it's ready for the next input
                sendResponse("<<READY>>\n");
                // Wait for User Input
                String userInput = readLineFromQueue();

                // Check if user input is to print the main menu again
                if ("<<PRINT>>".equals(userInput)) {
                    printMenu = true;
                    break; // Break out of the current loop to start the outer loop again
                }

                result = handleMainUserInput(userInput, count, fundraiserTitles); // Returns 0 if valid input, 1 if need retry, 2 if exiting.
                if (result == 2) return false;
            } while (result != 0); // Retry if invalid input
        } while (printMenu); // Continue looping if <<PRINT>> was received

        return true; // Exit successfully, open main menu again
    }
    
    private boolean pastMenu() throws IOException, InterruptedException {
        boolean printMenu;

        do {
            printMenu = false; 

            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                    " >> Displaying past fundraisers menu.");

            //Display past fundraisers
            ObjResult objr = displayFundraisers(false); 
            int count = objr.returnCode;
            String[] fundraiserTitles = (String[]) objr.value;

            //Show options that user can pick
            displayPastOptions();
            int result;
            do {
                // Notify the client that it's ready for the next input
                sendResponse("<<READY>>\n");
                //Wait for User Input
                String userInput = readLineFromQueue();

                // Check if the user input is to print the menu again
                if ("<<PRINT>>".equals(userInput)) {
                    printMenu = true;
                    break; // Break out of the current loop to start the outer loop again
                }

                // Returns 0 if valid input, 1 if need retry, 2 if exit
                result = handlePastUserInput(userInput, count, fundraiserTitles); 
                if (result == 2) return false;
            } while (result != 0); //Retry if invalid input
        } while (printMenu); // Continue looping if <<PRINT>> was received

        return true; //Exit past menu
    }

    private void displayWelcome() throws IOException { //Welcome message, shows to client on startup
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
        String currentTime = sdf.format(new Date());
        sendResponse("You have started a new session on the server. " + "The current time is " + currentTime + '\n');
    }

    private ObjResult displayFundraisers(boolean isCurrent) throws IOException {
        String title = isCurrent ? "Current Fundraisers" : "Past Fundraisers"; //Title of table
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Grabbing " + title.toLowerCase() + " for table.");
        int indexCounter = 1; //Start the counter for the table
        StringBuilder sb = new StringBuilder();
        ArrayList<String[]> fundraisersStr = new ArrayList<>(); //ArrayList for string arrays for table
        ArrayList<String> fundraisersTitles = new ArrayList<>(); //Arraylist to store event names for menu selection
        
        synchronized(Main.fundraisers) {
            Main.fundraisers.sort(Comparator.comparing(Fundraiser::getDeadline)); // Sort the fundraisers by deadline

            for (Fundraiser fundraiser : Main.fundraisers) {
                if (fundraiser.isCurrent() == isCurrent) { //For every fundraiser that isCurrent
                    String[] row = { //Formatted data for table
                        String.format("%d", indexCounter++),
                        fundraiser.getEventName(),
                        String.format("$%.2f", fundraiser.getAmountRaised()),
                        String.format("$%.2f", fundraiser.getTargetAmount()),
                        fundraiser.getDeadline().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                        String.format("%d", fundraiser.getDonationLog().size())
                    };
                    fundraisersStr.add(row); //Add entry to list
                    fundraisersTitles.add(fundraiser.getEventName()); //Keep track of what the user sees on screen
                }
            }

            if (fundraisersStr.isEmpty()) { //If there are no fundraisers
                String[][] message = {{"There are no " + title.toLowerCase()}};
                ASCIITableCreator.print(message, 2, false, title, null, false, false, sb);
            } else { //If there are, print the data
                String[][] fundraisersArray = fundraisersStr.toArray(new String[0][]);
                ASCIITableCreator.print(fundraisersArray, 2, true, title, new String[] {"#", "Event Name", "Amount Raised", "Target Amount", "Deadline", "Donations"}, true, false, sb);
            }
            sendResponse(sb.toString()); //Push the table to client
            
            int count = indexCounter - 1; //Amount of entries in the table
            String[] fundraiserTitlesArr = fundraisersTitles.toArray(new String[0]); //This array holds the table the user is viewing to make sure the user is navigated correctly
            return new ObjResult(count, fundraiserTitlesArr); //Return the amount of entries in the cable, with the fundraisersTitles
        }
    }

    private void displayMainOptions() throws IOException {
        String options = "Type \"create\" to create a new fundraiser.";
        boolean hasPastFundraisers = false;
        boolean hasCurrentFundraisers = false;
        
        synchronized (Main.fundraisers) {
            if (!Main.fundraisers.isEmpty()) { //Check if there are any fundraisers
            for (Fundraiser pfr : Main.fundraisers) { 
                 {
                    if (!hasPastFundraisers && !pfr.isCurrent()) { //Check if there are any past fundraisers
                        hasPastFundraisers = true;
                    }
                    if (!hasCurrentFundraisers && pfr.isCurrent()) { //Check if there are any current fundraisers
                        hasCurrentFundraisers = true;
                    } 
                    if (hasPastFundraisers && hasCurrentFundraisers) break; //Since it found both past and current, no need to keep checking
                }
            }

            if (hasPastFundraisers) {  //If there are past fundraisers
                options += "\nType \"past\" to view past fundraisers.";
            }
            if (hasCurrentFundraisers) { //If there are fundraisers
                 options += "\nOtherwise, type the number corresponding to the fundraiser above to open it."; 
            }
            }
        }

        sendResponse(options + '\n');
        
    }
    
    private void displayPastOptions() throws IOException {
        String options = "Type \"back\" or \"menu\" to go back to current fundraisers.";
        boolean hasPastFundraisers = false;
        synchronized (Main.fundraisers) {
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
        }
        sendResponse(options + '\n');
        
    }

    private int handleMainUserInput(String userInput, int count, String[] fundraisersTitles) throws IOException, InterruptedException {
        //Returns 0 if valid entry
        //Returns 1 if invalid
        //Returns 2 if user is exiting
        if (userInput == null) {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Since user input was null, thread will close.");
            return 2;
        } else if (userInput.equalsIgnoreCase("create")) {
            createFundraiser();
            return 0;
        } else if (userInput.equalsIgnoreCase("past")) {
            if (!pastMenu()) return 2;
            return 0;
        } else {
            try {
                int index = Integer.parseInt(userInput) - 1;  // Convert to 0-based index
                if (index >= 0 && index < count) {
                    String eventName = fundraisersTitles[index];
                    Fundraiser targetFundraiser = findFundraiserByName(eventName);
                    int result;
                    if (targetFundraiser != null) {
                        result = viewFundraiser(targetFundraiser);
                    } else {
                        sendResponse("Unable to find that fundraiser. Please try again.\n");
                        
                        return 1;
                    }
                    if (result == 6) {
                        if (!pastMenu()) return 2;
                        return 0;
                    } else if (result == 2) return 2;
                    return 0;
                } else {
                    // Index out of range
                    sendResponse("Invalid selection. Please try again.\n");
                    
                    return 1;
                }
            } catch (NumberFormatException e) {
                // Invalid input (not a number and not "create")
                sendResponse("Invalid input. Please try again.\n");
                
                return 1;
            }
        }
    }
    
    private int handlePastUserInput(String userInput, int count, String[] fundraisersTitles) throws IOException, InterruptedException {
        //Returns 0 if valid entry
        //Returns 1 if invalid
        //Returns 2 if user is exiting
        if (userInput == null) {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Since user input was null, thread will close.");
            return 2;
        } else if (userInput.equalsIgnoreCase("back") || userInput.equalsIgnoreCase("menu")) {
            return 0;
        } else {
            try {
                int index = Integer.parseInt(userInput) - 1;  // Convert to 0-based index
                if (index >= 0 && index < count) {
                    String eventName = fundraisersTitles[index];
                    Fundraiser targetFundraiser = findFundraiserByName(eventName);
                    int result;
                    if (targetFundraiser != null) {
                        result = viewFundraiser(targetFundraiser);
                    } else {
                        sendResponse("Unable to find that fundraiser. Please try again.\n");
                        
                        return 1;
                    }
                    if (result == 6) {
                        if (!pastMenu()) return 2;
                        return 0;
                    } else if (result == 2) return 2;
                    return 0;
                } else {
                    // Index out of range
                    sendResponse("Invalid selection. Please try again.\n");
                    
                    return 1;
                }
            } catch (NumberFormatException e) {
                // Invalid input (not a number or string above)
                sendResponse("Invalid input. Please try again.\n");
                
                return 1;
            }
        }
    }
    
    private Fundraiser findFundraiserByName(String eventName) { 
    synchronized (Main.fundraisers) {
        for (Fundraiser fundraiser : Main.fundraisers) {
            if (fundraiser.getEventName().equals(eventName)) {
                return fundraiser; //Return first (and only) match
            }
        }
    }
    return null;
    }
    
    private synchronized int viewFundraiser(Fundraiser fundraiser) throws IOException, InterruptedException {
        int result = 1;
        boolean printTable = true; //If the donations table should be printed
        do {
            if (printTable) {
                System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                    " >> Viewing fundraiser information for " + fundraiser.getEventName());
                ArrayList<Donation> donations = fundraiser.getDonationLog();  //Grab the donations from the fundraiser
                StringBuilder sb = new StringBuilder();
                if (donations.isEmpty()) {
                    String[][] message = {{"There are no donations for this fundraiser."}};
                    ASCIITableCreator.print(message, 1, false, "Fundraiser: " + fundraiser.getEventName(),
                            null, false, false, sb);
                } else {
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
                            new String[] {"#", "Name", "IP Address", "Amount", "Date/Time"}, true, false, sb);
                }
                sendResponse(sb.toString());
                
                printTable = false; //Since the table is already printed, don't print again next time
            }
            sendResponse( "Type \"back\" to go back to fundraisers list.\n" //Display options
                    + "Type \"remove\" if you want to delete this fundraiser.\n");
            if (fundraiser.isCurrent()) sendResponse("Type \"donate\" to donate to this fundraiser.\n"); //Only show donate if the fundraiser is current
            sendResponse("<<READY>>\n");
            //Wait for User Input
            String userInput = readLineFromQueue();
            
            if ("<<PRINT>>".equals(userInput)) {
                printTable = true; // Set the flag to reprint the table
                continue; // Skip the rest of the loop and start from the beginning
            }

            result = handleFundraiserInput(userInput, fundraiser);
            if (result == 0) { //Add donation
                sendResponse("Type \"cancel\" at any time to return back to fundraiser.\n");
                result = addDonationToFundraiser(fundraiser);
            }
            
            if (result == 2) {
                break;
            } else if (result == 3) { //User canceled adding to donation
                printTable = true;
            } else if (result == 4) { //User added to donation
                printTable = true;
            } else if (result == 5) { //User wants to go to main menu
                return 5;
            } else if (result == 6) { //User wants to go to past fundraiser menu
                return 6;
            } else if (result == 7) { //User canceled deleting fundraiser
                printTable = true;
            }
        } while (result != 2);
        return 2;
    }
    
    private int handleFundraiserInput(String userInput, Fundraiser fundraiser) throws IOException, InterruptedException {
        //Return codes:
        //0 - Add Donation
        //1 - Retry
        //2 - Exit App
        //5 - Goto Main
        //6 - Goto Past
        //7 - Fundraiser not removed
        //8 - Fundraiser removed
        System.out.println(userInput);
        if (userInput == null) {  //This usually happens when the client wants to exit
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Since user input was null, thread will close.");
            return 2;
        } else if (userInput.equalsIgnoreCase("back")) {
            if (!fundraiser.isCurrent()) {
                userInput = "past";
            } else {
                userInput = "menu";
            }
        }
        if (userInput.equalsIgnoreCase("menu")) {
            return 5; //Goto Main Menu
        } else if (userInput.equalsIgnoreCase("past")) {
            return 6; //Goto Past Menu
        }
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User is viewing fundraiser: \"" + fundraiser.getEventName() + "\"");
        if (userInput.equalsIgnoreCase("remove")) { //If user wants to remove fundraiser
            sendResponse("Removing a fundraiser is FINAL. Are you sure you want to continue?.\n"
                    + "Type \"confirm\" if you want to delete, any other input will cancel.\n"
                    + "<<READY>>\n");
            
            String userInputConfirm = readLineFromQueue();
            if (userInputConfirm == null) { //Check if client exits
                System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                    " >> Since user input was null, thread will close.");
                return 2;
            } else if (userInputConfirm.equalsIgnoreCase("confirm")) { //If user wants to remove fundraiser
                boolean wasCurrent = true;
                if (!fundraiser.isCurrent()) wasCurrent = false;
                System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                    " >> User deleted fundraiser: \"" + fundraiser.getEventName() + "\"");
                synchronized (Main.fundraisers) {
                    if (Main.fundraisers.remove(fundraiser)) {
                    sendResponse("Fundraiser removed.\n");
                    
                    if (!wasCurrent) return 6;
                        return 5;
                    }
                }
            }
        sendResponse("The fundraiser has not been removed.\n");
        return 7;  //Fundraiser not removed
        } else if (userInput.equalsIgnoreCase("donate")) {
            return 0;
        } else {
            sendResponse("Invalid input. Please try again.\n");
            
        }
        return 1;
    }
    
    private int createFundraiser() throws IOException, InterruptedException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User starting to create fundraiser.");
        
        ObjResult result;
        
        sendResponse("Type \"cancel\" at any time to return back to main menu.\n");
        
        // Handle name input
        String name;
        boolean nameIsUnique;
        do {
            nameIsUnique = true;

            result = handleUserInput("Please enter the name of the fundraiser.", 0);
            if (result.returnCode != 0) return result.returnCode;
            name = (String) result.value;

            synchronized (Main.fundraisers) {
                // Check for duplicate names
                for (Fundraiser existingFundraiser : Main.fundraisers) {
                    if (existingFundraiser.getEventName().equals(name)) {
                        sendResponse("A fundraiser with this name already exists. Please choose a different name.\n");
                        
                        nameIsUnique = false;
                        break;
                    }
                }
            }

        } while (!nameIsUnique);

        // Handle target amount input
        result = handleUserInput("Please enter the target amount for the fundraiser. ($)", 1);
        if (result.returnCode != 0) return result.returnCode;
        double targetAmount = (double) result.value;

        // Handle date input
        result = handleUserInput("Please enter the deadline for the fundraiser (yyyy-mm-dd).", 2);
        if (result.returnCode != 0) return result.returnCode;
        LocalDate deadline = (LocalDate) result.value;

        // Create fundraiser object and add to ArrayList
        Fundraiser f = new Fundraiser(name, targetAmount, deadline);
        synchronized (Main.fundraisers) {
            Main.fundraisers.add(f);
        }

        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User created fundraiser: " + f.toString());
        return 0;
    }
    
    private int addDonationToFundraiser(Fundraiser fundraiser) throws IOException, InterruptedException {
        ObjResult result;
        
        if (!fundraiser.isCurrent()) {
            sendResponse("This fundraiser is no longer accepting donations\nThank you for your interest.\n");
            
            return 3;
        }

        // Ask user for username
        result = handleUserInput("Please enter your name.", 0);
        if (result.returnCode != 0) return result.returnCode;
        String username = (String) result.value;

        // Ask user for donation amount
        result = handleUserInput("Please enter the amount you wish to donate. ($)", 1);
        if (result.returnCode != 0) return result.returnCode;
        double amount = (double) result.value;

        // Create a new Donation object
        Donation donation = new Donation(
            clientIP.toString(),
            LocalDateTime.now(),
            amount,
            username
        );

        // Add the donation to the fundraiser
        fundraiser.addDonation(donation);

        // Notify user and log
        sendResponse("Thank you for your donation of $" + String.format("%.2f", amount) + "!\n");
        
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User added a donation of $" + String.format("%.2f", amount) + " to fundraiser: " + fundraiser.getEventName());
        return 4;
    }

   private ObjResult handleUserInput(String prompt, int type) throws IOException, InterruptedException {
        while (true) {
            sendResponse(prompt + "\n<<READY>>\n");
            
            String userInput = readLineFromQueue();
            if (userInput == null) {
                return new ObjResult(2, null);
            }
            if (userInput.equalsIgnoreCase("cancel") || userInput.equalsIgnoreCase("<<PRINT>>")) {
                return new ObjResult(3, null);
            }

            if (type == 0) { //Name input
                if (!userInput.trim().isEmpty() && userInput.trim().length() < 100) {
                    return new ObjResult(0, userInput.trim());
                }
                if (userInput.trim().isEmpty()) {
                    sendResponse("Name cannot be empty. Please try again.\n");
                } else {
                    sendResponse("Please keep the name fewer than 100 characters long. You used " + userInput.trim().length() + " characters.\n");
                }
            } else if (type == 1) { // $ input
                try {
                    double roundedValue = Math.round(Double.parseDouble(userInput) * 100.0) / 100.0;
                    if (roundedValue > 0) {  // Check for positive amount
                        return new ObjResult(0, roundedValue);
                    } else {
                        sendResponse("Please enter a positive amount.\n");
                    }
                } catch (NumberFormatException e) {
                    sendResponse("The number you entered was not valid. Please try again.\n");
                }
            } else if (type == 2) {//Date input
                try {
                    LocalDate enteredDate = LocalDate.parse(userInput);
                    if (enteredDate.isBefore(LocalDate.now())) {
                        sendResponse("Warning: The date you entered is in the past.\n");
                    }
                    return new ObjResult(0, enteredDate);
                } catch (DateTimeParseException e) {
                    sendResponse("The date you entered was not valid. Please use format yyyy-mm-dd.\n");
                }
            }
        }
    }
}
