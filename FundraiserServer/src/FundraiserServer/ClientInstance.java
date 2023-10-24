package FundraiserServer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private static class ObjResult { //Some methods use this to return an object and a return status
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
            outToClient = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                    + " >> Starting client instance...");
            displayWelcome(); //Display connection info and time
            
            //Main loop
            while (true) {
                if (!mainMenu()) break; //If main menu returns 2, exit program
            }
            //
            
            //Exit gracefully
            outToClient.close();
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Connection closed.");
            
        } catch (SocketException se) { //Catch when client closes ungracefully
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                + " >> Connection closed ungracefully.");
        } catch (IOException ex) { //Required IO exception
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                + " >> IO Exception!");
        }
    }
    
    private boolean mainMenu() throws IOException { //Top level main menu
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Displaying current fundraisers main menu.");

        ObjResult objr = displayFundraisers(true); //Display current fundraisers
        int count = objr.returnCode;
        String[] fundraiserTitles = (objr.value != null) ? (String[]) objr.value : new String[0];
        displayMainOptions(); //Show options that user can pick
        int result;
        do {
            // Notify the client that it's ready for the next input
            outToClient.writeBytes("<<READY>>\n");
            outToClient.flush();

            //Wait for User Input
            String userInput = in.readLine();

            result = handleMainUserInput(userInput, count, fundraiserTitles); // Returns 0 if valid input, 1 if need retry, 2 if exiting.
            if (result == 2) return false;
        } while (result != 0); //Retry if invalid input
        return true; //Exit successfully, open main menu again
    }
    
    private boolean pastMenu() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Displaying past fundraisers menu.");
        ObjResult objr = displayFundraisers(false); //Display past fundraisers
        int count = objr.returnCode;
        String[] fundraiserTitles = (String[]) objr.value;
        
        displayPastOptions();
        int result;
        do {
            // Notify the client that it's ready for the next input
            outToClient.writeBytes("<<READY>>\n");
            outToClient.flush();

            //Wait for User Input
            String userInput = in.readLine();

            result = handlePastUserInput(userInput, count, fundraiserTitles); // Returns 0 if valid input, 1 if need retry, 2 if exit
            if (result == 2) return false;
        } while (result != 0); //Retry if invalid input
        return true; //Exit past menu
    }

    private void displayWelcome() throws IOException { //Welcome message, shows to client on startup
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
        String currentTime = sdf.format(new Date());
        outToClient.writeBytes("You are connected to " + clientSocket.getRemoteSocketAddress().toString().substring(1) + ". The current time is " + currentTime + '\n');
        outToClient.flush();
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
            outToClient.writeBytes(sb.toString()); //Push the table to client
            outToClient.flush();
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
            for (Fundraiser pfr : Main.fundraisers) {  // Now directly iterating through the list
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

        outToClient.writeBytes(options + '\n');
        outToClient.flush();
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
        outToClient.writeBytes(options + '\n');
        outToClient.flush();
    }

    private int handleMainUserInput(String userInput, int count, String[] fundraisersTitles) throws IOException {
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
                        outToClient.writeBytes("Unable to find that fundraiser. Please try again.\n");
                        outToClient.flush();
                        return 1;
                    }
                    if (result == 6) {
                        if (!pastMenu()) return 2;
                        return 0;
                    } else if (result == 2) return 2;
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
    
    private int handlePastUserInput(String userInput, int count, String[] fundraisersTitles) throws IOException {
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
                        outToClient.writeBytes("Unable to find that fundraiser. Please try again.\n");
                        outToClient.flush();
                        return 1;
                    }
                    if (result == 6) {
                        if (!pastMenu()) return 2;
                        return 0;
                    } else if (result == 2) return 2;
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

    
    private synchronized int viewFundraiser(Fundraiser fundraiser) throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> Viewing fundraiser information for " + fundraiser.getEventName());
        
        //Everything in the loop below is for user input
        int result = 1;
        boolean printTable = true; //If the donations table should be printed
        do {
            if (printTable) {
                ArrayList<Donation> donations = fundraiser.getDonationLog();  //Grab the donations from the fundraiser
                StringBuilder sb = new StringBuilder();
                //Like above for listing the fundraiser list, print out donations in a table
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
                outToClient.writeBytes(sb.toString());
                outToClient.flush();
                printTable = false; //Since the table is already printed, don't print again next time
            }
            outToClient.writeBytes( "Type \"back\" to go back to fundraisers list.\n" //Display options
                    + "Type \"remove\" if you want to delete this fundraiser.\n");
            if (fundraiser.isCurrent()) outToClient.writeBytes("Type \"donate\" to donate to this fundraiser.\n"); //Only show donate if the fundraiser is current
            outToClient.writeBytes("<<READY>>\n");
            outToClient.flush();

            //Wait for User Input
            String userInput = in.readLine();

            result = handleFundraiserInput(userInput, fundraiser);
            if (result == 0) { //Add donation
                outToClient.writeBytes("Type \"cancel\" at any time to return back to fundraiser.\n");
                outToClient.flush();
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
    
    private int handleFundraiserInput(String userInput, Fundraiser fundraiser) throws IOException {
        //Return codes:
        //0 - Add Donation
        //1 - Retry
        //2 - Exit App
        //5 - Goto Main
        //6 - Goto Past
        //7 - Fundraiser not removed
        //8 - Fundraiser removed
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
            outToClient.writeBytes("Removing a fundraiser is FINAL. Are you sure you want to continue?.\n"
                    + "Type \"confirm\" if you want to delete, any other input will cancel.\n"
                    + "<<READY>>\n");
            outToClient.flush();
            String userInputConfirm = in.readLine();
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
                    outToClient.writeBytes("Fundraiser removed.\n");
                    outToClient.flush();
                    if (!wasCurrent) return 6;
                        return 5;
                    }
                }
            }
        outToClient.writeBytes("The fundraiser has not been removed.\n");
        outToClient.flush();
        return 7;  //Fundraiser not removed
        } else if (userInput.equalsIgnoreCase("donate")) {
            return 0;
        } else {
            outToClient.writeBytes("Invalid input. Please try again.\n");
            outToClient.flush();
        }
        return 1;
    }
    
    private int createFundraiser() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User starting to create fundraiser.");
        
        ObjResult result;
        
        outToClient.writeBytes("Type \"cancel\" at any time to return back to main menu.\n");
        outToClient.flush();

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
                        outToClient.writeBytes("A fundraiser with this name already exists. Please choose a different name.\n");
                        outToClient.flush();
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
    
    private int addDonationToFundraiser(Fundraiser fundraiser) throws IOException {
        ObjResult result;
        
        if (!fundraiser.isCurrent()) {
            outToClient.writeBytes("This fundraiser is no longer accepting donations\nThank you for your interest.\n");
            outToClient.flush();
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
            clientIP,
            LocalDateTime.now(),
            amount,
            username
        );

        // Add the donation to the fundraiser
        fundraiser.addDonation(donation);

        // Notify user and log
        outToClient.writeBytes("Thank you for your donation of $" + String.format("%.2f", amount) + "!\n");
        outToClient.flush();
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User added a donation of $" + String.format("%.2f", amount) + " to fundraiser: " + fundraiser.getEventName());
        return 4;
    }


   private ObjResult handleUserInput(String prompt, int type) throws IOException {
        while (true) {
            outToClient.writeBytes(prompt + "\n<<READY>>\n");
            outToClient.flush();
            String userInput = in.readLine();
            if (userInput == null) {
                return new ObjResult(2, null);
            }
            if (userInput.equalsIgnoreCase("cancel")) {
                return new ObjResult(3, null);
            }

            if (type == 0) { //Name input
                if (!userInput.trim().isEmpty() && userInput.trim().length() < 100) {
                    return new ObjResult(0, userInput.trim());
                }
                if (userInput.trim().isEmpty()) {
                    outToClient.writeBytes("Name cannot be empty. Please try again.\n");
                } else {
                    outToClient.writeBytes("Please keep the name fewer than 100 characters long. You used " + userInput.trim().length() + " characters.\n");
                }
            } else if (type == 1) { //$ input
                try {
                    double roundedValue = Math.round(Double.parseDouble(userInput) * 100.0) / 100.0;
                    return new ObjResult(0, roundedValue);
                } catch (NumberFormatException e) {
                    outToClient.writeBytes("The number you entered was not valid. Please try again.\n");
                }
            } else if (type == 2) {//Date input
                try {
                    LocalDate enteredDate = LocalDate.parse(userInput);
                    if (enteredDate.isBefore(LocalDate.now())) {
                        outToClient.writeBytes("Warning: The date you entered is in the past.\n");
                    }
                    return new ObjResult(0, enteredDate);
                } catch (DateTimeParseException e) {
                    outToClient.writeBytes("The date you entered was not valid. Please use format yyyy-mm-dd.\n");
                }
            }
            outToClient.flush();
        }
    }

}
