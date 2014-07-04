package de.comlineag.snc.data;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author		Magnus Leinemann, Christian Guenther, Thomas Nowak
 * @category 	Helper Class
 * @version 	1.2
 * 
 * @description Tools for managing special Requests in the Crawler logic
 * 
 * @changelog	1.0 class created
 * 				1.1 try and error for lithium post date time
 * 				1.2 bugfixing and productive version with support for lithium
 * 
 */

public class DataHelper {

	private static Logger logger = Logger.getLogger("de.comlineag.snc.data.DataHelper");

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
				// the date time format for twitter must be set to US, otherwise english designators will not be translated correctly
				snLocale = Locale.US;
			} else if (_snId.equalsIgnoreCase(SocialNetworks.LITHIUM.getValue())) {
				logger.trace("formatting date time for use with Lithium");
				// 2014-01-08T12:21:42+00:00
				// date time pattern provided by Thomas Nowak
				snPattern = "yyyy-MM-dd'T'HH:mm:ssZZ";
				snLocale = Locale.GERMANY;
			} else {
				logger.warn("no specific conversion for system " + _snId);
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
