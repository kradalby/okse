package no.ntnu.okse.core;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
}
