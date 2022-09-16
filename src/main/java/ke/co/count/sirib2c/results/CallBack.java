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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import ke.co.count.sirib2c.log.Logging;
import ke.co.count.sirib2c.services.B2CUtilities;
import ke.co.count.sirib2c.datasource.DBConnection;
import ke.co.count.sirib2c.services.Integrator;
import ke.co.count.sirib2c.util.StatusCode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 *
 * @author ronald.langat
 */
public class CallBack extends AbstractVerticle {

    private Logging logger;
    private final int CALLBACK_SUCCESS = StatusCode.SUCCESS_CALLBACK;
    private final int CALLBACK_FAILED = StatusCode.CALLBACK_FAILED;

    @Override
    public void start(Future<Void> done) throws Exception {
        System.out.println("deploymentId CallBack =" + vertx.getOrCreateContext().deploymentID());
        EventBus eventBus = vertx.eventBus();
        logger = new Logging();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("B2C_CALLBACK");
        consumer.handler((Message<JsonObject> message) -> {
            try {
                JsonObject data = message.body();
                // log success status
                logger.applicationLog(logger.logPreString() + "Callback from saf  - " + data + "\n\n", "", 6);
                //System.out.println("callBack: " + data);
                JsonObject result = data.getJsonObject("Result");

                // if  ResultCode != 0
                Object ResultCode = result.getValue("ResultCode");

                int ResultType = result.getInteger("ResultType");
                String ResultDesc = result.getString("ResultDesc");
                String OriginatorConversationID = result.getString("OriginatorConversationID");
                String ConversationID = result.getString("ConversationID");
                String TransactionID = result.getString("TransactionID");

                // success params
                String TransactionReceipt = "";
                String TransactionCompletedDateTime = "";
                String ReceiverPartyPublicName = "";
                String B2CChargesPaidAccountAvailableFunds = "";
                String B2CRecipientIsRegisteredCustomer = "";

                if (ResultCode instanceof Integer) {
                    int ResultCodeInt = (int) ResultCode;
                    if (ResultCodeInt == 0) { // means success

                        // update status as success
                        B2CUtilities b2cUtil = new B2CUtilities();
                        b2cUtil.updateMpesaStatusByCOnversationID(ConversationID, CALLBACK_SUCCESS);

                        // Get ResultParameters
                        JsonObject jResultParameters = result.getJsonObject("ResultParameters");
                        JsonArray arrayParams = jResultParameters.getJsonArray("ResultParameter");
                        JsonObject resultParameters = new JsonObject();
                        for (Object jParam : arrayParams) {
                            JsonObject param = (JsonObject) jParam;
                            // System.out.println("key: " + param.getValue("Value"));
                            resultParameters.put(param.getString("Key"), String.valueOf(param.getValue("Value")));
                        }
                        // System.out.println("resultParameters: " + resultParameters);

                        TransactionReceipt = resultParameters.getString("TransactionReceipt");
                        TransactionCompletedDateTime = resultParameters.getString("TransactionCompletedDateTime");
                        ReceiverPartyPublicName = resultParameters.getString("ReceiverPartyPublicName");
                        B2CChargesPaidAccountAvailableFunds = resultParameters.getString("B2CChargesPaidAccountAvailableFunds");
                        B2CRecipientIsRegisteredCustomer = resultParameters.getString("B2CRecipientIsRegisteredCustomer");

                    } else {
                        // update status as failed
                        B2CUtilities b2cUtil = new B2CUtilities();
                        b2cUtil.updateMpesaStatusByCOnversationID(ConversationID, CALLBACK_FAILED);
                    }
                } else {
                    // log failed on the safaricom side
                    B2CUtilities b2cUtil = new B2CUtilities();
                    b2cUtil.updateMpesaStatusByCOnversationID(ConversationID, CALLBACK_FAILED);
                }

                String finalRes = "{call UpdateFinalResult(?,?,?,?,?,?,?,?,?,?,?)}";
                DBConnection conn = new DBConnection();

                try {
                    CallableStatement cstm = conn.getConnection().prepareCall(finalRes);
                    cstm.setString("ResultCode", String.valueOf(ResultCode));
                    cstm.setString("ResultType", String.valueOf(ResultType));
                    cstm.setString("ResultDesc", ResultDesc);
                    cstm.setString("OriginatorConversationID", OriginatorConversationID);
                    cstm.setString("ConversationID", ConversationID);
                    cstm.setString("TransactionID", TransactionID);
                    cstm.setString("TransactionReceipt", TransactionReceipt);
                    cstm.setString("TransactionCompletedDateTime", TransactionCompletedDateTime);
                    cstm.setString("ReceiverPartyPublicName", ReceiverPartyPublicName);
                    cstm.setString("B2CChargesPaidAccountAvailableFunds", B2CChargesPaidAccountAvailableFunds);
                    cstm.setString("B2CRecipientIsRegisteredCustomer", B2CRecipientIsRegisteredCustomer);
                    cstm.execute();


                    //Send SMS to party A
                    String sqlQuery = "SELECT mpesa_out.amount,mpesa_out.RES2_ReceiverPartyPublicName,"
                            + "mpesa_in.PhoneNumber, mpesa_in.AccountReference,mpesa_in.TransactionDesc FROM mpesa_in "
                            + "LEFT JOIN mpesa_out ON mpesa_in.R_MpesaReceiptNumber=mpesa_out.txn_no "
                            + "WHERE mpesa_out.RES1_ResultCode = '0' AND mpesa_out.RES1_ConversationID='"+ConversationID+"';";
                    try (Connection con = conn.getConnection();
                            PreparedStatement pst = con.prepareStatement(sqlQuery);
                            ResultSet rs = pst.executeQuery();) {

                        if (rs.next()) {
                            String phoneFrom = rs.getString("PhoneNumber");
                            String phoneTo = rs.getString("AccountReference");
                            String transactiondescription = rs.getString("TransactionDesc");
                            String publicname = rs.getString("RES2_ReceiverPartyPublicName");
                            String mpesaRefNo = TransactionID;
                            String amt = rs.getString("amount");
                            String compileMessage = "Dear Customer, your MPESA transfer of KES."
                                    + amt + " to " + publicname+" has been processed successfully. "
                                    + "MPESA ref number " + mpesaRefNo + ". PRIVPAY, NUMBER YAKO NI SIRI YAKO";


                            String customerMessageNewApp = "Dear Customer you have received " +amt +" from siri yangu reference "
                                    +transactiondescription+ " PRIVPAY, NUMBER YAKO NI SIRI YAKO";

                            String customerMessageOldApp = "Dear Customer you have received KSH "+amt+" from siri yangu reference PRIVPAY, NUMBER YAKO NI SIRI YAKO";


                            JsonObject j = new JsonObject();
                            j.put("phoneTo", phoneFrom);
                            j.put("msg", compileMessage);

                            String jmsg = j.toString();
                            System.out.println(jmsg);
                            System.out.println("send the first message");

                            OkHttpClient client = new OkHttpClient();
                            MediaType mediaType = MediaType.parse("application/json");
                            RequestBody body = RequestBody.create(mediaType, jmsg);
                            Request request = new Request.Builder()
                                    .url("http://localhost:8840/send_sms")
                                    .post(body)
                                    .addHeader("content-type", "application/json")
                                    .build();

                            try (Response response = client.newCall(request).execute()) {
                                JsonObject respSMS = new JsonObject(response.body().string());
                            }

                            JsonObject messageToCustomer = new JsonObject();
                            messageToCustomer.put("phoneTo", phoneTo);
                            if(transactiondescription.contains("Send money from")){
                                messageToCustomer.put("msg", customerMessageOldApp);
                            }else{
                                messageToCustomer.put("msg", customerMessageNewApp);
                            }

                            String jsonmessage = messageToCustomer.toString();
                            System.out.println(jsonmessage);

                            System.out.println("sending second message");
                            OkHttpClient client2 = new OkHttpClient();
                            RequestBody body2 = RequestBody.create(mediaType, jsonmessage);
                            Request request2 = new Request.Builder()
                                    .url("http://localhost:8840/send_sms")
                                    .post(body2)
                                    .addHeader("content-type", "application/json")
                                    .build();

                            try (Response response = client2.newCall(request2).execute()) {
                                JsonObject message2 = new JsonObject(response.body().string());
                            }
//                            try (Response response = client.newCall(request).execute()) {
//                                JsonObject respSMS = new JsonObject(response.body().string());
//                            }

                        }
                    } catch (Exception e) {

                    }

                } catch (SQLException e) {
                    logger.applicationLog(logger.logPreString() + "Error Callback  - " + e.getLocalizedMessage() + "\n\n", "", 5);
                } finally {
                    conn.closeConn();
                }
                //else save ALL successfull
                // System.out.println("" + ResultType + " : " + ResultCode + " : " + TransactionID);
            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "Error Callback  - " + ex.getLocalizedMessage() + "\n\n", "", 5);
            }

        });
    }
}
