package no.ntnu.okse.web.controller;

import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.protocol.amqp.AMQPServer;
import no.ntnu.okse.protocol.amqp.AMQProtocolServer;
import no.ntnu.okse.protocol.wsn.WSNTools;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 17/03/15.
 *
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final String GET_ALL_INFO = "/get/all";
    private static final String GET_ALL_MAPPINGS = "/mapping/get/all";
    private static final String ADD_MAPPING = "/mapping/add";
    private static final String DELETE_MAPPING = "/mapping/delete/single";
    private static final String DELETE_ALL_MAPPINGS = "/mapping/delete/all";
    private static final String CHANGE_AMQP = "/mapping/queue/change";
    private static final String ADD_RELAY = "/relay/add";
    private static final String DELETE_ALL_RELAYS = "/relay/delete/all";
    private static final String DELETE_RELAY = "/relay/delete/single";

    private static Logger log = Logger.getLogger(ConfigController.class.getName());

    private ConcurrentHashMap<String, String> relays = new ConcurrentHashMap<>();

    @RequestMapping(method = RequestMethod.GET, value = GET_ALL_INFO)
    public @ResponseBody HashMap<String, Object> getAllInfo() {
        TopicService ts = TopicService.getInstance();
        HashMap<String, HashSet<String>> allMappings = ts.getAllMappings();

        HashMap<String, Object> result = new HashMap<String, Object>(){{
            put("mappings", allMappings);
            put("relays", relays);
        }};

        return result;
    }

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

    @RequestMapping(method = RequestMethod.POST, value = ADD_RELAY)
    public @ResponseBody ResponseEntity<String> addRelay(@RequestParam(value = "from") String relay) {
        log.debug("Adding relay from: " + relay);

        if (!relay.startsWith("http://")) { relay = "http://" + relay; }

        String subscriptionReference = WSNTools.extractSubscriptionReferenceFromRawXmlResponse(
                WsnUtilities.sendSubscriptionRequest(
                        relay,
                        WSNotificationServer.getInstance().getURI(),
                        WSNotificationServer.getInstance().getRequestParser()
                )
        );

        if (subscriptionReference == null) {
            log.debug("Relay could not be created");
            return new ResponseEntity<String>("{ \"added\" :false }", HttpStatus.OK);

        }
        relays.put(subscriptionReference.split("subscriberkey=")[1], subscriptionReference);
        log.debug("Relay got subscriptionReference: " + subscriptionReference);
        return new ResponseEntity<String>("{ \"added\" :true }", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_RELAY)
    public @ResponseBody ResponseEntity<String> deleteRelay(@RequestParam(value = "relayID") String relay) {
        log.debug("Trying to remove a relay: " + relay);

        if (relays.containsKey(relay)) {
            WsnUtilities.sendUnsubscribeRequest(relays.get(relay), WSNotificationServer.getInstance().getRequestParser());
            relays.remove(relay);
            log.debug("Removed relay: " + relay);
            return new ResponseEntity<String>("{ \"deleted\" :true }", HttpStatus.OK);
        } else {
            log.debug("Unable to remove relay: " + relay);
            return new ResponseEntity<String>("{ \"deleted\" :false }", HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_ALL_RELAYS)
    public @ResponseBody ResponseEntity<String> deleteAllRelays() {
        log.debug("Trying to delete all relays");
        relays.forEach((k, v) -> {
            WsnUtilities.sendUnsubscribeRequest(relays.get(k), WSNotificationServer.getInstance().getRequestParser());
            relays.remove(k);
            log.debug("Removed relay: " + k);
        });

        return new ResponseEntity<String>("{ \"deleted\" :true }", HttpStatus.OK);
    }
}
