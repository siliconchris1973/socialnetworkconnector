package de.comlineag.sbm.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
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
public final class LithiumParser extends DefaultHandler { //GenericParser {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	// now some static modifier for the sax parsing
	static final   String       sNEWLINE   = System.getProperty( "line.separator" );
	static private Writer       out        = null;
	private        StringBuffer textBuffer = null;
	
	
	public LithiumParser() {}

	public LithiumParser(Object content) {}

	
	// ---- SAX DefaultHandler methods ----
	@Override 
	public void startDocument() throws SAXException {
		echoString( sNEWLINE + "<?xml ...?>" + sNEWLINE + sNEWLINE );
	}

	@Override
	public void endDocument() throws SAXException {
	    echoString( sNEWLINE );
	}

	@Override
	public void startElement( String namespaceURI,
			  					String localName,   // local name
	                            String qName,       // qualified name
	                            Attributes attrs )
	                            		throws SAXException {
	    
		echoTextBuffer();
		String eName = ( "".equals( localName ) ) ? qName : localName;
	    echoString( "<" + eName );                  // element name
	    if( attrs != null ) {
	    	for( int i=0; i<attrs.getLength(); i++ ) {
	    		String aName = attrs.getLocalName( i ); // Attr name
	    		if( "".equals( aName ) )  aName = attrs.getQName( i );
	    		echoString( " " + aName + "=\"" + attrs.getValue( i ) + "\"" );
	    	}
	    }
	    echoString( ">" );
	}

	@Override  
	public void endElement( String namespaceURI,
	                          String localName,     // local name
	                          String qName )        // qualified name
	                        		  throws SAXException {
		echoTextBuffer();
		String eName = ( "".equals( localName ) ) ? qName : localName;
	    echoString( "</" + eName + ">" );           // element name  
	}


	@Override  
	public void characters( char[] buf, int offset, int len )
			throws SAXException {
		String s = new String( buf, offset, len );
		if( textBuffer == null )
			textBuffer = new StringBuffer( s );
		else
			textBuffer.append( s );
	}
	
	
	// ---- Helper methods ----
	// Display text accumulated in the character buffer  
	private void echoTextBuffer()
			throws SAXException {
		if( textBuffer == null )  return;
		echoString( textBuffer.toString() );
		textBuffer = null;
	}
	
	
	// Wrap I/O exceptions in SAX exceptions, to
	// suit handler signature requirements
	private void echoString( String s )
			throws SAXException {
		try {
			if( null == out )
				out = new OutputStreamWriter( System.out, "UTF8" );
			out.write( s );
			out.flush();
		} catch( IOException ex ) {
			throw new SAXException( "I/O error", ex );
		}
	}
	
	//@Override
	//protected void parse(String strPost) {
	protected void parse(InputStream is) {
		// log the startup message
		logger.debug("Lithium parser START");
		
		logger.trace("this is what I got from you " + is.toString());
		
		/* ALTER Parser
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
		*/
		
		logger.debug("Lithium parser END");
	}
}
