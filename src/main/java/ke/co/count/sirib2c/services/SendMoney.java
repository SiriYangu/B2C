/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.services;

import ke.co.count.sirib2c.main.SiriB2C;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import ke.co.count.sirib2c.log.Logging;
import ke.co.count.sirib2c.security.SecurityCredential;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * @author bernard.mwangi
 */
public class SendMoney extends AbstractVerticle {

    String SHORTCODE = SiriB2C.SHORTCODE;
    String COMMAND_ID = SiriB2C.COMMAND_ID;
    String QUEUETIMEOUT_URL = SiriB2C.QUEUETIMEOUT_URL;
    String RESULT_URL = SiriB2C.RESULT_URL;
    String REMARKS = "B2C transaction";
    String INITIATOR_NAME = SiriB2C.INITIATOR_NAME;
    String B2C_URL_To = SiriB2C.B2C_URL_To.trim();
    String CONSUMER_KEY = SiriB2C.CONSUMER_KEY;
    String CONSUMER_SECRET = SiriB2C.CONSUMER_SECRET;
    String SECURITY_CREDENTIAL = SiriB2C.SECURITY_CREDENTIAL;
    //String SECURITY_CREDENTIAL = "";
    private int globalMpesaId;

    private Logging logger;

    @Override
    public void start(Future<Void> done) throws Exception {
        System.out.println("deploymentId B2Customer =" + vertx.getOrCreateContext().deploymentID());
        EventBus eventBus = vertx.eventBus();
        logger = new Logging();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("TO_MPESA");
        consumer.handler((Message<JsonObject> message) -> {

            String SECURITY_FILE_PATH = System.getProperty("user.dir") + File.separator + "saf_certificate" + File.separator + "prod_env" + File.separator + "cert.cer";
            //token
            String accessToken = B2CUtilities.getAccessToken(CONSUMER_KEY, CONSUMER_SECRET);
            //SECURITY_CREDENTIAL = SecurityCredential.encryptInitiatorPassword(SECURITY_FILE_PATH, "KAPOETA2022!");
            System.out.println("SECURITY_CREDENTIAL : " + SECURITY_CREDENTIAL);

            try {
                // Initialize DBConnection object
                JsonObject data = message.body();
                String amount = data.getString("amount").trim();
                String phonenumber = data.getString("phonenumber").trim();
                String transId = data.getString("transId");
                int mpesaId = data.getInteger("mpesaId");
                globalMpesaId = mpesaId;

                String json = "{\n"
                        + "    \"InitiatorName\": \"" + INITIATOR_NAME + "\",\n"
                        + "    \"SecurityCredential\": \"" + SECURITY_CREDENTIAL + "\",\n"
                        + "    \"CommandID\": \"" + COMMAND_ID + "\",\n"
                        + "    \"Amount\": \"" + amount + "\",\n"
                        + "    \"PartyA\": \"" + SHORTCODE + "\",\n"
                        + "    \"PartyB\": \"" + phonenumber + "\",\n"
                        + "    \"Remarks\": \"" + REMARKS + "\",\n"
                        + "    \"QueueTimeOutURL\": \"" + QUEUETIMEOUT_URL + "\",\n"
                        + "    \"ResultURL\": \"" + RESULT_URL + "\",\n"
                        + "    \"Occasion\": \"" + REMARKS + "\"\n"
                        + "}";

                // log sent to 
                logger.applicationLog(logger.logPreString() + "Req to broker  - " + json + "\n\n", "", 3);

                OkHttpClient client = new OkHttpClient();

                MediaType mediaType = MediaType.parse("application/json");
                RequestBody body = RequestBody.create(mediaType, json);
                Request request = new Request.Builder()
                        .url(B2C_URL_To)
                        .post(body)
                        .addHeader("authorization", "Bearer " + accessToken + "")
                        .addHeader("content-type", "application/json")
                        .build();

                try ( //
                        Response response = client.newCall(request).execute()) {

                    JsonObject ImmediateResponse = new JsonObject(response.body().string());
                    //add MpesaID to the response
                    ImmediateResponse.put("mpesaId", mpesaId);
                    //log response / acknoledgement from saf
                    logger.applicationLog(logger.logPreString() + "Acknowledgement from saf  - " + ImmediateResponse + "\n\n", "", 4);

                    // send to update immediate Response
                    eventBus.send("IMMEDIATE_RESPONSE", ImmediateResponse);
                }

            } catch (IOException ex) {
                JsonObject nullObject = new JsonObject();
                nullObject.put("mpesaId", globalMpesaId);
                eventBus.send("IMMEDIATE_RESPONSE", nullObject);
                logger.applicationLog(logger.logPreString() + "Error sending TO mpesa  - " + ex.getMessage() + "\n\n", "", 5);
            }
        });
    }
}
