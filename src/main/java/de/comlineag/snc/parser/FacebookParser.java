package de.comlineag.snc.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.base.Stopwatch;

import de.comlineag.snc.handler.FacebookPosting;
import de.comlineag.snc.handler.FacebookUser;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Handler
 * @version		0.1
 * @status		not implemented
 * 
 * @description FacebookParser implementation of the parser for facebook postings
 * 				calls specific classes for posting and user for every object
 * 				and finally calls the persistence manager to store the objects
 * 
 * @changelog	0.1 (Chris)		copy from TwitterParser
 * 
 * TODO implement facebook data parser
 */
public final class FacebookParser extends GenericParser {

	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	public FacebookParser() {}

	@Override
	protected boolean parse(String strPost) {
		String PARSER_NAME="Facebook";
		Stopwatch timer = new Stopwatch().start();
		
		// log the startup message
		logger.debug(PARSER_NAME + " parser START");

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
		
		timer.stop();
		logger.debug(PARSER_NAME + " parser END - parsing took "+timer.elapsed(TimeUnit.SECONDS)+" seconds");
		return true;
	}

	@Override
	protected boolean parse(InputStream is) {return false;}
}
