package de.comlineag.sbm.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * 
 * @description LithiumParser implementation of the parser for Lithium postings and users
 * 				LithiumParsr is an extension of the default GenericParser. 
 * 				
 * 				LithiumParser creates objects for posting and user from the classes
 * 				LithiumUserData and LithiumPostingData, feeds this in a queue 
 * 				and finally calls the persistence manager to store the objects
 * 
 */
public final class LithiumParser extends GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	public LithiumParser() {}

	@Override
	protected void parse(String strPosting) {
		// macht ein JSon Decode aus dem uebergebenen String
		JSONParser parser = new JSONParser();
		List<LithiumPosting> postings = new ArrayList<LithiumPosting>();
		List<LithiumUser> users = new ArrayList<LithiumUser>();

		try {
			// zuerst suchen wir uns den post (tweet)
			JSONObject jsonPostResource = (JSONObject) parser.parse(strPosting);
			LithiumPosting posting = new LithiumPosting(jsonPostResource);
			postings.add(posting);

			// und dann den user
			JSONObject jsonUser = (JSONObject) jsonPostResource.get("user");
			LithiumUser user = new LithiumUser(jsonUser);
			users.add(user);
			
			//TODO check if retweeted REALLY is added
			JSONObject jsonRePosted = (JSONObject) jsonPostResource.get("retweeted_status");
			if (jsonRePosted != null) {
				postings.add(new LithiumPosting(jsonRePosted));
			}

		} catch (ParseException e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		for (int ii = 0; ii < postings.size(); ii++) {
			LithiumPosting post = (LithiumPosting) postings.get(ii);
			post.save();
		}
		
		for (int ii = 0; ii < users.size(); ii++) {
			LithiumUser user = (LithiumUser) users.get(ii);
			user.save();
		}
	}

	@Override
	protected void parse(InputStream is) {
		// this parse method is NOT used for the Lithium community
	}
	
	
}
