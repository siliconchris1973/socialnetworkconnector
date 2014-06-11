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
 * @author Christian Guenther
 * @category Handler
 * 
 * @description FacebookParser implementation of the parser for facebook postings
 * 				calls specific classes for posting and user for every object
 * 				and finally calls the persistence manager to store the objects
 * 
 */
public final class FacebookParser extends GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());

	public FacebookParser() {}

	@Override
	protected void parse(String strPost) {
		// log the startup message
		logger.debug("Facebook parser START");

		// macht ein JSon Decode aus dem uebergebenen String
		JSONParser parser = new JSONParser();
		List<FacebookPosting> postings = new ArrayList<FacebookPosting>();
		List<FacebookUser> users = new ArrayList<FacebookUser>();

		try {
			// zuerst suchen wir uns den post (post)
			JSONObject jsonPostResource = (JSONObject) parser.parse(strPost);
			FacebookPosting posting = new FacebookPosting(jsonPostResource);
			postings.add(posting);

			// und dann den user
			JSONObject jsonUser = (JSONObject) jsonPostResource.get("user");
			FacebookUser user = new FacebookUser(jsonUser);
			users.add(user);
			
			// zum schluss noch etwaige reposted messages
			//TODO check if reposted REALLY is added
			JSONObject jsonRePosted = (JSONObject) jsonPostResource.get("reposted_status");
			if (jsonRePosted != null) {
				postings.add(new FacebookPosting(jsonRePosted));
			}

		} catch (ParseException e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		for (int ii = 0; ii < postings.size(); ii++) {
			FacebookPosting post = (FacebookPosting) postings.get(ii);
			post.save();
		}
		
		for (int ii = 0; ii < users.size(); ii++) {
			FacebookUser user = (FacebookUser) users.get(ii);
			user.save();
		}
		logger.debug("Facebook parser END");
	}

	@Override
	protected void parse(InputStream is) {
		// TODO CHECK IF THIS ONE IS NEEDED
		
	}
}
