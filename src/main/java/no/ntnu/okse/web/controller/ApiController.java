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

import no.ntnu.okse.web.model.Subscriber;
import no.ntnu.okse.web.model.Topic;
import org.springframework.web.bind.annotation.*;

import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@RestController
public class ApiController {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping(value = "/api/topics", method = RequestMethod.GET)
    public Topic topics(@RequestParam(value="name", defaultValue="World") String name) {

        return new Topic(1233L, "myTopic", new ArrayList<Subscriber>(Arrays.asList(
                new Subscriber("128.0.0.1", "8080", "WS", new HashMap<String, String>()),
                new Subscriber("78.91.14.24", "234", "stomp", new HashMap<String, String>()),
                new Subscriber("localhost", "235", "amqp", new HashMap<String, String>())
        )));
    }

    @RequestMapping(value = "/api/main", method = RequestMethod.GET)
    public List<Topic> main() {
        List<Topic> allTheShit = new ArrayList<>();
        return allTheShit;
    }

    @RequestMapping(value = "/api/stats", method = RequestMethod.GET)
    public List<Topic> stats() {
        List<Topic> allTheShit = new ArrayList<>();
        return allTheShit;
    }

    @RequestMapping(value = "/api/config", method = RequestMethod.GET)
    public List<Topic> config(){
        List<Topic> allTheShit = new ArrayList<>();
        return allTheShit;
    }
}