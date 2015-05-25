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

package no.ntnu.okse;

import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.db.DB;
import no.ntnu.okse.examples.DummyProtocolServer;
import no.ntnu.okse.protocol.amqp.AMQProtocolServer;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import no.ntnu.okse.web.Server;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ntnunotif.wsnu.base.util.Log;

import java.io.File;
import java.time.Duration;
import java.util.Properties;


/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class Application {

    // Version
    public static final String VERSION = "1.0.0";

    // Initialization time
    public static long startedAt = System.currentTimeMillis();

    /* Default global fields */
    public static String OKSE_SYSTEM_NAME = "OKSE System";
    public static boolean BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS = false;
    public static boolean CACHE_MESSAGES = true;
    public static long DEFAULT_SUBSCRIPTION_TERMINATION_TIME = 15552000000L; // Half a year
    public static long DEFAULT_PUBLISHER_TERMINATION_TIME = 15552000000L; // Half a year

    /* Public reference to the properties object for potential custom options */
    public static Properties config = new Properties();

    private static Logger log;
    public static CoreService cs;
    public static Server webserver;

    /**
     * Main method for the OKSE Message Broker
     * Used to initate the complete application (CoreService and WebServer)
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        /*
        // Check for presence of the needed config files, if they do not exist, they must be created
        Utilities.createConfigDirectoryAndFilesIfNotExists();
        // Configure the Log4j logger
        PropertyConfigurator.configure("config/log4j.properties");
        */
        // Reads config files, creates defaults if it does not exist, and updates fields
        readConfigurationFiles();

        // Init the logger
        log = Logger.getLogger(Application.class.getName());

        // Create a file handler for database file
        File dbFile = new File("okse.db");
        if (!dbFile.exists()) {
            DB.initDB();
            log.info("okse.db initiated");
        } else {
            log.info("okse.db exists");
        }

        // Initialize main system components
        webserver = new Server();
        cs = CoreService.getInstance();

        /* REGISTER CORE SERVICES HERE */
        cs.registerService(TopicService.getInstance());
        cs.registerService(MessageService.getInstance());
        cs.registerService(SubscriptionService.getInstance());

        /* REGISTER PROTOCOL SERVERS HERE */
        cs.addProtocolServer(WSNotificationServer.getInstance());
        //cs.addProtocolServer(DummyProtocolServer.getInstance());    // Example ProtocolServer
        cs.addProtocolServer(AMQProtocolServer.getInstance());

        // Start the admin console
        webserver.run();

        // Start the CoreService
        log.info("Starting OKSE " + VERSION);
        cs.boot();
    }

    /**
     * Returns a Duration instance of the time the Application has been running
     *
     * @return The amount of time the application has been running
     */
    public static Duration getRunningTime() {
        return Duration.ofMillis(System.currentTimeMillis() - startedAt);
    }

    /**
     * Resets the time at which the system was initialized. This method
     * can be used during a system restart from
     */
    public static void resetStartTime() {
        startedAt = System.currentTimeMillis();
    }

    /**
     * This public static method reads the OKSE configuration file from disk. If it does not exist, it is created.
     * Additionally, default fields in the Application class are populated after config has been read.
     *
     * @return The Properties object containing the config key/value pairs.
     */
    public static Properties readConfigurationFiles() {
        // Check for presence of the needed config files, if they do not exist, they must be created
        Utilities.createConfigDirectoryAndFilesIfNotExists();
        // Configure the Log4j logger
        PropertyConfigurator.configure("config/log4j.properties");
        // Read the contents of the configuration file
        config = Utilities.readConfigurationFromFile("config/okse.properties");
        // Initialize the internal fields from the defaults
        initializeDefaultsFromConfigFile(config);

        return config;
    }

    /**
     * Initializes/updates the default field values in this Application class from the configuration file
     *
     * @param properties A Properties object containing values from the configuration file.
     */
    private static void initializeDefaultsFromConfigFile(Properties properties) {
        if (properties == null) {
            log.error("Failed to update variables from config file, using internal defaults.");
            return;
        }
        // Iterate over all the keys
        for (String option : properties.stringPropertyNames()) {
            option = option.toUpperCase();
            // Update values based on what keys are provided
            switch (option) {
                case "CACHE_MESSAGES":
                    if (properties.getProperty(option).equalsIgnoreCase("true")) CACHE_MESSAGES = true;
                    else CACHE_MESSAGES = false;
                    break;
                case "BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS":
                    if (properties.getProperty(option).equalsIgnoreCase("true")) {
                        BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS = true;
                    } else BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS = false;
                    break;
                case "DEFAULT_SUBSCRIPTION_TERMINATION_TIME":
                    try {
                        DEFAULT_SUBSCRIPTION_TERMINATION_TIME = Long.parseLong(properties.getProperty(option));
                    } catch (NumberFormatException numEx) {
                        log.error("Malformed subscription termination time, using internal default");
                    }
                    break;
                case "DEFAULT_PUBLISHER_TERMINATION_TIME":
                    try {
                        DEFAULT_PUBLISHER_TERMINATION_TIME = Long.parseLong(properties.getProperty(option));
                    } catch (NumberFormatException numEx) {
                        log.error("Malformed subscription termination time, using internal default");
                    }
                    break;
                case "ENABLE_WSNU_DEBUG_OUTPUT":
                    if (properties.getProperty(option).equalsIgnoreCase("true")) Log.setEnableDebug(true);
                    else Log.setEnableDebug(false);
                    break;

            }
        }
    }
}