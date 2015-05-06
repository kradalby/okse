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
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.TopicService;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.lang.String;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@RestController
public class MainController {

    public static int MB = 1024 * 1024;

    private static Logger log = Logger.getLogger(MainController.class.getName());

    /**
     * Returns all necessary information for rendering the main-pane
     * @return A HashMap containing all the information
     */
    @RequestMapping(value = "/api/main", method = RequestMethod.GET)
    public @ResponseBody HashMap<String, Object> main() {
        SubscriptionService ss = SubscriptionService.getInstance();
        TopicService ts = TopicService.getInstance();

        long totalRam = Runtime.getRuntime().totalMemory();
        long freeRam = Runtime.getRuntime().freeMemory();

        HashMap<String, Object> result = new HashMap<String, Object>(){{
            put("subscribers", ss.getNumberOfSubscribers());
            put("publishers", ss.getNumberOfPublishers());
            put("topics", ts.getTotalNumberOfTopics());
            put("uptime", Utilities.getDurationAsISO8601(Application.getRunningTime()));
            // Runtime statistics
            put("runtimeStatistics", new HashMap<String, Object>(){{
                put("cpuAvailable", Runtime.getRuntime().availableProcessors());
                put("totalRam", totalRam / MB);
                put("freeRam", freeRam / MB);
                put("usedRam", (totalRam - freeRam) / MB);
            }});
            // Protocol information
            ArrayList<HashMap<String, Object>> protocols = new ArrayList<>();
            Application.cs.getAllProtocolServers().forEach(p -> {
                protocols.add(new HashMap<String, Object>(){{
                    put("host", p.getHost());
                    put("port", p.getPort());
                    put("type", p.getProtocolServerType());
                }});
            });
            put("protocols", protocols);
        }};

        return result;
    }

}