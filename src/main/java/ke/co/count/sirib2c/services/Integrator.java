/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author ronald.langat
 */
public class Integrator {

    public Integrator() {
    }

    public String requestToThirdParty(String urlto, String json) {
        String response = null;

        try {
            String req = json;
            URL ur;
            ur = new URL(urlto);

            HttpURLConnection con = (HttpURLConnection) ur.openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(40000);
            con.setReadTimeout(25000);
            con.setRequestProperty("Accept", "*/*");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            OutputStream reqStream = con.getOutputStream();

            reqStream.write(req.getBytes());

            String responseString = "";
            String outputString = "";
            
            System.out.println("TO saf request: "+json);

            InputStreamReader isr = new InputStreamReader(con.getInputStream());

            BufferedReader in = new BufferedReader(isr);
            while ((responseString = in.readLine()) != null) {
                outputString = outputString + responseString;
            }
            con.disconnect();

            response = outputString;
            System.out.println("TO saf response: "+response);

        } catch (MalformedURLException ex) {
            response = null;
        } catch (IOException ex) {
            response = null;
        }
        return response;
    }
}
