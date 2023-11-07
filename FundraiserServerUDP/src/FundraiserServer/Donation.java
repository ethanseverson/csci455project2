package FundraiserServer;

import java.time.LocalDateTime;

public class Donation {
    private String ipAddress;
    private LocalDateTime donationTime;
    private double amount;
    private String username;

    public Donation(String ipAddress, LocalDateTime donationTime, double amount, String username) {
        this.ipAddress = ipAddress;
        this.donationTime = donationTime;
        this.amount = amount;
        this.username = username;
    }

    // Getter methods
    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getDonationTime() {
        return donationTime;
    }

    public double getAmount() {
        return amount;
    }

    public String getUsername() {
        return username;
    }

    // Setter methods
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setDonationTime(LocalDateTime donationTime) {
        this.donationTime = donationTime;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
