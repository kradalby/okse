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

import no.ntnu.okse.core.subscription.SubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import no.ntnu.okse.Application;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@Controller
public class IndexController {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(IndexController.class.getName());

    @Value("${spring.application.name}")
    private String appName;

    private Properties environment = System.getProperties();

    @RequestMapping("/")
    public String index(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof AnonymousAuthenticationToken) {
            // The user is not logged in
            return "fragments/indexNotLoggedIn";
        }

        SubscriptionService ss = SubscriptionService.getInstance();

        model.addAttribute("projectName", appName);
        model.addAttribute("environment", createEnvironmentList());
        model.addAttribute("subscribers", ss.getNumberOfSubscribers());
        model.addAttribute("publishers", ss.getNumberOfPublishers());
        model.addAttribute("runtime", Application.getRunningTime().toString());

        HashSet<String> ipAddresses = new HashSet<>();

        String ip;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iFace = interfaces.nextElement();
                // Filters out 127.0.0.1 and inactive interfaces
                if (iFace.isLoopback() || !iFace.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iFace.getInetAddresses();
                iFace.getInterfaceAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // We are only interested in Inet4Addresses
                    if (addr instanceof Inet6Address)
                        continue;
                    ip = addr.getHostAddress();
                    ipAddresses.add(ip);
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        model.addAttribute("serverIpAddresses", ipAddresses);

        return "fragments/indexLoggedIn";

    }

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

