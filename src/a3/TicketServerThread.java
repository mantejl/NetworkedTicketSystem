package a3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class TicketServerThread extends Thread {

	private PrintWriter pw;
	private BufferedReader br;
	private TicketServer ticketServer;
	private List<Ticket> assignTickets = Collections.synchronizedList(new ArrayList<Ticket>());
	AgentLock lock;
	public boolean running = true;
	public boolean complete = false;
	public boolean processing = false;

	public boolean ready = false;

	public boolean isReady() {
		return ready;
	}

	public void setReady() {
		if (assignTickets.size() > 0) {
			this.ready = true;
		}
	}

	public TicketServerThread(Socket s, AgentLock agentLock) {
		try {
			this.lock = agentLock;
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			this.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		while (true) {
			try {
				lock.agentLock.lock();
				if (assignTickets.size() >= 0 ) {
					processTrade();
				}
				if ( !running) {
					break;
				}else {
					lock.assignCondition.await();
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} finally {
				lock.agentLock.unlock();
				ready = true;
			}

		}
	}

	public void stopThread() {
		running = false;
	}

	public void finalMessage(String message, String incomplete) {
		if (!complete) {
			pw.println(TicketServer.printFormattedTime() + " " + incomplete);
			pw.println(message);
			pw.println("Processing complete");
			pw.flush();
			complete = true;
			lock.agentLock.lock();
			lock.assignCondition.signal();
			lock.agentLock.unlock();
		}
	}

	public boolean isProcessing() {
		return processing;
	}

	public boolean isStopped() {
		return !running && assignTickets.isEmpty();
	}

	public void writeMessage(String message) {
		pw.println(message);
		pw.flush();
	}

	public void assignTrade(Ticket t) {
		assignTickets.add(t);
		if (t.getSale() > 0) {
			String message = String.format(
					TicketServer.printFormattedTime()
							+ " Assigned purchase of %d ticket(s) of %s. Total cost estimate = %.2f  *  %d  = %.2f",
					t.getSale(), t.getEvent().getName(), t.getEvent().getPrice(), t.getSale(),
					(Math.abs(t.getSale()) * t.getEvent().getPrice()));
			pw.println(message);
			pw.flush();
		} else if (t.getSale() < 0) {
			String message = String.format(
					TicketServer.printFormattedTime()
							+ " Assigned sale of %d ticket(s) of %s. Total gain estimate = %.2f  *  %d  = %.2f",
					Math.abs(t.getSale()), t.getEvent().getName(), t.getEvent().getPrice(), Math.abs(t.getSale()),
					(Math.abs(t.getSale()) * t.getEvent().getPrice()));
			pw.println(message);
			pw.flush();
		}
	}

	public void processTrade() {
		processing = true;
		try {
			for (Ticket t : assignTickets) {
				if (t.getSale() > 0) {
					String message = String.format(
							TicketServer.printFormattedTime()
									+ " Starting purchase of %d ticket(s) of %s. Total cost = %.2f  *  %d  = %.2f",
							t.getSale(), t.getEvent().getName(), t.getEvent().getPrice(), t.getSale(),
							(Math.abs(t.getSale()) * t.getEvent().getPrice()));
					pw.println(message);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String finish = String.format(
							TicketServer.printFormattedTime() + " Finished purchase of %d ticket(s) of %s.",
							t.getSale(), t.getEvent().getName());
					pw.println(finish);
					pw.flush();
				} else if (t.getSale() < 0) {
					String message = String.format(
							TicketServer.printFormattedTime()
									+ " Starting sale of %d ticket(s) of %s. Total gain = %.2f  *  %d  = %.2f",
							Math.abs(t.getSale()), t.getEvent().getName(), t.getEvent().getPrice(),
							Math.abs(t.getSale()), (Math.abs(t.getSale()) * t.getEvent().getPrice()));
					pw.println(message);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String finish = String.format(
							TicketServer.printFormattedTime() + " Finished sale of %d ticket(s) of %s.",
							Math.abs(t.getSale()), t.getEvent().getName());
					pw.println(finish);
					pw.flush();
				}
			}
			assignTickets.removeAll(assignTickets);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			processing = false;
		}
	}

}
