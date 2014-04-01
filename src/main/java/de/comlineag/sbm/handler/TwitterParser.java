package de.comlineag.sbm.handler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.comlineag.sbm.data.*;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 *
 * @description	TwitterParser implementiert den Parser zur Dekodierung der Twitter postings
 *  
 */
public class TwitterParser extends GenericParser {

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public TwitterParser() {}

	@Override
	protected void parse(String strTweet){
		// log the startup message
		logger.debug("method bigParser from class " + getClass().getName() + " called");
				
		// macht ein JSon Decode aus dem uebergebenen String
		// TODO: How to decode the map and seperate Tweets from Users and Retweets and URLs and so on
		JSONParser parser = new JSONParser();
		List<TwitterPosting> postings =  new ArrayList<TwitterPosting>();
		List<TwitterUser> users = new ArrayList<TwitterUser>();
		
		try {
			JSONObject jsonTweetResource = (JSONObject) parser.parse(strTweet);
			TwitterPosting posting = new TwitterPosting(jsonTweetResource);
			postings.add(posting);

			JSONObject jsonUser = (JSONObject) jsonTweetResource.get("user");
			TwitterUser user = new TwitterUser(jsonUser);
			users.add(user);

			JSONObject jsonReTweeted = (JSONObject) jsonTweetResource.get("retweeted_status");
			if(jsonReTweeted != null){
				postings.add(new TwitterPosting(jsonReTweeted));
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error(e.toString());
		}

		TwitterPostingManager post = new TwitterPostingManager();
//		post.save(postings); // hier Key fuer Tweets uebergeben
		post.save(); // hier Key fuer Tweets uebergeben


		TwitterUserManager user = new TwitterUserManager();
//		user.save(users); // hier key fuer User uebergeben
		user.save(); // hier key fuer User uebergeben
	}

}
