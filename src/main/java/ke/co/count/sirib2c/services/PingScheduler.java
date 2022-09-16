/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ke.co.count.sirib2c.services;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import java.util.TimerTask;

/**
 *
 * @author ronald.langat
 */
public class PingScheduler extends TimerTask {

    Vertx vertx;

    public PingScheduler(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void run() {
        startSchedule();
    }

    public void startSchedule() {
        EventBus eventBus = vertx.eventBus();

        JsonObject starterObject = new JsonObject();
        starterObject.put("start", "start");

        eventBus.send("PING_CALLBACK", starterObject);
    }

}
