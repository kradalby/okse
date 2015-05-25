package no.ntnu.okse.web.controller;

import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.protocol.amqp.AMQProtocolServer;
import no.ntnu.okse.protocol.wsn.WSNTools;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 17/03/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    // URL routes
    private static final String GET_ALL_INFO = "/get/all";
    private static final String GET_ALL_MAPPINGS = "/mapping/get/all";
    private static final String ADD_MAPPING = "/mapping/add";
    private static final String DELETE_MAPPING = "/mapping/delete/single";
    private static final String DELETE_ALL_MAPPINGS = "/mapping/delete/all";
    private static final String CHANGE_AMQP = "/mapping/queue/change";
    private static final String ADD_RELAY = "/relay/add";
    private static final String DELETE_ALL_RELAYS = "/relay/delete/all";
    private static final String DELETE_RELAY = "/relay/delete/single";

    // LOG4J logger
    private static Logger log = Logger.getLogger(ConfigController.class.getName());

    // Relay fields
    private ConcurrentHashMap<String, String> relays;
    private HashSet<String> localRelays;

    /**
     * Constructor for ConfigController. Initiates the localRelays
     */
    public ConfigController() {
        relays = new ConcurrentHashMap<>();
        localRelays = new HashSet<String>() {{
            add("127.0.0.1");
            add("0.0.0.0");
            add("localhost");
            // Maybe WSNotificationServer.getInstance().getPublicWANHost() should be added here?
        }};
    }

    /**
     * This method returns all mappings and relays registered in OKSE
     *
     * @return A HashMap containing all mappings and relays
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_ALL_INFO)
    public
    @ResponseBody
    HashMap<String, Object> getAllInfo() {
        TopicService ts = TopicService.getInstance();
        HashMap<String, HashSet<String>> allMappings = ts.getAllMappings();

        HashMap<String, Object> result = new HashMap<String, Object>() {{
            put("mappings", allMappings);
            put("relays", relays);
        }};

        return result;
    }

    /**
     * This method returns all mappings that exists in the TopicService
     *
     * @return A JSON serialized response body of the HashMap containing all mappings
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_ALL_MAPPINGS)
    public
    @ResponseBody
    HashMap<String, HashSet<String>> getAllMappings() {
        TopicService ts = TopicService.getInstance();
        return ts.getAllMappings();
    }

    /**
     * This method adds a mapping between two topics in the TopicService
     *
     * @param topic    The topic to map from
     * @param newTopic The topic to map to
     * @return A JSON serialized response body
     */
    @RequestMapping(method = RequestMethod.POST, value = ADD_MAPPING)
    public
    @ResponseBody
    ResponseEntity<String> addMapping(@RequestParam(value = "fromTopic") String topic, @RequestParam(value = "toTopic") String newTopic) {
        log.debug("Adding a mapping between Topic{" + topic + "} and Topic{" + newTopic + "}");
        TopicService ts = TopicService.getInstance();
        ts.addMappingBetweenTopics(topic, newTopic);
        // TODO: We probably need to add some check somewhere, that checks if the input string is correct.

        return new ResponseEntity<String>("{ \"message\" :\"Added mapping from Topic{" + topic + "} to Topic{ " + newTopic + " }\" }", HttpStatus.OK);
    }

    /**
     * This method deletes all mappings for a given topic in the TopicService
     *
     * @param topicToRemove The topic to remove
     * @return A JSON serialized response body
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_MAPPING)
    public
    @ResponseBody
    ResponseEntity<String> deleteMapping(@RequestParam(value = "topic") String topicToRemove) {
        log.debug("Trying to remove the mapping for Topic{" + topicToRemove + "}");
        TopicService ts = TopicService.getInstance();
        ts.deleteMapping(topicToRemove);

        return new ResponseEntity<String>("{ \"message\" :\"Deleted mapping for Topic{" + topicToRemove + "}\" }", HttpStatus.OK);
    }

    /**
     * This method deletes all mappings in the TopicService
     *
     * @return A JSON serialized response body
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_ALL_MAPPINGS)
    public
    @ResponseBody
    ResponseEntity<String> deleteAllMapping() {
        log.debug("Trying to delete all mappings");
        TopicService ts = TopicService.getInstance();
        ts.getAllMappings().forEach((k, v) -> {
            ts.deleteMapping(k);
        });
        return new ResponseEntity<String>("{ \"message\" :\"Deleted all mappings\" }", HttpStatus.OK);
    }

    /**
     * This method changes the boolean value of the useQueue field in AMQPProtocolServer
     *
     * @return A message stating the new value of the useQueue variable
     */
    @RequestMapping(method = RequestMethod.POST, value = CHANGE_AMQP)
    public
    @ResponseBody
    ResponseEntity<String> changeAMQPqueue() {
        AMQProtocolServer.getInstance().useQueue = (AMQProtocolServer.getInstance().useQueue ? false : true);
        log.debug("Value of AMQP queue is now " + AMQProtocolServer.getInstance().useQueue);
        return new ResponseEntity<String>("{ \"value\": " + AMQProtocolServer.getInstance().useQueue + ", " +
                "\"message\" :\"Successfully changed the useQueue variable to " + AMQProtocolServer.getInstance().useQueue + "\"}", HttpStatus.OK);
    }

    /**
     * This method takes in a relay and a topic (not required) and sets up a relay
     *
     * @param relay String with host/port to relay from
     * @param topic String with topic to relay (not required)
     * @return A message telling the outcome of the subscription request.
     */
    @RequestMapping(method = RequestMethod.POST, value = ADD_RELAY)
    public
    @ResponseBody
    ResponseEntity<String> addRelay(@RequestParam(value = "from") String relay, @RequestParam(value = "topic", required = false) String topic) {
        log.debug("Trying to add relay from: " + relay + " with topic:" + topic);

        String regex = "(?:http.*://)?(?<host>[^:/ ]+).?(?<port>[0-9]*).*";
        Matcher m = Pattern.compile(regex).matcher(relay);
        String host = null;
        Integer port = null;

        if (m.matches()) {
            host = m.group("host");
            port = Integer.valueOf(m.group("port"));
        }

        if (host == null || port == null) {
            log.debug("Host or port not provided, not able to add relay");
            return new ResponseEntity<String>("{ \"message\" :\"Host or port not provided, not able to add relay\" }", HttpStatus.BAD_REQUEST);
        }

        // if relay.host == 0.0.0 etc sjekk port
        if (localRelays.contains(host)) {
            log.debug("Same host, need to check port");
            if (WSNotificationServer.getInstance().getPort() == port) {
                log.debug("Same port, invalid relay command");
                return new ResponseEntity<String>("{ \"message\" :\"Same host and port, not able to add relay\" }", HttpStatus.BAD_REQUEST);
            }
            // else sjekk relay.host mot publicWANHost, så sjekk port
        } else if (host.equals(WSNotificationServer.getInstance().getPublicWANHost())) {
            log.info("Same host (WAN), need to check port");
            if (WSNotificationServer.getInstance().getPublicWANPort() == port) {
                log.info("Same port (WAN), invalid relay command");
                return new ResponseEntity<String>("{ \"message\" :\"Same host and port (WAN), not able to add relay\" }", HttpStatus.BAD_REQUEST);
            }
        }

        if (!relay.startsWith("http://")) {
            relay = "http://" + relay;
        }

        String subscriptionReference = WSNTools.extractSubscriptionReferenceFromRawXmlResponse(
                WSNotificationServer.getInstance().getRequestParser().acceptLocalMessage(
                        WSNTools.generateSubscriptionRequestWithTopic(
                                relay,
                                topic, // Topic
                                WSNotificationServer.getInstance().getURI(),
                                null) // Term time
                )
        );

        if (subscriptionReference == null) {
            log.debug("Relay could not be created");
            return new ResponseEntity<String>("{ \"message\" :\"The subscription request failed for relay failed\" }", HttpStatus.BAD_REQUEST);

        }
        relays.put(subscriptionReference.split("subscriberkey=")[1], subscriptionReference);
        log.debug("Relay got subscriptionReference: " + subscriptionReference);
        return new ResponseEntity<String>("{ \"message\" :\"Successfully added relay\" }", HttpStatus.OK);
    }

    /**
     * This method deletes a relay if it exists
     *
     * @param relay The relay to delete
     * @return A message telling if the removal were successful.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_RELAY)
    public
    @ResponseBody
    ResponseEntity<String> deleteRelay(@RequestParam(value = "relayID") String relay) {
        log.debug("Trying to remove a relay: " + relay);

        if (relays.containsKey(relay)) {
            WsnUtilities.sendUnsubscribeRequest(relays.get(relay), WSNotificationServer.getInstance().getRequestParser());
            relays.remove(relay);
            log.debug("Removed relay: " + relay);
            return new ResponseEntity<String>("{ \"message\" :\"Successfully removed the relay\" }", HttpStatus.OK);
        } else {
            log.debug("Unable to remove relay: " + relay);
            return new ResponseEntity<String>("{ \"message\" :\"Unable to remove the relay, can't find it.\" }", HttpStatus.OK);
        }
    }

    /**
     * This method deletes all relays registered.
     *
     * @return A response message
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_ALL_RELAYS)
    public
    @ResponseBody
    ResponseEntity<String> deleteAllRelays() {
        log.debug("Trying to delete all relays");
        relays.forEach((k, v) -> {
            WsnUtilities.sendUnsubscribeRequest(relays.get(k), WSNotificationServer.getInstance().getRequestParser());
            relays.remove(k);
            log.debug("Removed relay: " + k);
        });

        return new ResponseEntity<String>("{ \"message\" :\"Deleted all relays\" }", HttpStatus.OK);
    }
}
