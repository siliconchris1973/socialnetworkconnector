package de.comlineag.snc.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.handler.TwitterPosting;
import de.comlineag.snc.handler.TwitterUser;


/**
 * 
 * @author 		Christian Guenther, Maic Rittmeier, Magnus Leinemann
 * @category 	Parser
 * @version		0.4a			- 01.10.2014
 * @status		productive
 * 
 * @description TwitterParser is the implementation of the generic parser for Twitter.
 * 				It decodes a tweet, passed along as a JSON String, calls the specific 
 * 				classes for posting (TwitterPosting) and user (TwitterUser) and 
 * 				finally calls the persistence manager to store the objects
 * 
 * @changelog	0.1 (Chris)		first skeleton
 * 				0.2 (Maic)		added support for list of posts	(no retweeted yet)
 * 				0.3 (Magnus)	added support for list of users and decode user
 * 				0.4	(Chris)		fixed retweet bug
 * 				0.4a			changed return value of method parse to boolean
 * 
 */
public final class TwitterParser extends GenericParser {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	public TwitterParser() {}

	@Override
	protected Boolean parse(String strTweet) {
		// log the startup message
		logger.debug("Twitter parser START");

		// macht ein JSon Decode aus dem uebergebenen String
		JSONParser parser = new JSONParser();
		List<TwitterPosting> postings = new ArrayList<TwitterPosting>();
		List<TwitterUser> users = new ArrayList<TwitterUser>();
		
		try {
			// first posts (tweets)
			JSONObject jsonTweetResource = (JSONObject) parser.parse(strTweet);
			TwitterPosting posting = new TwitterPosting(jsonTweetResource);
			
			// add posting to list
			postings.add(posting);
			
			// placing failsave method here, so that the original data object is stored on file
			// now instantiate a new JsonJilePersistence class with the data object and store the failed object on disk
			// intialize faile save persistence
			
			// retweeted posts need to go in message array as well
			JSONObject jsonReTweeted = (JSONObject) jsonTweetResource.get("retweeted_status");
			if (jsonReTweeted != null) {
				logger.debug("retweet found - adding to message iQueue");
				logger.trace("    retweeted message: " + jsonReTweeted.toString());
				postings.add(new TwitterPosting(jsonReTweeted));
			}
			
			// now users
			JSONObject jsonUser = (JSONObject) jsonTweetResource.get("user");
			TwitterUser user = new TwitterUser(jsonUser);
			users.add(user);
			
		} catch (ParseException e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		for (int ii = 0; ii < postings.size(); ii++) {
			if (RuntimeConfiguration.isPERSISTENCE_THREADING_ENABLED()){
				// execute persistence layer in a new thread, so that it does NOT block the crawler
				logger.trace("execute persistence layer in a new thread...");
				final TwitterPosting postT = (TwitterPosting) postings.get(ii);
				new Thread(new Runnable() {
					
					@Override
					public void run() {
							postT.save();
					}
				}).start();
			} else {
				// otherwise just call it sequentially
				TwitterPosting post = (TwitterPosting) postings.get(ii);
				post.save();
			}
		}
		
		for (int ii = 0; ii < users.size(); ii++) {
			TwitterUser user = (TwitterUser) users.get(ii);
			user.save();
		}
		
		logger.debug("Twitter parser END\n");
		return true;
	}

	@Override
	protected Boolean parse(InputStream is) {
		// THIS METHOD IS NOT USED
		return false;
	}
}
