package FundraiserServer;

import java.time.LocalDate;
import java.util.ArrayList;

public class Fundraiser {
    private String eventName;
    private double targetAmount;
    private LocalDate deadline;
    private ArrayList<Donation> donationLog;

    public Fundraiser(String eventName, double targetAmount, LocalDate deadline) {
        this.eventName = eventName;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
        this.donationLog = new ArrayList<>();
    }

    // Getter methods
    public String getEventName() {
        return eventName;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public ArrayList<Donation> getDonationLog() {
        return donationLog;
    }

    // Setter methods
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void setDonationLog(ArrayList<Donation> donationLog) {
        this.donationLog = donationLog;
    }

    // Method to add a donation
    public void addDonation(Donation donation) {
        this.donationLog.add(donation);
    }
}
