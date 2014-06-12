package de.comlineag.sbm.data;

public final class LithiumConstants {
	public final String CC_COMMUNITY_ID							= "hiaeh67457";
	
	// constants for the json response structure
	public final String JSON_RESPONSE_OBJECT_TEXT				= "response";
	public final String JSON_STATUS_CODE_TEXT					= "status";
	public final String JSON_MESSAGES_OBJECT_IDENTIFIER			= "messages";
	public final String JSON_SINGLE_MESSAGE_OBJECT_IDENTIFIER	= "message";
	public final String JSON_MESSAGE_REFERENCE					= "href";
	
	// pathes to REST endpoints
	public final String REST_MESSAGES_SEARCH_URI				= "/search/messages";
	
	// constants for the search process
	public final String SEARCH_TERM								= "phrase";
	
	// constants for the http connection - these used to come from applicationContents.xml
	public final String PROTOCOL								= "https";
	public final String SERVER_URL								= "wissen.cortalconsors.de";
	public final String PORT 									= "443";
	public final String REST_API_LOC							= "/restapi/vc";
	public final String HTTP_RESPONSE_FORMAT					= "json";
	public final String HTTP_RESPONSE_FORMAT_COMMAND			= "restapi.response_format";
	
	public LithiumConstants(){}
	
}
