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

    private static Logger log = Logger.getLogger(ConfigController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    public void config() {
        // should return
        log.info("Config called");
    }

    @RequestMapping(method = RequestMethod.POST, value="/mapping/exsisting/add/{topic}/{newTopic}")
    public void addExsistingMapping(@PathVariable String topic, @PathVariable String newTopic) {
        log.info("Trying to add exsisting mapping between  " + topic + " --> " + newTopic);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/mapping/predefined/{topic}/{newTopic}")
    public void addPredefinedMapping(@PathVariable String topic, @PathVariable String newTopic) {
        log.info("Trying to add predefined mapping between " + topic + " --> " + newTopic);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/encoding")
    public void setEncoding() {
        log.info("Changing chuncked transfer encoding");
    }

}
