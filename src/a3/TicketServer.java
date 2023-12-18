package a3;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;

public class TicketServer {

	public static List<TicketServerThread> serverThreads = new ArrayList<TicketServerThread>();
	public static List<Ticket> tickets = new ArrayList<Ticket>();
	public static List<Ticket> invalidTickets = new ArrayList<Ticket>();
	public static List<Ticket> timeTickets = new ArrayList<Ticket>();
	public static List<Agents> agents = new ArrayList<Agents>();
	public static List<AgentLock> agentLocks = new ArrayList<AgentLock>();

	static long start;

	public static void main(String[] args) {

		try {
			processSchedule();
			processAgentsFile();
			startServer();

			start = System.currentTimeMillis();
			int processCount = 0;
			int ticketCount = 0;

			while (true) {
				long now = System.currentTimeMillis();
				int tmpSeconds = 0;
				while (ticketCount < tickets.size()) {

					timeTickets.clear();
					while (ticketCount < tickets.size() && tickets.get(ticketCount).getSeconds() == tmpSeconds) {
						timeTickets.add(tickets.get(ticketCount));
						ticketCount++;
						processCount++;
					}
					if (timeTickets.size() > 0) {
						assignTimeTickets(timeTickets);
					}
					Thread.sleep(1000);
					tmpSeconds++;
				}
				if (processCount == tickets.size()) {
					break;
				}
			}

			for (TicketServerThread ts : serverThreads) {
				ts.stopThread();
			}

			// Waiting for agents to finish the last trade.
			int counter = 0;
			StringBuilder str = new StringBuilder();
			str.append("Incomplete Trades: ");
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			for (Ticket t : tickets) {
				if (!t.isAssigned()) {
					str.append(String.format("(%s, %s [%s], %s, %s) ", t.getSeconds(), t.geteventID(),
							t.getEvent().getName(), t.getSale(), dtf.format(now)));
					counter++;
				}
			}
			for (Ticket t : invalidTickets) {
				str.append(String.format("(%s, %s [%s], %s, %s) ", t.getSeconds(), t.geteventID(), "N/A", t.getSale(),
						dtf.format(now)));
				counter++;
			}
			if (counter == 0) {
				str.append("NONE");
			}
			while (true) {
				boolean check = true;
				for (int i = 0; i < serverThreads.size(); i++) {
					if (serverThreads.get(i).isStopped()) {
						serverThreads.get(i).finalMessage(
								String.format("Total Profit Earned: %.2f", agents.get(i).getProfit()), str.toString());
					} else {
						check = false;
					}
				}
				if (check == true) {
					break;
				}
			}

			System.out.println("Processing complete.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void assignTimeTickets(List<Ticket> secTickets) {

		try {
			// TODO Auto-generated method stub
			int ticketPosition = 0;
			Map<String, Set> ticketUnassignedCount = new HashMap<String, Set>();
			do {
				for (int a = 0; a < agents.size(); a++) {
					if (serverThreads.get(a).isProcessing()) {
						continue;
					}
					agentLocks.get(a).agentLock.lock();
					while (ticketPosition < secTickets.size()) {
						float ticketPrice = secTickets.get(ticketPosition).getTotalPrice();
						if (secTickets.get(ticketPosition).getSale() > 0) {
							if (ticketPrice <= agents.get(a).getRemainingBalance()) {
								serverThreads.get(a).assignTrade(secTickets.get(ticketPosition));
								secTickets.get(ticketPosition).setAssigned(true);
								agents.get(a).deductRemainingBalance(ticketPrice);
								ticketPosition++;
							} else {
								Set agentSet = ticketUnassignedCount.get(secTickets.get(ticketPosition).geteventID());
								if (agentSet == null) {
									agentSet = new HashSet<Integer>();
									ticketUnassignedCount.put(secTickets.get(ticketPosition).geteventID(), agentSet);
								}
								agentSet.add(a);
								break;
							}
						} else {
							serverThreads.get(a).assignTrade(secTickets.get(ticketPosition));
							secTickets.get(ticketPosition).setAssigned(true);
							agents.get(a).addProfit(Math.abs(ticketPrice));
							ticketPosition++;
						}
					}
					agentLocks.get(a).assignCondition.signal();
					agentLocks.get(a).agentLock.unlock();
					serverThreads.get(a).setReady();

					// If ticket cannot be traded then skip and move to the next one.
					if (ticketPosition < secTickets.size()
							&& ticketUnassignedCount.get(secTickets.get(ticketPosition).geteventID()) != null) {
						if (ticketUnassignedCount.get(secTickets.get(ticketPosition).geteventID()).size() == agents
								.size()) {
							ticketPosition++;
						}
					}
				}
			} while (ticketPosition < secTickets.size());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private static void startServer() {
		// TODO Auto-generated method stub
		int port = 3456;
		ServerSocket server = null;
		Socket socket = null;
		try {
			server = new ServerSocket(port);
			System.out.println("Listening on port 3456");
			while (true) {
				if (serverThreads.size() == 0) {
					System.out.println("Waiting for agents...");
				} else {
					System.out.println("Waiting for " + (agents.size() - serverThreads.size()) + " more agent(s)...");
				}
				socket = server.accept();
				System.out.println("Connection from " + socket.getInetAddress());
				TicketServerThread ticketThread = new TicketServerThread(socket, agentLocks.get(serverThreads.size()));
				serverThreads.add(ticketThread);
				if (serverThreads.size() == agents.size()) {
					for (TicketServerThread tst : serverThreads) {
						tst.writeMessage("All agents have arrived!");
						tst.writeMessage("Starting service.");
					}
					System.out.println("Starting service.");
					for (Ticket t : invalidTickets) {
						System.out.println(String.format(
								"Invalid eventId (%s). Discard this transaction and continue...", t.geteventID()));
					}
					break;
				} else {
					int agentConnections = agents.size() - serverThreads.size();
					for (TicketServerThread tst : serverThreads) {
						tst.writeMessage(agentConnections + " more agent is needed before the service can begin.");
						tst.writeMessage("Waiting...");
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private static void processAgentsFile() {
		// TODO Auto-generated method stub
		boolean flag = true;
		do {
			Scanner scan = new Scanner(System.in);
			System.out.println("What is the path of the agent file?");
			String csvData = scan.nextLine();
			File file = new File(csvData);
			Scanner fileScanner = null;
			try {
				fileScanner = new Scanner(file);
				boolean invalidFormat = false;
				while (fileScanner.hasNextLine()) {
					String lineCheck = fileScanner.nextLine();
					String[] columns = lineCheck.split(",");
					if (columns.length != 2) {
						System.out.println("The file " + csvData + " is formatted incorrectly.");
						invalidFormat = true;
						break;
					}
				}
				if (invalidFormat) {
					continue;
				}
				fileScanner = new Scanner(file);
				while (fileScanner.hasNextLine()) {
					String line = fileScanner.nextLine();
					String[] fields = line.split(",");
					int serialNumber = Integer.parseInt(fields[0].trim());
					int startingBalance = Integer.parseInt(fields[1].trim());
					Agents a = new Agents(serialNumber, startingBalance);
					agents.add(a);
					AgentLock aLock = new AgentLock();
					agentLocks.add(aLock);
				}
				flag = false;
			} catch (NullPointerException e) {
				System.out.println("The file " + csvData + " is formatted incorrectly.");
				continue;
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("The file " + csvData + " is formatted incorrectly.");
				continue;
			} catch (FileNotFoundException e) {
				System.out.println("The file " + csvData + " cannot be found.");
				continue;
			}
		} while (flag);
		System.out.println("The agents file has been properly read");

	}

	private static void processSchedule() {
		// TODO Auto-generated method stub
		boolean flag = true;
		do {
			Scanner scan = new Scanner(System.in);
			System.out.println("What is the path of the schedule file?");
			String csvData = scan.nextLine();
			File file = new File(csvData);
			Scanner fileScanner = null;
			try {
				fileScanner = new Scanner(file);
				boolean invalidFormat = false;
				while (fileScanner.hasNextLine()) {
					String lineCheck = fileScanner.nextLine();
					String[] columns = lineCheck.split(",");
					if (columns.length != 3) {
						System.out.println("The file " + csvData + " is formatted incorrectly.");
						invalidFormat = true;
						break;
					}
				}
				if (invalidFormat) {
					continue;
				}
				fileScanner = new Scanner(file);
				while (fileScanner.hasNextLine()) {
					String line = fileScanner.nextLine();
					String[] fields = line.split(",");
					int sec = Integer.parseInt(fields[0].trim());
					String name = fields[1].trim();
					int sale = Integer.parseInt(fields[2].trim());
					Ticket t = new Ticket(sec, name, sale);
					if (t.getEvent() == null) {
						invalidTickets.add(t);
						t.setAssigned(false);
					} else {
						tickets.add(t);
					}
				}
				flag = false;
			} catch (NullPointerException e) {
				System.out.println("The file " + csvData + " is formatted incorrectly.");
				continue;
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("The file " + csvData + " is formatted incorrectly.");
				continue;
			} catch (FileNotFoundException e) {
				System.out.println("The file " + csvData + " cannot be found.");
				continue;
			}
		} while (flag);
		System.out.println("The schedule file has been properly read");

	}

	public static String printFormattedTime() {
		DateFormat simple = new SimpleDateFormat("HH:mm:ss:SSS");
		Date result = new Date(System.currentTimeMillis() - start);
		simple.setTimeZone(TimeZone.getTimeZone("UTC"));
		return "[" + simple.format(result) + "]";
	}
}
