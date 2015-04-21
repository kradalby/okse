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
import org.springframework.web.bind.annotation.*;

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

    private static final String LOG_LEVELS = "/levels";
    private static final String LOG_FILES = "/files";

    private static final HashMap<String, ArrayList<String>> logLevels = new HashMap<String, ArrayList<String>>(){{
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

    private static HashMap<Integer, String> fileNames;

    private static Integer fileID = 0;

    public LogController() {
        fileNames = new HashMap<>();
        updateAvailableLogFiles();
    }


    public static boolean isWithinLogLevel(String logLevel, String line) {
        for (String level : logLevels.get(logLevel)) {
            if (line.contains(level)) return true;
        }
        return false;

    }

    private static void updateAvailableLogFiles(){
        File dir = new File("logs");
        Collection<File> files = FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        for (File file: files) {
            if (!fileNames.containsValue(file.getName())) {
                fileNames.put(fileID++, file.getName());
            }
        }
    }



    @RequestMapping(method = RequestMethod.GET)
    public Log log(@RequestParam(value="logID", defaultValue="0") Integer logID,
                   @RequestParam(value="logLevel", defaultValue="DEBUG") String logLevel,
                   @RequestParam(value = "length", defaultValue = "250") int length) {

        try {
            Stream<String> l = Files.lines(Paths.get("logs/", fileNames.get(logID)));

            List<String> lines = l.filter(x -> isWithinLogLevel(logLevel, x)).collect(Collectors.toList());
            Collections.reverse(lines);

            Log log;

            if (lines.size() > length) {
                log = new Log(fileNames.get(logID), lines.subList(0,length));
            } else {
                log = new Log(fileNames.get(logID), lines);
            }

            return log;

        } catch (IOException e) {
            Log log = new Log("Does not exists", new ArrayList<String>() {{
                add("The logfile you requested do not exist.");
            }} );
            return log;
        }
    }

    @RequestMapping(value = LOG_FILES, method = RequestMethod.GET)
    public @ResponseBody HashMap<Integer,String> logFilesAvailable() {
        updateAvailableLogFiles();
        return fileNames;
    }

    @RequestMapping(value = LOG_LEVELS, method = RequestMethod.GET)
    public @ResponseBody Set<String> logLevelsAvailable() {
        return logLevels.keySet();
    }

}

