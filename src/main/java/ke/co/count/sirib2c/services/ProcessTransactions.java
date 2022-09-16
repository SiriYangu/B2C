/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.services;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import ke.co.count.sirib2c.log.Logging;
import ke.co.count.sirib2c.datasource.DBConnection;
import ke.co.count.sirib2c.util.StatusCode;

/**
 *
 * @author ronald.langat
 */
public class ProcessTransactions extends AbstractVerticle {

    private Logging logger;
    private final int PICKED_STATUS = StatusCode.FETCH_STATUS;

    @Override
    public void start(Future<Void> done) throws Exception {
        System.out.println("deploymentId B2CProcess =" + vertx.getOrCreateContext().deploymentID());
        EventBus eventBus = vertx.eventBus();
        logger = new Logging();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("2002");
        consumer.handler((Message<JsonObject> message) -> {
            // Initialize DBConnection object
            DBConnection con = new DBConnection();
            try {
                //fetch data from B2CRequest
                String fetch = "{call [GetMpesaTransactions] ()}";
                CallableStatement stmt = con.getConnection().prepareCall(fetch);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    //update As Picked first ... then proceed to post it to mpesa handler
                    int isUpdateSuccessfull = 0;
                    try {
                        B2CUtilities b2cUtil = new B2CUtilities();
                        isUpdateSuccessfull = b2cUtil.updateMpesaStatus(rs.getInt("id"), PICKED_STATUS);
                    } catch (SQLException e) {
                        logger.applicationLog(logger.logPreString() + "Error database  - "+e.getMessage()+" \n\n", "", 9);
                    } finally {
                        //close conection
                    }

                    // if the update was successfull then post to "TO_MPESA" Address
                    if (isUpdateSuccessfull == 1) {
                        // proceed to create object to send to Mpesa
                        JsonObject sendObject = new JsonObject();
                        String phone = rs.getString("phonenumber");
                        sendObject.put("mpesaId", rs.getInt("id"));
                        sendObject.put("transId", rs.getString("txn_no"));
                        sendObject.put("amount", rs.getString("amount"));
                        sendObject.put("phonenumber", "254" + phone.substring(phone.length() - 9, phone.length())); //"254"+pho.substring(pho.length()-9, pho.length())      

                        // log picked from the core table
                        logger.applicationLog(logger.logPreString() + "picked from mpesa_out table  - " + sendObject + "\n\n", "", 7);

                        // send to sender service
                        eventBus.send("TO_MPESA", sendObject);
                    }else{
                       // log error update to database
                       logger.applicationLog(logger.logPreString() + "Error Updating database  - \n\n", "", 9);
                    }
                } // end while loop
                con.closeConn();

            } catch (SQLException ex) {
                logger.applicationLog(logger.logPreString() + "Error database:   - "+ex.getMessage()+" \n\n", "", 9);
            } finally {
                con.closeConn();
            }
        });
    }
}
