package de.comlineag.sbm.data;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * 
 * @author		MLeinemann
 * @category 	Helper Class
 * 
 * @description Tools for managing special Requests in the Crawler logic
 * 
 * @version 	1.0
 * 
 */

public class DataHelper {

	private static Logger logger = Logger.getLogger("de.comlineag.sbm.data.DataHelper");

	/**
	 * 
	 * @description		create a timestamp for the OData Service Interface from the social media timestamp
	 * 					as each network is expected to act a little bit different, the snId is sent to decide 
	 * 					which algorithm is used. Implemented is the algorithm for Twitter and Lithium
	 * 					all other types return the current timestamp
	 * 
	 * @param 			_timestamp
	 *            			the timestamp in the social network
	 * @param 			_snId
	 *            			social network identifier
	 * @return			formatted timestamp
	 */
	public static LocalDateTime prepareLocalDateTime(String _timestamp, String _snId) {

		// Datumsformatierung fuer den Formatter
		String snPattern = "";
		Locale snLocale = Locale.getDefault();

		try {

			if (_snId.equalsIgnoreCase(SocialNetworks.TWITTER.getValue())) {
				logger.trace("formatting date time for use with twitter");
				snPattern = "EEE MMM d H:m:s Z yyyy";
				// Formatter unbedingt mit USA da sonst die englischen Bezeichner nicht aufgeloest werden, evtl. auch EN/UK
				snLocale = Locale.US;
			} else if (_snId.equalsIgnoreCase(SocialNetworks.LITHIUM.getValue())) {  
				logger.trace("formatting date time for use with Lithium");
				// 2014-01-08T12:21:42+00:00
				//snPattern = "EEE MMM d H:m:s Z yyyy";
				snPattern = "yyyy-mm-ddTH:m:s Z";
				snLocale = Locale.GERMANY;
			} else {
				logger.warn("no specific conversion for system " + _snId);
				snPattern = "EEE MMM d H:m:s Z yyyy";
				// Formatter unbedingt mit USA da sonst die englischen Bezeichner nicht aufgeloest werden, evtl. auch EN/UK
				snLocale = Locale.GERMANY;
			}

			// convert the datum
			DateTimeFormatter formatter =
					DateTimeFormat.forPattern(snPattern).withLocale(snLocale);

			DateTime dateTime = formatter.parseDateTime(_timestamp);
			return dateTime.toLocalDateTime();

		} catch (Exception e) {
			logger.error(e.getMessage());
			// default: current Timestamp in case any error occurs
			DateTime dt = new DateTime();
			return new LocalDateTime(dt);
		}
	}
}
