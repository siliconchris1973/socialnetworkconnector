package de.comlineag.sbm.handler;

import java.util.ArrayList;
import java.util.Stack;
import org.apache.log4j.Logger;

import org.json.simple.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.comlineag.sbm.data.LithiumPostingData;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * 
 * @description Implementation of the lithium posting manager - extends
 *              GenericDataManager This handler is used to save a new post or
 *              update an existing one. LithiumPostingManager is called after a
 *              posting with all relevant information about the posting (the
 *              original as well as the reposted one) is decoded by
 *              LithiumParser.
 * 
 * 				The data type lithium posting consists of these elements
 *	            	id						Long 
 *					created_at				String 
 *					text					String 
 *					source					String
 *            		truncated				Boolean 
 *            		in_reply_to_status_id	Long
 *            		in_reply_to_user_id		Long 
 *            		in_reply_to_screen_name	String
 *            		coordinates				List 
 *            		place					List 
 *            		lang					String 
 *            		hashtags				List
 *            		symbols					List
 *            		user_mentions			List
 * 
 * @param <LithiumPosting>
 * 
 */

public class LithiumPosting extends DefaultHandler { // GenericDataManager<LithiumPostingData> {
	/*
	 * Die nachfolgenden Elemente des Posts sollen weiter verarbeitet und
	 * gespeichert werden
	 * 
	 * key="cl_postID" 					value="id" 
	 * key="cl_postTime" 				value="created_at"
	 * key="cl_posting" 				value="text" 
	 * key="cl_postClient" 				value="source"
	 * key="cl_postTruncated" 			value="truncated" 
	 * key="cl_postInReplyTo" 			value="in_reply_to_status_id" 
	 * key="cl_postInReplyToUserID" 	value="in_reply_to_user_id" 
	 * key="cl_postInReplyToScreenName"	value="in_reply_to_screen_name" 
	 * key="cl_postGeoLocation"			value="coordinates" 
	 * key="cl_postPlace"				value="place" 
	 * key="cl_postLang" 				value="lang" 
	 * key="cl_postHashtags" 			value="hashtags" 
	 * key="cl_postSymbols" 			value="symbols"
	 * key="cl_userMentions" 			value="mentions"
	 */
	
	/* ORIGINAL CODE
	private LithiumPostingData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());
	
	public LithiumPosting(JSONObject jsonObject) {
		data = new LithiumPostingData(jsonObject);
	}

	@Override
	// public void save(List<LithiumPosting> posting){
	public void save() {
		persistenceManager.savePosts(data);
	}
	*/
	
	// SAX CODE
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// This is the list which shall be populated while parsing the XML.
	private ArrayList<LithiumPostingData> postingList = new ArrayList<LithiumPostingData>();
	
	// As we read any XML element we will push that in this stack
	private Stack<String> elementStack = new Stack<String>();
	
	// As we complete one user block in XML, we will push the Posting instance in userList
	private Stack<LithiumPostingData> objectStack = new Stack<LithiumPostingData>();
	
	public void startDocument() throws SAXException {
		logger.trace("start of the xml input stream   : ");
	}

	public void endDocument() throws SAXException {
		logger.trace("end of the xml input stream     : ");
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		logger.trace("new " + localName + "-element to push on postingList stack " + uri);
		
		//Push it in element stack    
		this.elementStack.push(qName);

		//If this is start of 'user' element then prepare a new Posting instance and push it in object stack
		if ("message".equals(qName)) {
			//New Posting instance
			LithiumPostingData posting = new LithiumPostingData();
			
			//Set all required attributes in any XML element here itself
			if(attributes != null && attributes.getLength() == 1) {
				posting.setId(Integer.parseInt(attributes.getValue(0)));
			}
			
			this.objectStack.push(posting);
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		logger.trace("endElement for " + qName + " found");
		
		//Remove last added  element
		this.elementStack.pop();
		
		//User instance has been constructed so pop it from object stack and push in userList
		if ("user".equals(qName)) {
			LithiumPostingData object = this.objectStack.pop();
			this.postingList.add(object);
		}
	}
	
	//This will be called everytime parser encounter a value node
	public void characters(char[] ch, int start, int length) throws SAXException {
		String value = new String(ch, start, length).trim();
		
		if (value.length() == 0) {
			return; // ignore white space
		}

		//handle the value based on to which element it belongs
		if ("id".equals(currentElement())) {
			LithiumPostingData posting = this.objectStack.peek();
			posting.setId(Integer.parseInt(value));
		} else if ("text".equals(currentElement())) {
			LithiumPostingData posting = this.objectStack.peek();
			posting.setText(value);
		}
	}

	// Utility method for getting the current element in processing
	private String currentElement() {
		return this.elementStack.peek();
	}

	// Accessor for userList object
	public ArrayList<LithiumPostingData> getPostings() {
		return postingList;
	}
}
