/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import java.util.Timer;
import ke.co.count.sirib2c.log.Logging;
import ke.co.count.sirib2c.services.ProcessTransactions;
import ke.co.count.sirib2c.services.SendMoney;
import ke.co.count.sirib2c.services.GetTransactions;
import ke.co.count.sirib2c.services.PingCallBackUrl;
import ke.co.count.sirib2c.services.PingScheduler;
import ke.co.count.sirib2c.services.ServiceStarter;
import ke.co.count.sirib2c.util.Prop;
import ke.co.count.sirib2c.results.CallBack;
import ke.co.count.sirib2c.results.ImmediateResponse;

/**
 *
 * @author Benart
 */
public class SiriB2C extends AbstractVerticle {

    public static Prop props;
    public static Logging logger;
    public static String LOGS_PATH;
    public static String DATABASE_DRIVER;
    public static String DATABASE_IP;
    public static String DATABASE_PORT;
    public static String DATABASE_NAME;
    public static String DATABASE_USER;
    public static String DATABASE_PASSWORD;
    public static String DATABASE_SERVER_TIME_ZONE;
    public static String SYSTEM_PORT;
    public static String SYSTEM_HOST;
    // ----- B2C ----------------
    public static String SHORTCODE;
    public static String COMMAND_ID;
    public static String QUEUETIMEOUT_URL;
    public static String RESULT_URL;
    public static String INITIATOR_NAME;
    public static String B2C_URL_To;
    public static String CONSUMER_KEY;
    public static String CONSUMER_SECRET;
    public static String SECURITY_CREDENTIAL;
    public static String TOKEN_URL;
    // Transaction status
    public static String TRANSACTION_STATUS_URL;
    public static String TRANSACTION_STATUS_RESULT_URL;

    // Hikari Setup
    static int MAX_POOL_SIZE = 3;
    static int MAX_IDLE_TIME = 4;
    static int WORKER_POOL_SIZE = 6;
    static int TIMEOUT_TIME = 50000;
    static int INITIAL_POOL_SIZE = 2;

    static {
        props = new Prop();
        logger = new Logging();
        LOGS_PATH = "";
        DATABASE_DRIVER = "";
        DATABASE_IP = "";
        DATABASE_PORT = "";
        DATABASE_NAME = "";
        DATABASE_USER = "";
        DATABASE_PASSWORD = "";
        DATABASE_SERVER_TIME_ZONE = "";
        SYSTEM_PORT = "";
        SYSTEM_HOST = "";
        //-----B2C ------
        SHORTCODE = "";
        COMMAND_ID = "";
        QUEUETIMEOUT_URL = "";
        RESULT_URL = "";
        INITIATOR_NAME = "";
        B2C_URL_To = "";
        CONSUMER_KEY = "";
        CONSUMER_SECRET = "";
        SECURITY_CREDENTIAL = "";
        TOKEN_URL = "";

        TRANSACTION_STATUS_URL = "";
        TRANSACTION_STATUS_RESULT_URL = "";

    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        // instatiate Properties and Logging classes
        props = new Prop();
        logger = new Logging();

        // Get properties from property file
        LOGS_PATH = props.getLogsPath();
        DATABASE_DRIVER = props.getDATABASE_DRIVER();
        DATABASE_IP = props.getDATABASE_IP();
        DATABASE_PORT = props.getDATABASE_PORT();
        DATABASE_NAME = props.getDATABASE_NAME();
        DATABASE_USER = props.getDATABASE_USER();
        DATABASE_PASSWORD = props.getDATABASE_PASSWORD();
        DATABASE_SERVER_TIME_ZONE = props.getDATABASE_SERVER_TIME_ZONE();
        SYSTEM_PORT = props.getSYSTEM_PORT();
        SYSTEM_HOST = props.getSYSTEM_HOST();

        SHORTCODE = props.getSHORTCODE();
        COMMAND_ID = props.getCOMMAND_ID();
        QUEUETIMEOUT_URL = props.getQUEUETIMEOUT_URL();
        RESULT_URL = props.getRESULT_URL();
        TOKEN_URL = props.getTOKEN_URL();
        INITIATOR_NAME = props.getINITIATOR_NAME();
        B2C_URL_To = props.getB2C_URL_To();
        CONSUMER_KEY = props.getCONSUMER_KEY();
        CONSUMER_SECRET = props.getCONSUMER_SECRET();
        SECURITY_CREDENTIAL = props.getSECURITY_CREDENTIAL();

        TRANSACTION_STATUS_URL = props.getTRANSACTION_STATUS_URL();
        TRANSACTION_STATUS_RESULT_URL = props.getTRANSACTION_STATUS_RESULT_URL();

        // Deployment options
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(1)
                .setWorkerPoolName("mpesa-thread")
                .setWorker(true)
                .setWorkerPoolSize(40)
                .setHa(true);

        // deploy Vertices Here 
        vertx.deployVerticle(SiriB2C.class.getName(), options);
        vertx.deployVerticle(GetTransactions.class.getName(), options);
        vertx.deployVerticle(ProcessTransactions.class.getName(), options);
        vertx.deployVerticle(SendMoney.class.getName(), options);
        vertx.deployVerticle(CallBack.class.getName(), options);
        vertx.deployVerticle(ImmediateResponse.class.getName(), options);
        vertx.deployVerticle(PingCallBackUrl.class.getName(), options);

        // start starter
        ServiceStarter sp = new ServiceStarter(vertx);
        Timer schede = new Timer();
        schede.schedule(sp, 1000, 10000); //after 10 seconds

        // ping scheduler
        PingScheduler ps = new PingScheduler(vertx);
        Timer schedele = new Timer();
        schedele.schedule(ps, 1000, 60000);  //after 60 seconds
    }

    @Override
    public void start(Future<Void> start_application) {
        EventBus eventBus = vertx.eventBus();
        int port = Integer.parseInt(SYSTEM_PORT);
        String host = SYSTEM_HOST;
        HttpServer ovHttpServer;
        ovHttpServer = vertx.createHttpServer();

        ovHttpServer.requestHandler(request -> {
            HttpServerResponse response = request.response();
            response.headers()
                    .add("Content-Type", "application/json")
                    .add("Access-Control-Allow-Origin", "*")
                    .add("Access-Control-Allow-Headers", "*")
                    .add("Access-Control-Allow-Methods", "*")
                    .add("Access-Control-Allow-Credentials", "true");
            String method = request.method().name();
            System.err.println(method);
            
            String path = request.path();

            request.bodyHandler(bodyHandler -> {
                String body = bodyHandler.toString();
                JsonObject responseOBJ = new JsonObject();
                if ("POST".equalsIgnoreCase(method)) {
                    JsonObject data = new JsonObject(body);
                    if (path.endsWith("/mpesa_channel")) {
                        try {
                            DeliveryOptions deliveryOptions = new DeliveryOptions()
                                    .setSendTimeout(20000);

                            //System.out.println("data: " + data);
                            String processingCode = data.getString("processingCode");
                            System.out.println("processingCode: " + processingCode);
                            eventBus.send(processingCode, data);
                            responseOBJ.put("response_code", "00")
                                    .put("response", " received");
                            response.end(responseOBJ.toString());

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            responseOBJ.put("response_code", "901")
                                    .put("response", "error occured || exception");
                            response.end(responseOBJ.toString());
                        }
                    } else if (path.endsWith("/b2c_callback")) {

                        try {
                            DeliveryOptions deliveryOptions = new DeliveryOptions()
                                    .setSendTimeout(20000);

                            eventBus.send("B2C_CALLBACK", data);
                            responseOBJ.put("response_code", "00")
                                    .put("response", " received");
                            response.end(responseOBJ.toString());

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            responseOBJ.put("response_code", "901")
                                    .put("response", "error occured || exception");
                            response.end(responseOBJ.toString());
                        }

                    } else if (path.endsWith("/query_callback")) {

                        try {
                            DeliveryOptions deliveryOptions = new DeliveryOptions()
                                    .setSendTimeout(20000);

                            eventBus.send("QUERY_CALLBACK", data);
                            //responseOBJ.put("response_code", "00")
                            //        .put("response", " received");
                            //response.end(responseOBJ.toString());

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            responseOBJ.put("response_code", "901")
                                    .put("response", "error occured || exception");
                            response.end(responseOBJ.toString());
                        }

                    } else {
                        // Unknown path
                        responseOBJ.put("response_code", "404")
                                .put("response", "Invalid path");
                        response.end(responseOBJ.toString());
                    }
                } else {
                    // wrong request method
                    responseOBJ.put("response_code", "901")
                            .put("response", "Bad Request");
                    response.end(responseOBJ.toString());
                }
            });
        });

        ovHttpServer.listen(port, resp -> {
            if (resp.succeeded()) {
                System.out.println("System listening at " + host + ":" + port);
            } else {
                System.out.println("System failed to start !!" + resp.failed());
            }
        });

    }
}
