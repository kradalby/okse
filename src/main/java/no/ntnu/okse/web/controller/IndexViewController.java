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
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.protocol.amqp.AMQProtocolServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@Controller
public class IndexViewController {

    // Log4j logger
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(IndexViewController.class.getName());

    // Properties from the okse.properties file
    @Value("${spring.application.name}")
    private String appName;
    @Value("${ADMIN_PANEL_HOST}")
    private String serverHost;
    @Value("${server.port}")
    private int serverPort;

    // System information
    private Properties environment = System.getProperties();

    /**
     * This method returns the view to render when a user tries to reach the '/'-url
     *
     * @param model The model to configure
     * @return A string telling what HTML fragment to render
     */
    @RequestMapping("/")
    public String index(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof AnonymousAuthenticationToken) {
            // The user is not logged in
            return "fragments/indexNotLoggedIn";
        }

        SubscriptionService ss = SubscriptionService.getInstance();
        TopicService ts = TopicService.getInstance();
        CoreService cs = CoreService.getInstance();

        long totalRam = Runtime.getRuntime().totalMemory();
        long freeRam = Runtime.getRuntime().freeMemory();

        model.addAttribute("projectName", appName + " (" + Application.VERSION + ")");
        model.addAttribute("environment", createEnvironmentList());
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("serverHost", serverHost);
        model.addAttribute("subscribers", ss.getNumberOfSubscribers());
        model.addAttribute("publishers", ss.getNumberOfPublishers());
        model.addAttribute("topics", ts.getTotalNumberOfTopics());
        model.addAttribute("totalMessages", cs.getTotalMessagesSentFromProtocolServers());
        model.addAttribute("uptime", Utilities.getDurationAsISO8601(Application.getRunningTime()));
        model.addAttribute("cpuCores", Runtime.getRuntime().availableProcessors());
        model.addAttribute("totalRam", totalRam / MainController.MB);
        model.addAttribute("freeRam", freeRam / MainController.MB);
        model.addAttribute("usedRam", (totalRam - freeRam) / MainController.MB);
        model.addAttribute("useQueue", AMQProtocolServer.getInstance().useQueue);

        ArrayList<HashMap<String, Object>> protocols = new ArrayList<>();

        Application.cs.getAllProtocolServers().forEach(p -> {
            protocols.add(new HashMap<String, Object>() {{
                put("host", p.getHost());
                put("port", p.getPort());
                put("type", p.getProtocolServerType());
            }});
        });

        model.addAttribute("protocols", protocols);
        model.addAttribute("protocolPower", cs.protocolServersBooted);

        return "fragments/indexLoggedIn";

    }

    /**
     * Private helper method that creates a HashMap of some environment specifics
     *
     * @return A HashMap containing JAVA information etc.
     */
    private HashMap<String, String> createEnvironmentList() {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("Java Runtime Name", environment.getProperty("java.runtime.name"));
        properties.put("Java Runtime Version", environment.getProperty("java.runtime.version"));
        properties.put("Java VM Name", environment.getProperty("java.vm.name"));
        properties.put("Java VM Version", environment.getProperty("java.vm.version"));
        properties.put("OS Name", environment.getProperty("os.name"));
        properties.put("OS Version", environment.getProperty("os.version"));
        properties.put("OS Architecture", environment.getProperty("os.arch"));
        return properties;
    }
}

