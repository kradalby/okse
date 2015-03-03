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

import no.ntnu.okse.web.model.Topics;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.lang.String;
import java.util.ArrayList;
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
    public Topics topics(@RequestParam(value="name", defaultValue="World") String name) {
        return new Topics(counter.incrementAndGet(),
                String.format(template, name));
    }

    @RequestMapping(value = "/api/main", method = RequestMethod.GET)
    public List<Topics> main() {
        List<Topics> allTheShit = new ArrayList<>();
        allTheShit.add(new Topics(235, "test topics"));
        allTheShit.add(new Topics(299, "test topics #2"));
        return allTheShit;
    }

    @RequestMapping(value = "/api/stats", method = RequestMethod.GET)
    public List<Topics> stats() {
        List<Topics> allTheShit = new ArrayList<>();
        allTheShit.add(new Topics(235, "test topics"));
        allTheShit.add(new Topics(299, "test topics #2"));
        return allTheShit;
    }

    @RequestMapping(value = "/api/config", method = RequestMethod.GET)
    public List<Topics> config(){
        List<Topics> allTheShit = new ArrayList<>();
        allTheShit.add(new Topics(235, "test topics"));
        allTheShit.add(new Topics(299, "test topics #2"));
        return allTheShit;
    }
}