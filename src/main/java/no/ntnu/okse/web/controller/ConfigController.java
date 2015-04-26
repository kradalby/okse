package no.ntnu.okse.web.controller;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 17/03/15.
 *
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final String ADD_EXISISTING_MAPPING = "/mapping/exsisting/add/{topic}/{newTopic}";
    private static final String ADD_PREDEFINED_MAPPING = "/mapping/predefined/{topic}/{newTopic}";

    private static Logger log = Logger.getLogger(ConfigController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    public String config() {
        return "Temporary place holder";
    }

    @RequestMapping(method = RequestMethod.POST, value=ADD_EXISISTING_MAPPING)
    public void addExsistingMapping(@PathVariable("topic") String topic, @PathVariable("newTopic") String newTopic) {
        log.debug("Trying to add existing mapping between  " + topic + " --> " + newTopic);
    }

    @RequestMapping(method = RequestMethod.POST, value = ADD_PREDEFINED_MAPPING)
    public void addPredefinedMapping(@PathVariable("topic") String topic, @PathVariable("newTopic") String newTopic) {
        log.debug("Trying to add predefined mapping between " + topic + " --> " + newTopic);
    }
}
