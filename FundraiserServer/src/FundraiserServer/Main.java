package FundraiserServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {
    
    public static ArrayList<Fundraiser> fundraisers = new ArrayList<>();
    
    public static void main(String[] args) throws IOException {
        ServerSocket welcomeSocket = new ServerSocket(6900);
        System.out.println("The TCP server is on.");
        
//        LocalDate dt1 = LocalDate.of(2023, 8, 15);
//        Fundraiser e1 = new Fundraiser("Animal Shelter Fund", 1000, dt1);
//        fundraisers.add(e1);
//
//        LocalDate dt2 = LocalDate.of(2023, 9, 10);
//        Fundraiser e2 = new Fundraiser("Library Fund", 750, dt2);
//        fundraisers.add(e2);
//
//        LocalDate dt3 = LocalDate.of(2023, 9, 30);
//        Fundraiser e3 = new Fundraiser("School Supplies", 500, dt3);
//        Donation d1 = new Donation("192.168.1.1", LocalDateTime.now(), 43.33, "test");
//        Donation d2 = new Donation("192.168.1.2", LocalDateTime.now(), 44.33, "tes22t");
//        e3.addDonation(d1);
//        e3.addDonation(d2);
//        fundraisers.add(e3);
//
//        LocalDate dt4 = LocalDate.of(2023, 10, 5);
//        Fundraiser e4 = new Fundraiser("Disaster Relief", 2000, dt4);
//        fundraisers.add(e4);
//
//        LocalDate dt5 = LocalDate.of(2023, 10, 15);
//        Fundraiser e5 = new Fundraiser("Hospital Funds", 3000, dt5);
//        fundraisers.add(e5);
//
//         //Fundraisers in the future
//
//        LocalDate dt9 = LocalDate.of(2024, 2, 14);
//        Fundraiser e9 = new Fundraiser("Valentine's Love for All", 400, dt9);
//        fundraisers.add(e9);
//
//        LocalDate dt10 = LocalDate.of(2024, 4, 22);
//        Fundraiser e10 = new Fundraiser("Earth Day Fund", 800, dt10);
//        fundraisers.add(e10);
//        
//        LocalDate dt6 = LocalDate.of(2023, 11, 1);
//        Fundraiser e6 = new Fundraiser("Food Drive", 600, dt6);
//        fundraisers.add(e6);
//
//        LocalDate dt7 = LocalDate.of(2023, 12, 25);
//        Fundraiser e7 = new Fundraiser("Christmas Charity", 1200, dt7);
//        fundraisers.add(e7);
//
//        LocalDate dt8 = LocalDate.of(2024, 1, 15);
//        Fundraiser e8 = new Fundraiser("New Year Health Drive", 700, dt8);
//        fundraisers.add(e8);

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();

            // Create a new thread for each incoming client
            Thread clientThread = new Thread(new ClientInstance(connectionSocket));
            clientThread.start();
        }
    }
}
