package no.ntnu.okse.core;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 29/04/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class Utilities {

    public static Logger log = Logger.getLogger(Utilities.class.getName());

    /**
     * Returns a ISO 8601 HH:mm:ss.SSS formated string of a Duration object
     *
     * @param duration A Duration object
     * @return The formated string in HH:mm:ss.SSS format
     */
    public static String getDurationAsISO8601(Duration duration) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        long millis = duration.toMillis();
        return format.format(new Date(millis - TimeZone.getDefault().getRawOffset()));
    }

    /**
     * Retrieve a set of properties from a .properties configuration file
     *
     * @param path The relative path to the configuration file (including filenam)
     * @return A Properties object containing the data from the configuration file, null if errors during read.
     */
    public static Properties readConfigurationFromFile(String path) {
        try {
            File configFile = new File(path);
            FileInputStream fis = new FileInputStream(configFile);
            Properties properties = new Properties();
            properties.load(fis);
            fis.close();

            return properties;
        } catch (FileNotFoundException e) {
            log.error("Configuration file did not exist: " + path);
        } catch (IOException e) {
            log.error("I/O error while attempting to read from configuration file: " + e.getMessage());
        }

        return null;
    }

    /**
     * Helper method that creates a config file directory and checks for presence of the default properties files
     * If directory does not exist, it creates it, and then proceeds to copy the default properties files
     * from the classpath resources folder.
     */
    public static void createConfigDirectoryAndFilesIfNotExists() {
        // If the config directory does not exist, create it
        File configDir = new File("config");
        if (!configDir.exists()) configDir.mkdirs();

        // Declare the config files
        File okseConfig = new File("config/okse.properties");
        File log4jConfig = new File("config/log4j.properties");
        File topicConfig = new File("config/topicmapping.properties");

        try {
            // If okse.properties does not exist, copy it from the classpath resources folder
            if (!okseConfig.exists()) {
                InputStream baseOkseConfig = Utilities.class.getResourceAsStream("/config/okse.properties");
                Files.copy(baseOkseConfig, okseConfig.toPath());
            }
            // If log4j.properties does not exist, copy it from the classpath resources folder
            if (!log4jConfig.exists()) {
                InputStream baseLog4jConfig = Utilities.class.getResourceAsStream("/config/log4j.properties");
                Files.copy(baseLog4jConfig, log4jConfig.toPath());
            }
            // If topicmapping.properties does not exist, copy it from the classpath resources folder
            if (!topicConfig.exists()) {
                InputStream baseTopicConfig = Utilities.class.getResourceAsStream("/config/topicmapping.properties");
                Files.copy(baseTopicConfig, topicConfig.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
