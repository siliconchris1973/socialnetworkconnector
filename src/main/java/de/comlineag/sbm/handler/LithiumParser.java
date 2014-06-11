package de.comlineag.sbm.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.comlineag.sbm.data.LithiumUserData;
import de.comlineag.sbm.data.LithiumPostingData;
import de.comlineag.sbm.data.LithiumErrorData;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description LithiumParser implementation of the parser for Lithium postings and users
 * 				LithiumParsr is an extension of the default SAX handler and NOT, as with
 * 				TwitterParser, an extension of the GenericParser. 
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
		// THIS ONE IS NOT NEEDED
	}

	@Override
	protected void parse(InputStream is) {
		// this parse method is used for the Lithium community 
		
	}
	
	public ArrayList<LithiumErrorData> parseXml(InputStream in) {
		logger.debug("Lithium parser START");
		
		// Create a empty link of Errors initially
		ArrayList<LithiumErrorData> errors = new ArrayList<LithiumErrorData>();
		try {
			// Create default handler instance
			LithiumError handler = new LithiumError();
			
			// Create parser from factory
			XMLReader parser = XMLReaderFactory.createXMLReader();
			
			// Register handler with parser
			parser.setContentHandler(handler);
			
			//Create an input source from the XML input stream
			InputSource source = new InputSource(in);
			
			//parse the document
			parser.parse(source);
			
			//populate the parsed users list in above created empty list; You can return from here also.
			errors = handler.getErrors();
			
		} catch (SAXException e) {
			logger.error("EXCEPTION :: SAXException " + e.getStackTrace().toString());
		} catch (IOException e) {
			logger.error("EXCEPTION :: IOException " + e.getStackTrace().toString());
		} finally {}
	
		logger.debug("Lithium parser END");
		
		return errors;
	}
	
	/* This is the original parse method
	//@Override
	//protected void parse(String strPost) {
	protected void parse(InputStream is) {
		// log the startup message
		logger.debug("Lithium parser START");
		
		logger.trace("this is what I got from you " + is.toString());
		
		// ALTER Parser
		logger.trace("this is the content of the input source " + strPost.toString());
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
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e.getErrorType());
		}
		
		for (int ii = 0; ii < postings.size(); ii++) {
			LithiumPosting post = (LithiumPosting) postings.get(ii);
			post.save();
		}
		
		for (int ii = 0; ii < users.size(); ii++) {
			LithiumUser user = (LithiumUser) users.get(ii);
			user.save();
		}
		
		
		logger.debug("Lithium parser END");
	}
	*/
}
