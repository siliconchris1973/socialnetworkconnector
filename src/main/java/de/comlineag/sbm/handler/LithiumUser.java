package de.comlineag.sbm.handler;

import java.util.ArrayList;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.comlineag.sbm.data.LithiumUserData;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description Implementation of the lithium user manager - extends
 *              GenericDataManager. This handler is used to save a new user or
 *              update an existing one. LithiumUserManager is called after a
 *              posting with all relevant information about the user (posting
 *              user as well as mentioned users) is decoded by LithiumParser.
 * 				
 * 				The data type lithium user consists of these elements
 *            		id 					Long 
 *            		name 				String 
 *            		screen_name 		String 
 *            		location 			List
 *            		followers_count		Long 
 *            		friends_count 		Long 
 *            		statuses_count 		Long
 *            		favourites_count	Long 
 *            		listed_count		Long 
 *            		lang				String
 * 
 * @param none
 * 
 */

public class LithiumUser extends DefaultHandler { //GenericDataManager<LithiumUserData> {
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	public LithiumUser() {}
	
	/*
	private LithiumUserData data;

	//private final Logger logger = Logger.getLogger(getClass().getName());
	public LithiumUser(JSONObject jsonObject) {
		data = new LithiumUserData(jsonObject);
	}
	
	// public void save(List<LithiumUser> users){
	public void save() {
		persistenceManager.saveUsers(data);
	}
	*/
	
	// SAX CODE
	// This is the list which shall be populated while parsing the XML.
	private ArrayList userList = new ArrayList();
	
	// As we read any XML element we will push that in this stack
	private Stack elementStack = new Stack();
	
	// As we complete one user block in XML, we will push the User instance in userList
	private Stack objectStack = new Stack();
	
	public void startDocument() throws SAXException {
		logger.trace("start of the document   : ");
	}

	public void endDocument() throws SAXException {
		logger.trace("end of the document     : ");
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		//Push it in element stack    
		this.elementStack.push(qName);

		//If this is start of 'user' element then prepare a new User instance and push it in object stack
		if ("user".equals(qName)) {
			//New User instance
			LithiumUserData user = new LithiumUserData();
			
			//Set all required attributes in any XML element here itself
			if(attributes != null && attributes.getLength() == 1) {
				user.setId(Integer.parseInt(attributes.getValue(0)));
			}
			
			this.objectStack.push(user);
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		//Remove last added  element
		this.elementStack.pop();
		
		//User instance has been constructed so pop it from object stack and push in userList
		if ("user".equals(qName)) {
			LithiumUserData object = (LithiumUserData) this.objectStack.pop();
			this.userList.add(object);
		}
	}
	
	//This will be called everytime parser encounter a value node
	public void characters(char[] ch, int start, int length) throws SAXException {
		String value = new String(ch, start, length).trim();
		
		if (value.length() == 0) {
			return; // ignore white space
		}

		//handle the value based on to which element it belongs
		if ("screenname".equals(currentElement())) {
			LithiumUserData user = (LithiumUserData) this.objectStack.peek();
			user.setScreenName(value);
		} else if ("username".equals(currentElement())) {
			LithiumUserData user = (LithiumUserData) this.objectStack.peek();
			user.setUsername(value);
		}
	}

	//Utility method for getting the current element in processing
	private String currentElement() {
		return (String) this.elementStack.peek();
	}

	//Accessor for userList object
	public ArrayList getUsers() {
		return userList;
	}
	
}
