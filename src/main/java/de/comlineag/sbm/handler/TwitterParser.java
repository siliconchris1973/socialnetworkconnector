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

	public TwitterParser() {
	}

	@Override
	protected void parse(String strTweet) {
		// log the startup message
		logger.debug("method parse from class " + getClass().getName() + " called");

		// macht ein JSon Decode aus dem uebergebenen String
		// TODO: How to decode the map and seperate Tweets from Users and
		// Retweets and URLs and so on
		JSONParser parser = new JSONParser();
		List<TwitterPosting> postings = new ArrayList<TwitterPosting>();
		List<TwitterUser> users = new ArrayList<TwitterUser>();

		try {
			// zuerst suchen wir uns den posts (tweets)
			JSONObject jsonTweetResource = (JSONObject) parser.parse(strTweet);
			TwitterPosting posting = new TwitterPosting(jsonTweetResource);
			postings.add(posting);

			// und dann den user
			JSONObject jsonUser = (JSONObject) jsonTweetResource.get("user");
			TwitterUser user = new TwitterUser(jsonUser);
			users.add(user);

			JSONObject jsonReTweeted = (JSONObject) jsonTweetResource.get("retweeted_status");
			if (jsonReTweeted != null) {
				// warum machen wir hier nix?
				// weil noch nicht fertig
				// postings.add(new TwitterPostingData(jsonReTweeted));
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.trace(e.getMessage(), e);
		}

		for (int ii = 0; ii < postings.size(); ii++) {
			// TwitterPosting post = (TwitterPosting) postings.get(ii);
			TwitterPosting post = (TwitterPosting) postings.get(ii);
			post.save(); // hier Key fuer Tweets uebergeben

		}

		for (int ii = 0; ii < users.size(); ii++) {
			TwitterUser user = (TwitterUser) users.get(ii);
			user.save(); // hier key fuer User uebergeben

		}
	}
}
