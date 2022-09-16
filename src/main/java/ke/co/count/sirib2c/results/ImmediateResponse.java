/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.results;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import java.sql.CallableStatement;
import java.sql.SQLException;
import ke.co.count.sirib2c.log.Logging;
import ke.co.count.sirib2c.services.B2CUtilities;
import ke.co.count.sirib2c.datasource.DBConnection;
import ke.co.count.sirib2c.util.StatusCode;

/**
 *
 * @author ronald.langat
 */
public class ImmediateResponse extends AbstractVerticle {

    private Logging logger;
    private final int STATUSID_FAILED = StatusCode.FAILED_TO_SENT;
    private final int STATUSID_FAILED_ON_SAF = StatusCode.CALLBACK_FAILED;
    private final int STATUSID_SUCCESS = StatusCode.ACKNOWLEDGED;

    private int globalMpesaId;
    
    @Override
    public void start(Future<Void> done) throws Exception {
        System.out.println("deploymentId CallBack =" + vertx.getOrCreateContext().deploymentID());
        EventBus eventBus = vertx.eventBus();
        logger = new Logging();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("IMMEDIATE_RESPONSE");
        consumer.handler((Message<JsonObject> message) -> {
            JsonObject result = message.body();

            if (result != null) {
                // get Core table unique ID
                globalMpesaId = result.getInteger("mpesaId");

                if (result.containsKey("ResponseCode")) {

                    if ("0".equalsIgnoreCase(result.getString("ResponseCode"))) {
                        // update status to 3
                        B2CUtilities b2cUtil = new B2CUtilities();
                        b2cUtil.updateMpesaStatus(result.getInteger("mpesaId"), STATUSID_SUCCESS);

                    } else {
                        // update status to 2
                        B2CUtilities b2cUtil = new B2CUtilities();
                        b2cUtil.updateMpesaStatus(result.getInteger("mpesaId"), STATUSID_FAILED_ON_SAF);
                    }
                    
                    try {
                        String ResponseCode = result.getString("ResponseCode");
                        String ConversationID = result.getString("ConversationID");
                        String OriginatorConversationID = result.getString("OriginatorConversationID");
                        String ResponseDescription = result.getString("ResponseDescription");
                        

                        String updateResponse = "{call UpdateImmediateResponse(?,?,?,?,?)}";
                        DBConnection conn = new DBConnection();
                        try {
                            CallableStatement cstm = conn.getConnection().prepareCall(updateResponse);
                            cstm.setString("ResponseCode", ResponseCode);
                            cstm.setString("ConversationID", ConversationID);
                            cstm.setString("OriginatorConversationID", OriginatorConversationID);
                            cstm.setString("ResponseDescription", ResponseDescription);
                            cstm.setObject("MpesaID", globalMpesaId);

                            cstm.execute();
                        } catch (SQLException ex) {
                            logger.applicationLog(logger.logPreString() + "Error database:   - "+ex.getMessage()+" \n\n", "", 9);
                        } finally {
                            conn.closeConn();
                        }
                    } catch (Exception ex) {
                       logger.applicationLog(logger.logPreString() + "Error database:   - "+ex.getMessage()+" \n\n", "", 5);
                    }
                } else if (result.containsKey("errorCode")) {
                    // update as No response from safaricom .. to 2
                    B2CUtilities b2cUtil = new B2CUtilities();
                    b2cUtil.updateMpesaStatus(globalMpesaId, STATUSID_FAILED_ON_SAF);

                    // get Error Code & update Core table with error Code
                    String errorCode = result.getString("errorCode");
                    String errorMessage = result.getString("errorMessage");

                    String updateResponse = "{call UpdateImmediateResponse(?,?,?,?,?)}";
                    DBConnection conn = new DBConnection();
                    try {
                        CallableStatement cstm = conn.getConnection().prepareCall(updateResponse);
                        cstm.setString("ResponseCode", errorCode);
                        cstm.setString("ConversationID", "Error " + errorCode);
                        cstm.setString("OriginatorConversationID", "Error " + errorCode);
                        cstm.setString("ResponseDescription", errorMessage);
                        cstm.setObject("id", globalMpesaId);

                        cstm.execute();
                    } catch (SQLException ex) {
                        logger.applicationLog(logger.logPreString() + "Error database:   - "+ex.getMessage()+" \n\n", "", 9);
                    } finally {
                        conn.closeConn();
                    }

                } else {
                    // update as No response from safaricom .. to 2
                    B2CUtilities b2cUtil = new B2CUtilities();
                    b2cUtil.updateMpesaStatus(globalMpesaId, STATUSID_FAILED);
                }
            } else {
                // update as No response from safaricom .. to 2
                B2CUtilities b2cUtil = new B2CUtilities();
                b2cUtil.updateMpesaStatus(globalMpesaId, STATUSID_FAILED);
            }
        });

    }

}
