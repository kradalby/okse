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

import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.protocol.ProtocolServer;
import no.ntnu.okse.web.model.ProtocolStats;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Fredrik on 13/03/15.
 */

@RestController
@RequestMapping(value = "/api/statistics")
public class StatsController {

    // URL routes
    private static final String GET_STATS = "/get/all";

    // Log4j logger
    private static Logger log = Logger.getLogger(StatsController.class.getName());

    /**
     * Returnes all the information needed to refresh the stats-pane
     *
     * @return A HashMap containing all the information
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_STATS)
    public
    @ResponseBody
    HashMap<String, Object> getAllStats() {
        CoreService cs = CoreService.getInstance();
        SubscriptionService ss = SubscriptionService.getInstance();
        TopicService ts = TopicService.getInstance();

        HashMap<String, Object> result = new HashMap<>();

        // CoreService statistics
        result.put("coreServiceStatistics", new HashMap<String, Object>() {{
            put("totalMessagesSent", cs.getTotalMessagesSentFromProtocolServers());
            put("totalMessagesReceived", cs.getTotalMessagesReceivedFromProtocolServers());
            put("totalRequests", cs.getTotalRequestsFromProtocolServers());
            put("totalBadRequests", cs.getTotalBadRequestsFromProtocolServers());
            put("totalErrors", cs.getTotalErrorsFromProtocolServers());
            put("publishers", ss.getNumberOfPublishers());
            put("subscribers", ss.getNumberOfSubscribers());
            put("topics", ts.getTotalNumberOfTopics());
        }});

        // ProtocolServer statistics
        ArrayList<ProtocolServer> protocols = cs.getAllProtocolServers();
        ArrayList<ProtocolStats> protocolStats = new ArrayList<>();

        protocols.forEach(p -> {
            protocolStats.add(new ProtocolStats(
                    p.getProtocolServerType(),
                    p.getTotalMessagesSent(),
                    p.getTotalMessagesReceived(),
                    p.getTotalRequests(),
                    p.getTotalBadRequests(),
                    p.getTotalErrors()
            ));
        });
        result.put("protocolServerStatistics", protocolStats);

        return result;
    }
}




