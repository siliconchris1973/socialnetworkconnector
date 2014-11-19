package de.comlineag.snc.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.base.Stopwatch;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.constants.SocialNetworks;
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
	// this holds a reference to the runtime cinfiguration
	private final RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	
	private final boolean rtcPersistenceThreading = rtc.getBooleanValue("PersistenceThreadingEnabled", "runtime");
	
	
	public TwitterParser() {}

	@Override
	protected boolean parse(String strTweet) {
		String PARSER_NAME="Twitter";
		Stopwatch timer = new Stopwatch().start();
		
		// log the startup message
		logger.debug(PARSER_NAME + " parser START");

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
		
		
		
		// need to add users first, because a tweet needs to be able to point to a posting user in the db
		logger.trace("trying to save " + users.size() + " users");
		for (int ii = 0; ii < users.size(); ii++) {
			if (rtcPersistenceThreading){
				// execute persistence layer in a new thread, so that it does NOT block the crawler
				logger.trace("execute persistence layer in a new thread...");
				final TwitterUser userT = (TwitterUser) users.get(ii);
				new Thread(new Runnable() {
					
					@Override
					public void run() {
							userT.save();
					}
				}).start();
			} else {
				TwitterUser user = (TwitterUser) users.get(ii);
				user.save();
			}
		}
		
		logger.trace("trying to save " + postings.size() + " tweets");
		for (int ii = 0; ii < postings.size(); ii++) {
			if (rtcPersistenceThreading){
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
		
		
		timer.stop();
		logger.debug(PARSER_NAME + " parser END - parsing took "+timer.elapsed(TimeUnit.SECONDS)+" seconds");
		return true;
	}
	
	
	// THIS METHOD IS NOT USED
	@Override 
	protected boolean parse(InputStream is) {return false;}
}
