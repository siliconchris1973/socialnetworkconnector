package de.comlineag.sbm.data;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 *
 * @description	SN_TwitterManager implementiert den Parser zur Dekodierung der Twitter postings
 *  
 */
public class SN_TwitterManager extends SN_Manager {

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public SN_TwitterManager() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void bigParser(String strTweet){
		// log the startup message
		logger.debug("method bigParser from class " + getClass().getName() + " called");
				
		// macht ein JSon Decode aus dem uebergebenen String
		// TODO: How to decode the map and seperate Tweets from Users and Retweets and URLs and so on
		JSONParser parser = new JSONParser();
		List<SN_TwitterPosting> postings =  new ArrayList<SN_TwitterPosting>();
		List<SN_TwitterUser> users = new ArrayList<SN_TwitterUser>();
		
		try {
			JSONObject jsonTweetResource = (JSONObject) parser.parse(strTweet);
			SN_TwitterPosting posting = new SN_TwitterPosting(jsonTweetResource);
			postings.add(posting);

			JSONObject jsonUser = (JSONObject) jsonTweetResource.get("user");
			SN_TwitterUser user = new SN_TwitterUser(jsonUser);
			users.add(user);

			JSONObject jsonReTweeted = (JSONObject) jsonTweetResource.get("retweeted_status");
			if(jsonReTweeted != null){
				postings.add(new SN_TwitterPosting(jsonReTweeted));
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error(e.toString());
		}

		SN_TwitterPostingManager post = new SN_TwitterPostingManager();
		post.save(postings); // hier Key fuer Tweets uebergeben


		SN_TwitterUserManager user = new SN_TwitterUserManager();
		user.save(users); // hier key fuer User uebergeben
	}

}
