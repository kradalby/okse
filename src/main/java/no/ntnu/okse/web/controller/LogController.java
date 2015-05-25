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

import com.google.common.collect.Lists;
import no.ntnu.okse.web.model.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
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

    // URL routes
    private static final String LOG_LEVELS = "/levels";
    private static final String LOG_FILES = "/files";

    // The available log leves
    private static final LinkedHashMap<String, ArrayList<String>> logLevels = new LinkedHashMap<String, ArrayList<String>>() {{
        put("DEBUG", new ArrayList<String>() {{
            add("DEBUG");
            add("INFO");
            add("WARN");
            add("ERROR");
        }});
        put("INFO", new ArrayList<String>() {{
            add("INFO");
            add("WARN");
            add("ERROR");
        }});
        put("WARN", new ArrayList<String>() {{
            add("WARN");
            add("ERROR");
        }});
        put("ERROR", new ArrayList<String>() {{
            add("ERROR");
        }});
    }};

    // The available files
    private static HashMap<Integer, String> fileNames;
    private static Integer fileID = 1;

    // Log4j logger
    private static Logger log = Logger.getLogger(LogController.class.getName());


    /**
     * Constructor for the log controller
     */
    public LogController() {
        fileNames = new HashMap<>();
        updateAvailableLogFiles();
    }

    /**
     * This method checks if a line is within a given level String
     *
     * @param logLevel The level to check
     * @param line     The line to check
     * @return A boolean value telling if the line matches
     */
    public static boolean isWithinLogLevel(String logLevel, String line) {
        for (String level : logLevels.get(logLevel)) {
            if (line.contains(level)) return true;
        }
        return false;

    }

    /**
     * Private helper method that updates information regarding which log files that are available
     */
    private static void updateAvailableLogFiles() {
        File dir = new File("logs");
        Collection<File> files = FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        files.forEach(f -> {
            if (!fileNames.containsValue(f.getName())) {
                if (f.getName().equalsIgnoreCase("okse.log")) {
                    fileNames.put(0, f.getName());
                } else {
                    fileNames.put(fileID++, f.getName());
                }
            }
        });

    }

    /**
     * This method returns the log given the request parameters
     *
     * @param logID    The log file to return (represented as a integer)
     * @param logLevel The log level to show
     * @param length   How many lines to return
     * @return A Log model containing the correct log. Will be serialized as JSON
     */
    @RequestMapping(method = RequestMethod.GET)
    public Log log(@RequestParam(value = "logID", defaultValue = "0") Integer logID,
                   @RequestParam(value = "logLevel", defaultValue = "DEBUG") String logLevel,
                   @RequestParam(value = "length", defaultValue = "250") int length) {

        try {
            Stream<String> l = Files.lines(Paths.get("logs/", fileNames.get(logID)));

            List<String> lines = l.filter(x -> isWithinLogLevel(logLevel, x)).collect(Collectors.toList());
            Collections.reverse(lines);

            Log log;

            if (lines.size() > length) {
                log = new Log(fileNames.get(logID), lines.subList(0, length));
            } else {
                log = new Log(fileNames.get(logID), lines);
            }

            return log;

        } catch (IOException e) {
            Log log = new Log("Does not exists", new ArrayList<String>() {{
                add("The logfile you requested do not exist.");
            }});
            return log;
        }
    }

    /**
     * Returns the available log files
     *
     * @return A HashMap of the available log files
     */
    @RequestMapping(value = LOG_FILES, method = RequestMethod.GET)
    public
    @ResponseBody
    HashMap<Integer, String> logFilesAvailable() {
        updateAvailableLogFiles();
        return fileNames;
    }

    /**
     * Returns the available log levels
     *
     * @return A List of the available log levels
     */
    @RequestMapping(value = LOG_LEVELS, method = RequestMethod.GET)
    public
    @ResponseBody
    List<Object> logLevelsAvailable() {
        List<Object> reverseList = Lists.reverse(
                Lists.newArrayList(logLevels.keySet()));
        return reverseList;
    }

}

