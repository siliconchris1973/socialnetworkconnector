package de.comlineag.sbm.data;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * 
 * @author MLeinemann
 * 
 * @category Helper Class
 * 
 * @description Tools for managing special Requests in the Crawler logic
 * 
 * @version 1.0
 * 
 */

public class DataHelper {

	private static Logger logger = Logger.getLogger("de.comlineag.sbm.data.DataHelper");

	/**
	 * create a timestamp for the OData Service Interface from the social media timestamp
	 * 
	 * as each network is expected to act a little bit different, the snId is sent to decide which algorith is used.
	 * Implemented is the algorithm for Twitter
	 * 
	 * all other types return the current timestamp
	 * 
	 * @param _timestamp
	 *            the timestamp in the social network
	 * @param _snId
	 *            social network identifier
	 * @return
	 */
	public static LocalDateTime prepareLocalDateTime(String _timestamp, String _snId) {

		// Datumsformatierung für den Formatter
		String snPattern = "";
		Locale snLocale = Locale.getDefault();

		try {

			if (_snId.equalsIgnoreCase(SocialNetworks.TWITTER.getValue())) {
				snPattern = "EEE MMM d H:m:s Z yyyy";
				// Formatter unbedingt mit USA da sonst die englischen Bezeichner nicht aufgelöst werden, evtl. auch EN/UK
				snLocale = Locale.US;
			} else {
				logger.warn("unsupported System " + _snId);
			}

			// Übersetzen der Daten:
			DateTimeFormatter formatter =
					DateTimeFormat.forPattern(snPattern).withLocale(snLocale);

			DateTime dateTime = formatter.parseDateTime(_timestamp);
			return dateTime.toLocalDateTime();

		} catch (Exception e) {
			logger.error(e.getMessage());
			// default: current Timestamp falls noch Fehler auftreten
			DateTime dt = new DateTime();
			return new LocalDateTime(dt);
		}

	}
}
