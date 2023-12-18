package a3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TicketAgent {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Welcome to SalTickets v2.0!");
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter the server hostname: ");
		String host = scan.nextLine(); 
		System.out.println("Enter the server port: ");
		int port = Integer.parseInt(scan.nextLine()); 
		
		Socket socket = null; 
		BufferedReader br = null; 
		PrintWriter pw = null;
		
		try {
			socket = new Socket(host, port);
			br = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
			pw = new PrintWriter(socket.getOutputStream()); 
			String line = br.readLine();
			while(line != null) {
				System.out.println(line);
				line = br.readLine();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
