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
import no.ntnu.okse.web.model.Subscriber;
import no.ntnu.okse.web.model.Topic;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;


/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 13/03/15.
 *
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private static Logger log = Logger.getLogger(TopicController.class.getName());

    private int counter = 0;

    @RequestMapping(method = RequestMethod.GET)
    public Topic topics() {
        log.info("Subscribers: " + Application.cs.getSubscriptionService().getAllSubscribers());
        if (counter % 5 == 0) {
            counter++;
            return new Topic(new Random().nextLong(), "myTopic", new ArrayList<Subscriber>(Arrays.asList(
                    new Subscriber("127.0.1.1", "765", "mqtt", new HashMap<String, String>()),
                    new Subscriber("0.0.0.0", "60618", "WSN", new HashMap<String, String>()),
                    new Subscriber("localhost", "235", "amqp", new HashMap<String, String>()),
                    new Subscriber("127.0.0.1", "324", "ZeroMQ", new HashMap<String, String>())
            )));
        } else {
            counter++;
            return new Topic(new Random().nextLong(), "testTopic", new ArrayList<Subscriber>(Arrays.asList(
                    new Subscriber("128.0.0.1", "8080", "WSN", new HashMap<String, String>()),
                    new Subscriber("78.91.14.24", "234", "DDS", new HashMap<String, String>()),
                    new Subscriber("192.168.1.1", "58080", "ZeroMQ", new HashMap<String, String>())
            )));
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/delete/all")
    public void deleteAll() {
        log.info("Deleting all topics");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/delete/{id}")
    public void deleteOneTopic(@PathVariable String id) {
        log.info("Deleting topic with ID: " + id);
    }

    @RequestMapping(method = RequestMethod.POST, value="/delete/subscriber/{id}")
    public void deleteOneSubscriber(@PathVariable String id) {
        log.info("Deleting subscriber with ID: " + id);
    }



}
