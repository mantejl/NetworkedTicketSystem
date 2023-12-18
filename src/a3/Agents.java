package a3;

public class Agents {
	
	public int serialNumber; 
	public float startingBalance; 
	public float profit = 0; 
	public float remainingBalance; 
	
	public float getRemainingBalance() {
		return remainingBalance;
	}

	public void setRemainingBalance(float remainingBalance) {
		this.remainingBalance = remainingBalance;
	}
	public void deductRemainingBalance(float balance) {
		this.remainingBalance = this.remainingBalance - balance;
	}
	public void addProfit(float profit) {
		this.profit= this.profit + profit;
	}
	

	public Agents(int serialNumber, float startingBalance) {
		this.serialNumber = serialNumber;
		this.startingBalance = startingBalance; 
		this.remainingBalance = startingBalance;
	}
	
	public int getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(int serialNumber) {
		this.serialNumber = serialNumber;
	}
	public float getStartingBalance() {
		return startingBalance;
	}
	public void setStartingBalance(float startingBalance) {
		this.startingBalance = startingBalance;
	}
	public float getProfit() {
		return profit;
	}
	public void setProfit(float profit) {
		this.profit = profit;
	}
	
	public String toString() {
		return "Starting Balance: " + getStartingBalance() + " Serial Number: " + getSerialNumber() + "Remaining Balance: " + getRemainingBalance(); 
	}
}
