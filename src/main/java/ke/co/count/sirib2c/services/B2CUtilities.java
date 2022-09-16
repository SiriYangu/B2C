/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.services;

import ke.co.count.sirib2c.main.SiriB2C;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Security;
import java.sql.CallableStatement;
import java.sql.SQLException;
import ke.co.count.sirib2c.log.Logging;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ke.co.count.sirib2c.datasource.DBConnection;
import java.util.Base64;

/**
 *
 * @author ronald.langat
 */
public class B2CUtilities {

    private static Logging logger;
    static String TOKEN_URL = SiriB2C.TOKEN_URL;

    public static String getAccessToken(String app_key, String app_secret) {
        logger = new Logging();
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        String access_token = "";
        try {

            String appKeySecret = app_key + ":" + app_secret;
            byte[] bytes = appKeySecret.getBytes("ISO-8859-1");
            String auth = Base64.getEncoder().encodeToString(bytes);

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(TOKEN_URL)
                    .get()
                    .addHeader("authorization", "Basic " + auth)
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
            // System.out.println("response.body().string() "+response.body().string());
            JsonObject accessT = new JsonObject(response.body().string());

            access_token = accessT.getString("access_token");

        } catch (UnsupportedEncodingException ex) {
            access_token = "";
            logger.applicationLog(logger.logPreString() + "Error sending TO mpesa  - " + ex.getMessage() + "\n\n", "", 5);
        } catch (IOException ex) {
            access_token = "";
            logger.applicationLog(logger.logPreString() + "Error sending TO mpesa  - " + ex.getMessage() + "\n\n", "", 5);
        }
        // System.out.println("access_token r  : " + access_token);

        return access_token;
    }

    public int updateMpesaStatus(int mpesaId, int PICKED_STATUS) {
        logger = new Logging();

        int status = 0;
        String updateResponse = "{call UpdateRecordAsPicked(?,?)}";
        DBConnection conIns = new DBConnection();
        try {
            CallableStatement cstm = conIns.getConnection().prepareCall(updateResponse);
            cstm.setObject("mpesaId", mpesaId);
            cstm.setObject("statusId", PICKED_STATUS);
            cstm.execute();
            status = 1;
        } catch (SQLException e) {
            // log error
            logger.applicationLog(logger.logPreString() + "Error UpdateRecordAsPicked  - " + e.getLocalizedMessage() + "\n\n", "", 9);
        } finally {
            //close conection
            conIns.closeConn();
        }

        return status;
    }

    public void updateMpesaStatusByCOnversationID(String ConversationId, int PICKED_STATUS) {
        logger = new Logging();
        String updateResponse = "{call UpdateRecordCallBack(?,?)}";
        DBConnection conIns = new DBConnection();
        try {
            CallableStatement cstm = conIns.getConnection().prepareCall(updateResponse);
            cstm.setObject("ConversationId", ConversationId);
            cstm.setObject("StatusID", PICKED_STATUS);
            cstm.execute();
        } catch (SQLException e) {
            // log error
            logger.applicationLog(logger.logPreString() + "Error sending TO mpesa  - " + e.getMessage() + "\n\n", "", 9);
        } finally {
            //close conection
            conIns.closeConn();
        }
    }

    public static boolean pingURL(String callbackURL) {
        boolean isAvailable = false;
        try {
            final URLConnection connection = new URL(callbackURL).openConnection();
            connection.connect();
            System.out.println("Service " + callbackURL + " available");
            isAvailable = true;
        } catch (final MalformedURLException e) {
            isAvailable = false;
            System.out.println("Service " + callbackURL + " NOT reachable");
        } catch (final IOException e) {
            isAvailable = false;
            System.out.println("Service " + callbackURL + " NOT reachable");
        }

        return isAvailable;
    }
}
