package de.comlineag.snc.helper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.comlineag.snc.constants.SocialNetworks;

public class DateTimeServices {
	// we use simple org.apache.log4j.Logger for lgging
	static Logger logger = Logger.getLogger("de.comlineag.snc.helper.DateTimeServices");
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// the class is not to be instantiated
	private DateTimeServices() {}

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
	
			if (_snId.equalsIgnoreCase(SocialNetworks.getSocialNetworkConfigElement("code", "TWITTER"))) {
				DateTimeServices.logger.debug("formatting date time for use with twitter");
				snPattern = "EEE MMM d H:m:s Z yyyy";
				// the date time format for twitter must be set to US, otherwise english designators will not be translated correctly
				snLocale = Locale.US;
			} else if (_snId.equalsIgnoreCase(SocialNetworks.getSocialNetworkConfigElement("code", "LITHIUM"))) {
				DateTimeServices.logger.debug("formatting date time for use with Lithium");
				// 2014-01-08T12:21:42+00:00
				// date time pattern provided by Thomas Nowak
				snPattern = "yyyy-MM-dd'T'HH:mm:ssZZ";
				snLocale = Locale.GERMANY;
			} else if (_snId.equalsIgnoreCase(SocialNetworks.getSocialNetworkConfigElement("code", "WEBCRAWLER"))) {
				DateTimeServices.logger.debug("formatting date time for use with the simple webcrawler");
				// 2014-01-08T12:21:42+00:00
				// date time pattern provided by Thomas Nowak
				snPattern = "yyyy-MM-dd'T'HH:mm:ssZZ";
				snLocale = Locale.US;
			} else {
				DateTimeServices.logger.warn("no specific conversion for system " + _snId);
			}
			
			// convert the datum
			DateTimeFormatter formatter =
					DateTimeFormat.forPattern(snPattern).withLocale(snLocale);
			
			DateTime dateTime = formatter.parseDateTime(_timestamp);
			return dateTime.toLocalDateTime();
		} catch (Exception e) {
			DateTimeServices.logger.error(e.getMessage());
			// default: current Timestamp in case any error occurs
			DateTime dt = new DateTime();
			return new LocalDateTime(dt);
		}
	}

}