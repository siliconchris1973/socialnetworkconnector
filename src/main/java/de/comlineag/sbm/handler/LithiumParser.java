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
 * @description LithiumParser implementation of the parser for lithium postings
 * 				calls specific classes for posting and user for every object
 * 				and finally calls the persistence manager to store the objects
 * 
 */
public final class LithiumParser extends GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());

	public LithiumParser() {}

	@Override
	protected void parse(String strPost) {
		// log the startup message
		logger.debug("parser START");

		// macht ein JSon Decode aus dem uebergebenen String
		JSONParser parser = new JSONParser();
		List<LithiumPosting> postings = new ArrayList<LithiumPosting>();
		List<LithiumUser> users = new ArrayList<LithiumUser>();

		try {
			// zuerst suchen wir uns den post (post)
			JSONObject jsonPostResource = (JSONObject) parser.parse(strPost);
			LithiumPosting posting = new LithiumPosting(jsonPostResource);
			postings.add(posting);

			// und dann den user
			JSONObject jsonUser = (JSONObject) jsonPostResource.get("user");
			LithiumUser user = new LithiumUser(jsonUser);
			users.add(user);
			
			// zum schluss noch etwaige reposted messages
			//TODO check if reposted REALLY is added
			JSONObject jsonRePosted = (JSONObject) jsonPostResource.get("reposted_status");
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
		logger.debug("Parser END");
	}
}
