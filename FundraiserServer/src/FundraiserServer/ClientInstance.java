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
    
    
    @Override
    public void run() {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                    + " >> Connected to server.");


        try {
            outToClient = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                    + " >> Starting client instance...");
            displayWelcome();
            
            while (true) {
                if (!mainMenu()) break;
            }

            outToClient.close();
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                " >> Connection closed.");
            
        } catch (SocketException se) {
            System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort
                + " >> Connection closed ungracefully.");
        } catch (IOException ex) {
            Logger.getLogger(ClientInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean mainMenu() throws IOException {
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

            result = handleMainUserInput(userInput, count); // Returns 0 if valid input, 1 if need retry, 2 if break.
            if (result == 2) return false;
        } while (result != 0);
        return true;
    }
    
    private boolean pastMenu() throws IOException {
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

            result = handlePastUserInput(userInput, count); // Returns 0 if valid input, 1 if need retry, 2 if exit
            if (result == 2) return false;
        } while (result != 0);
        return true;
    }

    private void displayWelcome() throws IOException {
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
                    int result = viewFundraiser(index, true);
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
    
    private int handlePastUserInput(String userInput, int count) throws IOException {
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
                    int result = viewFundraiser(index, false);
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
        }

        Fundraiser fundraiser = filteredFundraisers.get(index);

        int result = 1;
        boolean printTable = true;
        do {
            if (printTable) {
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
                        " >> " + fundraiser.getEventName() + " contains " + donations.size() + " donations totaling in " + String.format("$%.2f", fundraiser.getAmountRaised()));
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
                printTable = false;
            }
            outToClient.writeBytes( "Type \"back\" to go back to fundraisers list.\n"
                    + "Type \"remove\" if you want to delete this fundraiser.\n");
            if (fundraiser.isCurrent()) outToClient.writeBytes("Type \"donate\" to donate to this fundraiser.\n");
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
        if (userInput == null) {
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
            return 5;
        } else if (userInput.equalsIgnoreCase("past")) {
            return 6;
        }
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User is viewing fundraiser: \"" + fundraiser.getEventName() + "\"");
        if (userInput.equalsIgnoreCase("remove")) {
            outToClient.writeBytes("Removing a fundraiser is FINAL. Are you sure you want to continue?.\n"
                    + "Type \"confirm\" if you want to delete, any other input will cancel.\n"
                    + "<<READY>>\n");
            outToClient.flush();
            String userInputConfirm = in.readLine();
            if (userInputConfirm == null) {
                System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                    " >> Since user input was null, thread will close.");
                return 2;
            } else if (userInputConfirm.equalsIgnoreCase("confirm")) {
                boolean wasCurrent = true;
                if (!fundraiser.isCurrent()) wasCurrent = false;
                System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
                    " >> User deleted fundraiser: \"" + fundraiser.getEventName() + "\"");
                if (Main.fundraisers.remove(fundraiser)) {
                    outToClient.writeBytes("Fundraiser removed.\n");
                    outToClient.flush();
                    if (!wasCurrent) return 6;
                    return 5;
                }
            }
        outToClient.writeBytes("The fundraiser has not been removed.\n");
        outToClient.flush();
        return 7;
        } else if (userInput.equalsIgnoreCase("donate")) {
            return 0;
        } else {
            outToClient.writeBytes("Invalid input. Please try again.\n");
            outToClient.flush();
        }
        return 1;
    }
    
    private static class UserInputResult {
        int returnCode;
        Object value;

        UserInputResult(int returnCode, Object value) {
            this.returnCode = returnCode;
            this.value = value;
        }
    }
    
    private int createFundraiser() throws IOException {
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User starting to create fundraiser.");
        
        UserInputResult result;
        
        outToClient.writeBytes("Type \"cancel\" at any time to return back to main menu.\n");
        outToClient.flush();

        // Handle name input
        result = handleUserInput("Please enter the name of the fundraiser.", 0);
        if (result.returnCode != 0) return result.returnCode;
        String name = (String) result.value;

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
        Main.fundraisers.add(f);
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()) + "] <" + Thread.currentThread().getName() + "> " + clientIP + ":" + clientPort +
            " >> User created fundraiser: " + f.toString());
        return 0;
    }
    
    private int addDonationToFundraiser(Fundraiser fundraiser) throws IOException {
        UserInputResult result;
        
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


   private UserInputResult handleUserInput(String prompt, int type) throws IOException {
        while (true) {
            outToClient.writeBytes(prompt + "\n<<READY>>\n");
            outToClient.flush();
            String userInput = in.readLine();
            if (userInput == null) {
                return new UserInputResult(2, null);
            }
            if (userInput.equalsIgnoreCase("cancel")) {
                return new UserInputResult(3, null);
            }

            if (type == 0) {
                if (!userInput.trim().isEmpty() && userInput.trim().length() < 100) {
                    return new UserInputResult(0, userInput.trim());
                }
                if (userInput.trim().isEmpty()) {
                    outToClient.writeBytes("Name cannot be empty. Please try again.\n");
                } else {
                    outToClient.writeBytes("Please keep the name fewer than 100 characters long. You used " + userInput.trim().length() + " characters.\n");
                }
            } else if (type == 1) {
                try {
                    double roundedValue = Math.round(Double.parseDouble(userInput) * 100.0) / 100.0;
                    return new UserInputResult(0, roundedValue);
                } catch (NumberFormatException e) {
                    outToClient.writeBytes("The number you entered was not valid. Please try again.\n");
                }
            } else if (type == 2) {
                try {
                    LocalDate enteredDate = LocalDate.parse(userInput);
                    if (enteredDate.isBefore(LocalDate.now())) {
                        outToClient.writeBytes("Warning: The date you entered is in the past.\n");
                    }
                    return new UserInputResult(0, enteredDate);
                } catch (DateTimeParseException e) {
                    outToClient.writeBytes("The date you entered was not valid. Please use format yyyy-mm-dd.\n");
                }
            }
            outToClient.flush();
        }
    }

}
