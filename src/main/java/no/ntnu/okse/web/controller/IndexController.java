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

import org.springframework.beans.factory.annotation.Value;
import no.ntnu.okse.Application;
import no.ntnu.okse.core.event.PageLoadEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@Controller
public class IndexController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private String port;

    private Properties environment = System.getProperties();

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("projectName", appName);
        model.addAttribute("serverPort", port);
        model.addAttribute("environment", createEnvironmentList());

        try {
            model.addAttribute("serverAddress", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            model.addAttribute("serverAddress", "Unknown");
        }

        Application.cs.getExecutor().execute(() -> {
            try {
                Application.cs.getEventQueue().put(new PageLoadEvent("PageLoad", "CorrectDataObject", "String"));
            } catch (InterruptedException e) {
                Logger.getLogger(Application.class.getName()).info(e.getMessage());
            } catch (IllegalArgumentException e1) {
                Logger.getLogger(Application.class.getName()).info(e1.getMessage());
            }
        });
        return "fragments/index";

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

