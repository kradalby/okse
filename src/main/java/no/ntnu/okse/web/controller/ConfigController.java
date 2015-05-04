package no.ntnu.okse.web.controller;

import no.ntnu.okse.core.topic.TopicService;
import org.apache.log4j.Logger;
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
    private static final String DELETE_ALL_MAPPINGS = "mapping/delete";

    private static Logger log = Logger.getLogger(ConfigController.class.getName());

    @RequestMapping(method = RequestMethod.GET, value = GET_ALL_MAPPINGS)
    public @ResponseBody HashMap<String, HashSet<String>> getAllMappings() {
        TopicService ts = TopicService.getInstance();
        return ts.getAllMappings();
    }

    @RequestMapping(method = RequestMethod.POST, value= ADD_MAPPING)
    public @ResponseBody ResponseEntity<String> addMapping(@RequestParam(value = "fromTopic") String topic, @RequestParam(value = "toTopic") String newTopic) {
        log.debug("Adding a mapping between Topic{" + topic + "} and Topic{" + newTopic + "}");
        TopicService ts = TopicService.getInstance();
        ts.addMappingBetweenTopics(topic, newTopic);
        // TODO: We probably need to add some check somewhere, that checks if the input string is correct.
        return new ResponseEntity<String>("{ \"added\" :true }", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_MAPPING)
    public @ResponseBody ResponseEntity<String> deleteMapping(@RequestParam(value = "topic") String topicToRemove) {
        log.debug("Trying to remove the mapping for Topic{" + topicToRemove + "}");
        TopicService ts = TopicService.getInstance();
        ts.deleteMapping(topicToRemove);

        return new ResponseEntity<String>("{ \"deleted\" :true }", HttpStatus.OK);
    }

}
