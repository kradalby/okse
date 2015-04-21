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

    private static final String DELETE_ALL_TOPICS = "/delete/all";
    private static final String DELETE_SINGLE_TOPIC = "/delete/{id}";
    private static final String DELETE_SINGLE_SUBSCRIBER = "/delete/subscriber/{id}";

    private static Logger log = Logger.getLogger(TopicController.class.getName());


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
    }

    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_ALL_TOPICS)
    public void deleteAllTopics() {
        log.info("Deleting all topics");
        TopicService ts = TopicService.getInstance();
        // Delete all topics
    }

    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_SINGLE_TOPIC)
    public @ResponseBody Topic deleteSingleTopic(@PathVariable("id") String id) {
        log.info("Deleting Topic with ID: " + id);
        TopicService ts = TopicService.getInstance();
        // Delete single topic
        return null;
    }

    @RequestMapping(method = RequestMethod.DELETE, value=DELETE_SINGLE_SUBSCRIBER)
    public @ResponseBody Subscriber deleteSingleSubscriber(@PathVariable("id") String id) {
        log.info("Deleting subscriber with ID: " + id);
        SubscriptionService ss = SubscriptionService.getInstance();
        // Delete single subscriber
        return new Subscriber("localhost", 8080, "no/ffi", "AMQP");


    }



}
