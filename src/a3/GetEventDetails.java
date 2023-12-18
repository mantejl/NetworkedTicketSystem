package a3;

import java.net.HttpURLConnection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GetEventDetails {

	private static String eventURL = "https://us-west2-csci201-376723.cloudfunctions.net/events/";

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

	public static Event getEvent(String entityID) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Event event = null;
		Fault fault = null;
		try {
			URL url = new URL(eventURL + entityID);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			int responseCode = con.getResponseCode();
			//System.out.println("GET Response Code :: " + responseCode);
			if (responseCode == HttpURLConnection.HTTP_OK) { // success
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				event = gson.fromJson(response.toString(), Event.class);
			} else if (responseCode == 404 || responseCode == 401){
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				fault = gson.fromJson(response.toString(), Fault.class);
			}
		} catch (Exception error) {
			// TODO Auto-generated catch block
			//error.printStackTrace();
		}
		return event; 
	}
}
