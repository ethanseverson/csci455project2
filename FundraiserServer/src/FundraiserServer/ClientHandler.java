package FundraiserServer;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private DataOutputStream outToClient;
        private BufferedReader in;

    public ClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }
    
    @Override
    public void run() {
        try {
            outToClient = new DataOutputStream(clientSocket.getOutputStream());

            displayWelcome();

            int count = displayFundraisers(true); //Display current fundraisers

            displayOptions();

            int result;
            do {
                // Notify the client that it's ready for the next input
                outToClient.writeBytes("<<READY>>\n");
                outToClient.flush();
                
                //Wait for User Input
                String userInput = in.readLine();

                result = handleMainUserInput(userInput, count); // Returns 0 if valid input, 1 if need retry
            } while (result != 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayWelcome() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a");
        String currentTime = sdf.format(new Date());
        outToClient.writeBytes("You are connected to " + clientSocket.getRemoteSocketAddress() + ". The current time is " + currentTime + '\n');
        outToClient.flush();
    }

    private int displayFundraisers(boolean isCurrent) throws IOException {
        int indexCounter = 1;
        StringBuilder sb = new StringBuilder();
        ArrayList<String[]> fundraisers = new ArrayList<>();

        String title = isCurrent ? "Current Fundraisers" : "Past Fundraisers";

        for (Fundraiser fundraiser : Main.fundraisers) {
            if (fundraiser.isCurrent() == isCurrent) {
                String[] row = {
                    String.format("%d", indexCounter++),
                    fundraiser.getEventName(),
                    String.format("$%.2f", fundraiser.getAmountRaised()),
                    String.format("$%.2f", fundraiser.getTargetAmount()),
                    fundraiser.getDeadline().toString(),
                    String.format("%d", fundraiser.getDonationLog().size())
                };
                fundraisers.add(row);
            }
        }

        if (fundraisers.isEmpty()) {
            String[][] message = {{"There are no " + title.toLowerCase()}};
            ASCIITableCreator.print(message, 2, false, null, new String[] {title}, false, false, sb);
        } else {
            String[][] fundraisersArray = fundraisers.toArray(new String[0][]);
            ASCIITableCreator.print(fundraisersArray, 2, true, title, new String[] {"#", "Event Name", "Amount Raised", "Target Amount", "Deadline", "Donations"}, true, false, sb);
        }

        outToClient.writeBytes(sb.toString());
        outToClient.flush();
        return indexCounter - 1;
    }

    private void displayOptions() throws IOException {
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

    
    private int handleMainUserInput(String userInput, int count) throws IOException {
        if (userInput.equalsIgnoreCase("create")) {
            // Call a method to handle fundraiser creation
            return 0;
        } else if (userInput.equalsIgnoreCase("past")) {
            displayFundraisers(false); //Display past fundraisers
            return 0;
        } else {
            try {
                int index = Integer.parseInt(userInput) - 1;  // Convert to 0-based index
                if (index >= 0 && index < count) {
                    // Call a method to view the selected fundraiser
                    //viewFundraiser(index);
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
}
