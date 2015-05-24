/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.web.controller;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.event.SystemEvent;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.TopicService;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping(value = "/api/main")
public class MainController {

    // URL routes
    private static final String MAIN_API = "/get/all";
    private static final String PROTOCOL_POWER = "/protocols/power";

    // Public static field representing a MB
    public static int MB = 1024 * 1024;

    // Log4j logger
    private static Logger log = Logger.getLogger(MainController.class.getName());

    /**
     * Returns all necessary information for rendering the main-pane
     *
     * @return A HashMap containing all the information
     */
    @RequestMapping(method = RequestMethod.GET, value = MAIN_API)
    public
    @ResponseBody
    HashMap<String, Object> main() {
        SubscriptionService ss = SubscriptionService.getInstance();
        TopicService ts = TopicService.getInstance();
        CoreService cs = CoreService.getInstance();

        long totalRam = Runtime.getRuntime().totalMemory();
        long freeRam = Runtime.getRuntime().freeMemory();

        HashMap<String, Object> result = new HashMap<String, Object>() {{
            put("subscribers", ss.getNumberOfSubscribers());
            put("publishers", ss.getNumberOfPublishers());
            put("topics", ts.getTotalNumberOfTopics());
            put("totalMessages", cs.getTotalMessagesSentFromProtocolServers());
            put("uptime", Utilities.getDurationAsISO8601(Application.getRunningTime()));
            // Runtime statistics
            put("runtimeStatistics", new HashMap<String, Object>() {{
                put("cpuAvailable", Runtime.getRuntime().availableProcessors());
                put("totalRam", totalRam / MB);
                put("freeRam", freeRam / MB);
                put("usedRam", (totalRam - freeRam) / MB);
            }});
            // Protocol information
            ArrayList<HashMap<String, Object>> protocols = new ArrayList<>();
            Application.cs.getAllProtocolServers().forEach(p -> {
                protocols.add(new HashMap<String, Object>() {{
                    put("host", p.getHost());
                    put("port", p.getPort());
                    put("type", p.getProtocolServerType());
                }});
            });
            put("protocols", protocols);
            put("protocolPower", cs.protocolServersBooted);
        }};

        return result;
    }

    /**
     * This method turns on/off the protocol servers registered in the CoreService
     *
     * @return A message telling if the protocol servers are booted or not
     */
    @RequestMapping(method = RequestMethod.POST, value = PROTOCOL_POWER)
    public
    @ResponseBody
    String powerProtocolServers() {
        CoreService cs = CoreService.getInstance();

        if (cs.protocolServersBooted) {
            try {
                cs.getEventQueue().put(new SystemEvent(SystemEvent.Type.SHUTDOWN_PROTOCOL_SERVERS, null));
            } catch (InterruptedException e) {
                log.warn("WARNING: Please don't interrupt the thread");
            }
        } else {
            try {
                cs.getEventQueue().put(new SystemEvent(SystemEvent.Type.BOOT_PROTOCOL_SERVERS, null));
            } catch (InterruptedException e) {
                log.warn("WARNING: Please don't interrupt the thread");
            }
        }
        return "{ \"power\":" + cs.protocolServersBooted + " }";
    }

}