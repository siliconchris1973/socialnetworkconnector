package de.comlineag.sbm.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description TwitterParser implementiert den Parser zur Dekodierung der
 *              Twitter postings
 * 
 */
public final class TwitterParser extends GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());

	public TwitterParser() {}

	@Override
	protected void parse(String strTweet) {
		// log the startup message
		logger.debug("parser START");

		// macht ein JSon Decode aus dem uebergebenen String
		JSONParser parser = new JSONParser();
		List<TwitterPosting> postings = new ArrayList<TwitterPosting>();
		List<TwitterUser> users = new ArrayList<TwitterUser>();

		try {
			// zuerst suchen wir uns den post (tweet)
			JSONObject jsonTweetResource = (JSONObject) parser.parse(strTweet);
			TwitterPosting posting = new TwitterPosting(jsonTweetResource);
			postings.add(posting);

			// und dann den user
			JSONObject jsonUser = (JSONObject) jsonTweetResource.get("user");
			TwitterUser user = new TwitterUser(jsonUser);
			users.add(user);
			
			// zum schluss noch etwaige retweeted messages
			//TODO check if retweeted REALLY is added
			JSONObject jsonReTweeted = (JSONObject) jsonTweetResource.get("retweeted_status");
			if (jsonReTweeted != null) {
				postings.add(new TwitterPosting(jsonReTweeted));
			}

		} catch (ParseException e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		for (int ii = 0; ii < postings.size(); ii++) {
			TwitterPosting post = (TwitterPosting) postings.get(ii);
			post.save();
		}
		
		for (int ii = 0; ii < users.size(); ii++) {
			TwitterUser user = (TwitterUser) users.get(ii);
			user.save();
		}
		logger.debug("Parser END");
	}
}
