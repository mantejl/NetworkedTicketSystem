package a3;

public class Ticket {

	public int seconds;
	public String eventID;
	public int sale;
	public Event event; 
	public boolean assigned = false; 

	public boolean isAssigned() {
		return assigned;
	}

	public void setAssigned(boolean assigned) {
		this.assigned = assigned;
	}

	public Ticket(int seconds, String eventID, int sale) {
		this.seconds = seconds;
		this.eventID = eventID;
		this.sale = sale;
		event = GetEventDetails.getEvent(eventID); 
	}

	public int getSeconds() {
		return seconds;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}

	public String geteventID() {
		return eventID;
	}

	public void seteventID(String name) {
		this.eventID = name;
	}

	public int getSale() {
		return sale;
	}

	public void setSale(int sale) {
		this.sale = sale;
	}
	
	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}
	
	
	public float getTotalPrice() {
		
		return sale*event.getPrice();
		
	}
	
	public String toString() {
		return "Seconds: " + getSeconds() + " EventID: " + geteventID() + " Sale: " + getSale() + " Event Name + Price: " + getEvent().getName() + " " + getEvent().getPrice(); 
	}
}

