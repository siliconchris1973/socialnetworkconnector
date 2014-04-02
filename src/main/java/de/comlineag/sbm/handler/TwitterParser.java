<<<<<<< HEAD
=======

>>>>>>> FETCH_HEAD
package de.comlineag.sbm.handler;

import de.comlineag.sbm.data.*;
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
 * @description	TwitterParser implementiert den Parser zur Dekodierung der Twitter postings
 *  
 */
public final class TwitterParser extends GenericParser {

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public TwitterParser() {}

	@Override
	protected void parse(String strTweet){
		// log the startup message
		logger.info("method parse from class " + getClass().getName() + " called");
				
		// macht ein JSon Decode aus dem uebergebenen String
		// TODO: How to decode the map and seperate Tweets from Users and Retweets and URLs and so on
		JSONParser parser = new JSONParser();
		List<TwitterPosting> postings =  new ArrayList<TwitterPosting>();
		List<TwitterUser> users = new ArrayList<TwitterUser>();
		logger.debug("HIHO hier ist der Osterhase");
		
		try {
			// zuerst suchen wir uns die posts (tweets) 
			JSONObject jsonTweetResource = (JSONObject) parser.parse(strTweet);
			TwitterPostingData posting = new TwitterPostingData(jsonTweetResource);
			//postings.add(posting);
			
			logger.error("DIES ist ein Fehler");
			
			// und dann die user
			JSONObject jsonUser = (JSONObject) jsonTweetResource.get("user");
			TwitterUserData user = new TwitterUserData(jsonUser);
			//users.add(user);

			JSONObject jsonReTweeted = (JSONObject) jsonTweetResource.get("retweeted_status");
			if(jsonReTweeted != null){
//				postings.add(new TwitterPostingData(jsonReTweeted));
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error(e.toString());
		}

<<<<<<< HEAD
for (int ii = 0; ii < postings.size(); ii++){
	TwitterPosting post = (TwitterPosting) postings.get(ii);
	post.save(); // hier Key fuer Tweets uebergeben
	
}
//		TwitterPosting post = new TwitterPosting();
//		post.save(postings); // hier Key fuer Tweets uebergeben

for (int ii = 0; ii < users.size(); ii++){
	TwitterUser user = (TwitterUser) users.get(ii);
	user.save(); // hier key fuer User uebergeben
	
}
//		TwitterUser user = new TwitterUser();
//		user.save(users); // hier key fuer User uebergeben
=======
		for (int ii = 0; ii < postings.size(); ii++){
			TwitterPosting post = (TwitterPosting) postings.get(ii);
			post.save(); // hier Key fuer Tweets uebergeben
			
		}
		//		TwitterPosting post = new TwitterPosting();
		//		post.save(postings); // hier Key fuer Tweets uebergeben

		for (int ii = 0; ii < users.size(); ii++){
			TwitterUser user = (TwitterUser) users.get(ii);
			user.save(); // hier key fuer User uebergeben
			
		}
		//		TwitterUser user = new TwitterUser();
		//		user.save(users); // hier key fuer User uebergeben
>>>>>>> FETCH_HEAD

	}

}

