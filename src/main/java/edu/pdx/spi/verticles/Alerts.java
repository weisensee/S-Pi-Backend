package edu.pdx.spi.verticles;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pdx.spi.GcmContent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Alerts extends AbstractVerticle {
    EventBus eb;
    int port = 9994;
    public static final String API_KEY = "AIzaSyBe-3d2hooDYXHHQt5Rb8Qo4wn0vRVRMNE";
    String UserKey;

    // alert sending interval
    int interval = 20000;


  @Override
  public void start() {
      // TODO: delete inactive registration numbers?
      // (maintain mapping of active users instead of just ones who've registered...)

      // GCM registration numbers
      List<String> userGcmRegistrationTokens = new LinkedList<String>();

      eb = vertx.eventBus();

      System.out.println("running alerts simulation, sending every " + interval + " milliseconds");

      // handle new registration tokens from android app
      eb.consumer("newGcmToken", message -> {
          String userRegToken = message.body().toString();
          System.out.println("Received New User Registration: " + userRegToken);

          // add new token to list
          userGcmRegistrationTokens.add(userRegToken);
      });

      // Sends a new alert every 20 seconds
      vertx.setPeriodic(interval, id -> {

          // send message to each registered user
          for (int i = 0; i < userGcmRegistrationTokens.size(); ++i) {
              // create message to send to watch user
              GcmContent content = createContent(userGcmRegistrationTokens.get(i));

              // Send message to specified user
              sendMessage(content, API_KEY);
          }

    });
  }

    // creates message content to be sent to watch
    public GcmContent createContent(String userToken){

        GcmContent c = new GcmContent();

        c.addRegId(userToken);
        c.createData("SPI ALERT!", "***WATCH DEMO:This is the message section of the GCM message***");


        return c;
    }
    private void sendMessage(GcmContent content, String apiKey) {
        try{

            // 1. URL
            URL url = new URL("https://android.googleapis.com/gcm/send");

            // 2. Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 3. Specify POST method
            conn.setRequestMethod("POST");

            // 4. Set the headers
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "key="+apiKey);

            conn.setDoOutput(true);

            // 5. Add JSON data into POST request body

            // 5.1 Use Jackson object mapper to convert Content object into JSON
            ObjectMapper mapper = new ObjectMapper();

            // 5.2 Get connection output stream
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

            // 5.3 Copy Content "JSON" into
            mapper.writeValue(wr, content);

            // 5.4 Send the request
            wr.flush();

            // 5.5 close
            wr.close();

            // 6. Get the response
            int responseCode = conn.getResponseCode();
            // System.out.println("\nSending 'POST' request to URL : " + url);
            // System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 7. Print result
            // System.out.println(response.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
