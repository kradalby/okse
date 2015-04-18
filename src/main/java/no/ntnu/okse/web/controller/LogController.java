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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package no.ntnu.okse.web.controller;

import no.ntnu.okse.web.model.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by kradalby on 17/04/15.
 */

@RestController
@RequestMapping(value = "/api/log")
public class LogController {

    private static HashMap<String, ArrayList<String>> logLevels = new HashMap<String, ArrayList<String>>(){{
        put("DEBUG", new ArrayList<String>(){{
            add("DEBUG");
            add("INFO");
            add("WARN");
        }});
        put("INFO", new ArrayList<String>(){{
            add("INFO");
            add("WARN");
        }});
        put("WARN", new ArrayList<String>(){{
            add("WARN");
        }});
    }};

    public static boolean isWithinLogLevel(String logLevel, String line) {
        for (String level : logLevels.get(logLevel)) {
            if (line.contains(level)) return true;
        }
        return false;

    }


    @RequestMapping(method = RequestMethod.GET)
    public Log log(@RequestParam(value="logName", defaultValue="okse.log") String logName, @RequestParam(value="logLevel", defaultValue="DEBUG") String logLevel) {
        //ArrayList<Log> logs = new ArrayList<>();
        //File dir = new File("logs");
        //Collection<File> files = FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

//        for (File file : files) {
//            String name = file.getName();
//            List<String> lines = FileUtils.readLines(file);
//            Log log = new Log(name, lines);
//
//            logs.add(log);
//        }

        try {
            Stream<String> l = Files.lines(Paths.get("logs/", logName));

            List<String> lines = l.filter(x -> isWithinLogLevel(logLevel, x)).collect(Collectors.toList());
            Collections.reverse(lines);
            Log log = new Log(logName, lines);

            return log;

        } catch (IOException e) {
            Log log = new Log("Does not exists", new ArrayList<String>() {{
                add("The logfile you requested do not exist.");
            }} );
            return log;
        }
    }

}
