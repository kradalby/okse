package no.ntnu.okse.web.controller;

import no.ntnu.okse.core.topic.TopicService;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

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
    private static final String ADD_MAPPING = "/mapping/add/{topic}/{newTopic}";
    private static final String DELETE_MAPPING = "/mapping/delete/{topicToRemove}";

    private static Logger log = Logger.getLogger(ConfigController.class.getName());

    @RequestMapping(method = RequestMethod.GET, value = GET_ALL_MAPPINGS)
    public @ResponseBody HashMap<String, HashSet<String>> getAllMappings() {
        TopicService ts = TopicService.getInstance();
        return ts.getAllMappings();
    }

    @RequestMapping(method = RequestMethod.POST, value= ADD_MAPPING)
    public @ResponseBody String addMapping(@PathVariable("topic") String topic, @PathVariable("newTopic") String newTopic) {
        log.debug("Adding a mapping between topic: " + topic + " and topic: " + newTopic);
        return "{ \"added\" :true }"; // TODO: Add some other shit here!
    }

    @RequestMapping(method = RequestMethod.DELETE, value = DELETE_MAPPING)
    public @ResponseBody String deleteMapping(@PathVariable("topicToRemove") String topicToRemove) {
        log.debug("Trying to remove the mapping for topic: " + topicToRemove);
        TopicService ts = TopicService.getInstance();
        ts.deleteMapping(topicToRemove);
        return "{ \"deleted\" :true }"; // TODO: Add check here or somewhere inside.
    }

}
