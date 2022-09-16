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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ke.co.count.sirib2c.log.Logging;
import ke.co.count.sirib2c.datasource.DBConnection;

/**
 *
 * @author ronald.langat
 */
public class GetTransactions extends AbstractVerticle {

    private Logging logger;

    @Override
    public void start(Future<Void> done) throws Exception {
        System.out.println("deploymentId FetchTxn =" + vertx.getOrCreateContext().deploymentID());
        EventBus eventBus = vertx.eventBus();
        logger = new Logging();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("2001");
        consumer.handler((Message<JsonObject> message) -> {

            DBConnection dbCon = new DBConnection();
            try {
                JsonObject data = message.body();
                String start = data.getString("start");

                //List to hold the transactions
                JsonArray arrayDataHolder = new JsonArray();

                // Before fetching data check if callback is online 
                boolean isOnline = B2CUtilities.pingURL(SiriB2C.RESULT_URL);
                if (isOnline) {
                    //fetch data from B2CRequest
                    String fetch = "{call [GetMpesaRequests] ()}";
                    CallableStatement stmt = dbCon.getConnection().prepareCall(fetch);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        // Define new Json Object
                        JsonObject objectDataHolder = new JsonObject();
                        objectDataHolder.put("id", rs.getInt("id"));
                        objectDataHolder.put("txn_no", rs.getString("R_MpesaReceiptNumber"));
                        objectDataHolder.put("Amount", rs.getString("Amount"));
                        objectDataHolder.put("phonenumber", rs.getString("AccountReference"));
                        objectDataHolder.put("ConvenienceFee", rs.getString("ConvenienceFee"));

                        // save to mpesa txns table
                        // picked id
                        int pickedId = rs.getInt("id");
                        Double amount = Double.parseDouble(rs.getString("Amount"));
                        Double convenienceFee = Double.parseDouble(rs.getString("ConvenienceFee"));
                        Double total = amount-convenienceFee;

                        logger.applicationLog(logger.logPreString() + "picked from transactions table - " + objectDataHolder + "\n\n", "", 1);
                        // save to Mpesa Core table
                        DBConnection con = new DBConnection();
                        try {
                            String createTxn = "{call [CreateMpesaTransactions] (?,?,?)}";
                            CallableStatement stmtTxn = con.getConnection().prepareCall(createTxn);
                            String amt = total.toString();
                            stmtTxn.setString("transId", rs.getString("R_MpesaReceiptNumber"));
                            stmtTxn.setString("phonenumber", rs.getString("AccountReference"));
                            stmtTxn.setString("amt",amt);

                            stmtTxn.execute();
                            con.closeConn();
                        } catch (SQLException ex) {
                            logger.applicationLog(logger.logPreString() + "Error Updating database  - " + ex.getMessage() + "\n\n", "", 9);
                        } finally {
                            con.closeConn();
                        }

                        //log writen to core table ********
                        logger.applicationLog(logger.logPreString() + "saved to mpesa_out table  - " + objectDataHolder + "\n\n", "", 2);
                        //  selected record as 7
                        DBConnection connect = new DBConnection();
                        try {
                            String updateSumary = "{call [UpdateMpesaRequests] (?)}";
                            CallableStatement stmtUpdateSummary = connect.getConnection().prepareCall(updateSumary);
                            stmtUpdateSummary.setInt("id", pickedId);
                            stmtUpdateSummary.execute();
                        } catch (SQLException e) {
                            logger.applicationLog(logger.logPreString() + "Error  - " + e.getMessage() + "\n\n", "", 9);
                        } finally {
                            connect.closeConn();
                        }
                    }
                } else {
                    logger.applicationLog(logger.logPreString() + "Error: - Callback URL (" + SiriB2C.RESULT_URL + ") is not reachable\n\n", "", 8);
                }
            } catch (SQLException ex) {
                logger.applicationLog(logger.logPreString() + "Error  - " + ex.getMessage() + "\n\n", "", 9);
            } finally {
                dbCon.closeConn();
            }
        });
    }
}
