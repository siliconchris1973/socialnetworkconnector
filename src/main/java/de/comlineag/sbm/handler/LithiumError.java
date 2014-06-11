package de.comlineag.sbm.handler;

import java.util.ArrayList;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.comlineag.sbm.data.LithiumErrorData;

public class LithiumError extends DefaultHandler {
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	public LithiumError() {}

	// This is the list which shall be populated while parsing the XML.
	private ArrayList errorList = new ArrayList();
	
	// As we read any XML element we will push that in this stack
	private Stack elementStack = new Stack();
	
	// As we complete one error block in XML, we will push the Error instance in errorList
	private Stack objectStack = new Stack();
	
	public void startDocument() throws SAXException {
		logger.trace("start of the document   : ");
	}

	public void endDocument() throws SAXException {
		logger.trace("end of the document     : ");
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		logger.trace("new element " + localName + "(" + qName + ") with " + attributes + " to push on errorList stack " + uri);
		//Push it in element stack    
		this.elementStack.push(qName);

		//If this is start of 'error' element then prepare a new Error instance and push it in object stack
		if ("response status".equals(qName)) {
			//New Error instance
			LithiumErrorData error = new LithiumErrorData();
			
			//Set all required attributes in any XML element here itself
			if(attributes != null && attributes.getLength() == 1) {
				error.setId(Integer.parseInt(attributes.getValue(0)));
			}
			
			this.objectStack.push(error);
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		logger.trace("endElement called");
		//Remove last added element
		this.elementStack.pop();
		
		//Error instance has been constructed so pop it from object stack and push in errorList
		if ("error".equals(qName)) {
			LithiumErrorData object = (LithiumErrorData) this.objectStack.pop();
			this.errorList.add(object);
		}
	}
	
	// This will be called every time the parser encounters a value node
	public void characters(char[] ch, int start, int length) throws SAXException {
		String value = new String(ch, start, length).trim();
		
		if (value.length() == 0) {
			return; // ignore white space
		}

		//handle the value based on to which element it belongs
		if ("error code".equals(currentElement())) {
			LithiumErrorData error = (LithiumErrorData) this.objectStack.peek();
			error.setErrorCode(value);
		} else if ("message".equals(currentElement())) {
			LithiumErrorData error = (LithiumErrorData) this.objectStack.peek();
			error.setMessage(value);
		}
	}

	//Utility method for getting the current element in processing
	private String currentElement() {
		return (String) this.elementStack.peek();
	}

	//Accessor for errorList object
	public ArrayList getErrors() {
		return errorList;
	}
}
