package de.comlineag.snc.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 * @author 		Christian Guenther, Maic Rittmeier, Magnus Leinemann
 * @category 	Parser
 * @version		0.3
 * 
 * @description TwitterParser is the implementation of the generic parser for Twitter.
 * 				It decodes a tweet, passed along as a JSON String, calls the specific 
 * 				classes for posting (TwitterPosting) and user (TwitterUser) and 
 * 				finally calls the persistence manager to store the objects
 * 
 * @changelog	0.1 first skeleton										Chris
 * 				0.2 added support for list of posts	(no retweeted yet)	Maic
 * 				0.3 added support for list of users and decode user		Magnus
 * 
 * @TODO		add support for retweeted tweets and bring to version 1.0
 */
public final class TwitterParser extends GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());

	public TwitterParser() {}

	@Override
	protected void parse(String strTweet) {
		// log the startup message
		logger.debug("Twitter parser START");

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
		logger.debug("Twitter parser END");
	}

	@Override
	protected void parse(InputStream is) {
		// THIS ONE IS NOT USED
	}
}
