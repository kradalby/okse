package no.ntnu.okse.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 17/03/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @RequestMapping(method = RequestMethod.GET)
    public void config() {
        // should return configs
    }

}
