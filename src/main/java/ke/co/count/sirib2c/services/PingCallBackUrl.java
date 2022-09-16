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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import ke.co.count.sirib2c.log.Logging;
import ke.co.count.sirib2c.datasource.DBConnection;

/**
 *
 * @author ronald.langat
 */
public class PingCallBackUrl extends AbstractVerticle {

    private Logging logger;

    @Override
    public void start(Future<Void> done) throws Exception {
        System.out.println("deploymentId PingCallBackUrl =" + vertx.getOrCreateContext().deploymentID());
        EventBus eventBus = vertx.eventBus();
        logger = new Logging();

        String RESULT_URL = SiriB2C.RESULT_URL;

        MessageConsumer<JsonObject> consumer = eventBus.consumer("PING_CALLBACK");
        consumer.handler((Message<JsonObject> message) -> {
            JsonObject data = message.body();

            JsonObject pingResults = new JsonObject();
            pingResults.put("available", false);

            int online = 0;
            try {
                final URLConnection connection = new URL(RESULT_URL).openConnection();
                connection.connect();
                online = 1;
                pingResults.put("available", true);
                pingResults.put("description", RESULT_URL + " Callback reachable");
            } catch (final MalformedURLException e) {
                online = 0;
                pingResults.put("available", false);
                pingResults.put("description", e.getMessage());
            } catch (final IOException e) {
                online = 0;
                pingResults.put("available", false);
                pingResults.put("description", e.getMessage());
            }

            Timestamp createdTime = new Timestamp(System.currentTimeMillis());
            // save to database
            DBConnection dbConnection = new DBConnection();
            String sqlInsert = "INSERT INTO call_back_url_status (status,created_at,updated_at) values ('" + online + "','" + createdTime + "','" + createdTime + "')";
            try {
                int insertion = dbConnection.update_db(sqlInsert);
            } catch (Exception e) {
                logger.applicationLog(logger.logPreString() + "Error - " + e.getMessage() + "\n\n", "", 5);
            } finally {
                try {
                    dbConnection.closeConn();
                } catch (Exception e) {
                    logger.applicationLog(logger.logPreString() + "Error - " + e.getMessage() + "\n\n", "", 5);
                }
            }

        });
    }
}
