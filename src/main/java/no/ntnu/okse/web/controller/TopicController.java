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

import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.web.model.Topics;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 13/03/15.
 *
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private static Logger log = Logger.getLogger(TopicController.class.getName());
    private static long id = 0;

    @RequestMapping(method = RequestMethod.GET)
    public ArrayList<Topics> topics() {
        SubscriptionService ss = SubscriptionService.getInstance();
        TopicService ts = TopicService.getInstance();

        ArrayList<Topics> results = new ArrayList<>();

        HashSet<Topic> allTopics = ts.getAllTopics();

        allTopics.stream()
                .forEach(t -> {
                    results.add(new Topics(t, ss.getAllSubscribersForTopic(t.getFullTopicString())));
                });

        return results;

        /*
        return new Topic(new Random().nextLong(), "testTopic", new ArrayList<Subscriber>(Arrays.asList(
                new Subscriber("128.0.0.1", "8080", "WSN", new HashMap<String, String>()),
                new Subscriber("78.91.14.24", "234", "DDS", new HashMap<String, String>()),
                new Subscriber("192.168.1.1", "58080", "ZeroMQ", new HashMap<String, String>())
        )));
        */
    }

    @RequestMapping(method = RequestMethod.POST, value = "/delete/all")
    public void deleteAll() {
        log.info("Deleting all topics");
        TopicService ts = TopicService.getInstance();
        // Delete all topics
    }

    @RequestMapping(method = RequestMethod.POST, value = "/delete/{id}")
    public void deleteOneTopic(@PathVariable String id) {
        log.info("Deleting Topic with ID: " + id);
        TopicService ts = TopicService.getInstance();
        // Delete single topic
    }

    @RequestMapping(method = RequestMethod.POST, value="/delete/subscriber/{id}")
    public void deleteOneSubscriber(@PathVariable String id) {
        log.info("Deleting subscriber with ID: " + id);
        SubscriptionService ss = SubscriptionService.getInstance();
        // Delete single subscriber

    }



}
