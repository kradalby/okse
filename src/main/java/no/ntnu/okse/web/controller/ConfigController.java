package no.ntnu.okse.web.controller;

import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.protocol.amqp.AMQPServer;
import no.ntnu.okse.protocol.amqp.AMQProtocolServer;
import no.ntnu.okse.protocol.wsn.WSNTools;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 17/03/15.
 *
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final String GET_ALL_MAPPINGS = "/mapping/get/all";
    private static final String ADD_MAPPING = "/mapping/add";
    private static final String DELETE_MAPPING = "/mapping/delete/single";
    private static final String DELETE_ALL_MAPPINGS = "/mapping/delete/all";
    private static final String CHANGE_AMQP = "/mapping/queue/change";

    private static Logger log = Logger.getLogger(ConfigController.class.getName());

    /**
     * This method returns all mappings that exists in the TopicService
     * @return A JSON serialized response body of the HashMap containing all mappings
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_ALL_MAPPINGS)
    public @ResponseBody HashMap<String, HashSet<String>> getAllMappings() {
        TopicService ts = TopicService.getInstance();
        return ts.getAllMappings();
    }

    /**
     * This method adds a mapping between two topics in the TopicService
     * @param topic The topic to map from
     * @param newTopic The topic to map to
     * @return A JSON serialized response body
     */
    @RequestMapping(method = RequestMethod.POST, value= ADD_MAPPING)
    public @ResponseBody ResponseEntity<String> addMapping(@RequestParam(value = "fromTopic") String topic, @RequestParam(value = "toTopic") String newTopic) {
        log.debug("Adding a mapping between Topic{" + topic + "} and Topic{" + newTopic + "}");
        TopicService ts = TopicService.getInstance();
        ts.addMappingBetweenTopics(topic, newTopic);
        // TODO: We probably need to add some check somewhere, that checks if the input string is correct.

        return new ResponseEntity<String>("{ \"added\" :true }", HttpStatus.OK);
    }

    /**
     * This method deletes all mappings for a given topic in the TopicService
     * @param topicToRemove The topic to remove
     * @return A JSON serialized response body
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_MAPPING)
    public @ResponseBody ResponseEntity<String> deleteMapping(@RequestParam(value = "topic") String topicToRemove) {
        log.debug("Trying to remove the mapping for Topic{" + topicToRemove + "}");
        TopicService ts = TopicService.getInstance();
        ts.deleteMapping(topicToRemove);

        return new ResponseEntity<String>("{ \"deleted\" :true }", HttpStatus.OK);
    }

    /**
     * This method deletes all mappings in the TopicService
     * @return A JSON serialized response body
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_ALL_MAPPINGS)
    public @ResponseBody ResponseEntity<String> deleteAllMapping() {
        log.debug("Trying to delete all mappings");
        TopicService ts = TopicService.getInstance();
        ts.getAllMappings().forEach((k, v) -> {
            ts.deleteMapping(k);
        });
        return new ResponseEntity<String>("{ \"deleted\" :true }", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = CHANGE_AMQP)
    public @ResponseBody ResponseEntity<String> changeAMQPqueue() {
        AMQProtocolServer.getInstance().useQueue = (AMQProtocolServer.getInstance().useQueue ? false : true);
        log.debug("Value of AMQP queue is now " + AMQProtocolServer.getInstance().useQueue);
        return new ResponseEntity<String>("{ \"value\" :" + AMQProtocolServer.getInstance().useQueue + " }", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/relay/add")
    public @ResponseBody ResponseEntity<String> addRelay(@RequestParam(value = "to") String relay) {
        log.debug("Add relay to: " + relay);
        WsnUtilities.sendSubscriptionRequest(relay, WSNotificationServer.getInstance().getURI(), WSNotificationServer.getInstance().getRequestParser());
        return new ResponseEntity<String>("{ \"added\" :true }", HttpStatus.OK);
    }


}
