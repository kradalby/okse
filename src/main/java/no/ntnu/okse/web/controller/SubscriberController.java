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
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 30/04/15.
 *
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/subscriber")
public class SubscriberController {

    // URL routes
    private static final String GET_ALL_SUBSCRIBERS = "/get/all";
    private static final String DELETE_SINGLE_SUBSCRIBER = "/delete/single";
    private static final String DELETE_ALL_SUBSCRIBERS = "/delete/all";

    // Log4j logger
    private static Logger log = Logger.getLogger(SubscriberController.class.getName());

    /**
     * This method returns all subscribers registered in the SubscriptionService
     * @return A response body to be serialized to JSON, containing all the subscribers
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_ALL_SUBSCRIBERS)
    public @ResponseBody List<Subscriber> getAllSubscribers() {
        SubscriptionService ss = SubscriptionService.getInstance();
        HashSet<Subscriber> allSubscribers = ss.getAllSubscribers(); // TODO: Sort this lexicographically on topic
        List<Subscriber> listToSort = new ArrayList<>(allSubscribers).stream()
                .sorted((s1, s2) -> { // Null-safe compare operator
                    if (s1.getTopic() == null ^ s2.getTopic() == null) { return (s1.getTopic() == null) ? -1 : 1; }

                    if (s1.getTopic() == null && s2.getTopic() == null) { return 0; }

                    return s1.getTopic().compareTo(s2.getTopic());
                })
                .collect(Collectors.toList());
        return listToSort;
    }

    /**
     * This method deletes a single subscriber given the subscriberID request parameter.
     * @param subscriberID The subscriber to be deleted, represented as a String id
     * @return A JSON serialization of the deleted subscriber
     */
    @RequestMapping(method = RequestMethod.DELETE, value= DELETE_SINGLE_SUBSCRIBER)
    public @ResponseBody Subscriber deleteSingleSubscriber(@RequestParam(value = "subscriberID") String subscriberID) {
        log.debug("Deleting subscriber with ID: " + subscriberID);
        SubscriptionService ss = SubscriptionService.getInstance();
        Subscriber s = ss.getSubscriberByID(subscriberID.trim());
        ss.removeSubscriber(s);
        return s;
    }

    /**
     * Thid method deletes all the subscribers registered in the SubscriptionService.
     * @return A JSON serialized string
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_ALL_SUBSCRIBERS)
    public @ResponseBody String deleteAllSubscribers() {
        SubscriptionService ss = SubscriptionService.getInstance();
        HashSet<Subscriber> allSubscribers = ss.getAllSubscribers();
        allSubscribers.stream().forEach(s -> ss.removeSubscriber(s));
        return "{ \"deleted\" :true }";
    }

}
