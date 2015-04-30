package no.ntnu.okse.core;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 29/04/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class Utilities {

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

}
