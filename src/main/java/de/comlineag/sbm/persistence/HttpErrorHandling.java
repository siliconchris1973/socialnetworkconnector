package de.comlineag.sbm.persistence;

import org.apache.commons.httpclient.HttpStatus;

public class HttpErrorHandling { // extends Exception {
//	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Error handling for graph actions
	 */
	//private static final long serialVersionUID = 4751739156910409082L;
//	private String httpErrorText;

	//	public String returnHttpErrorText(HttpStatusCode inputString) {
//	public HttpErrorHandling(String inputString){
//		httpErrorText = setHttpErrorText(inputString);
//	}


//	public HttpErrorHandling(){}

//	public String getHttpErrorText() {
//		return httpErrorText;
//	}
	
	// simple static text function to return funny messages on http errors
	public static String getHttpErrorText(int errorCode){
		String inputString = HttpStatus.getStatusText(errorCode);
		switch(errorCode) {
		case HttpStatus.SC_OK:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: Hey, what do you want from me? Ok is Ok. There is no error here. Go stick your nose someplace else.";
		case HttpStatus.SC_BAD_GATEWAY:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: the intermediate gateway is wrong - why do we use it anyway?";
		case HttpStatus.SC_BAD_REQUEST:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: malformed request sent - come on, you can do better than that";
		case HttpStatus.SC_CONFLICT:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: conflicting requests send to server - you guys should play nice together";
		case HttpStatus.SC_FORBIDDEN:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: the requested operation is forbidden on this server!";
		case HttpStatus.SC_GATEWAY_TIMEOUT:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the intermediate gateway did not respond in time - maybe it takes a nap";
		case HttpStatus.SC_GONE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the requested object does no longer exist - did it ever?";
		case HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the request was formatted in an unsupported http version - how can that ever be?";
		case HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: ressources are limited and at the moment there simply is not enough (of whatever we needed) for your request";
		case HttpStatus.SC_INSUFFICIENT_STORAGE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: oops, seems like we ran out of diskspace on the server side";
		case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: guru meditation! the server encountered an internal error";
		case HttpStatus.SC_METHOD_FAILURE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the invoked method failed, for unknown reasons";
		case HttpStatus.SC_MOVED_TEMPORARILY:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: look, the object you requested is not here, ok? So could you please be so kind and also just move on?";
		case HttpStatus.SC_METHOD_NOT_ALLOWED:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: whoa, wait a minute - you are not allowed to do that on my server";
		case HttpStatus.SC_NO_CONTENT:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: your request ended in void space - there is not content";
		case HttpStatus.SC_NOT_ACCEPTABLE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: this is simply not acceptable - try something else";
		case HttpStatus.SC_NOT_FOUND:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the requested object could not be found. To be absolutely clear: This is NOT my fault. Please recheck your query!";
		case HttpStatus.SC_REQUEST_TOO_LONG:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: could you phrase your request in a shorter way?";
		case HttpStatus.SC_REQUEST_TIMEOUT:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: I tried! Really I tried, but my ressources are limited and I could not fullfill your request in time";
		case HttpStatus.SC_REQUEST_URI_TOO_LONG:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: Hey, I am just a simple machine, and my brain is not big enough to handle such long ressource identifier";
		case HttpStatus.SC_SERVICE_UNAVAILABLE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: We are sorry to inform you that the requested service is permanently or at least temporarly closed. Please come back later.";
		case HttpStatus.SC_UNAUTHORIZED:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: I am perfectly capable of fullfilling your request. BUT YOU ARE NOT AUTHORIZED! so please go ayway.";
		case HttpStatus.SC_UNPROCESSABLE_ENTITY:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: Whatever it is you wanted from me, I am not able to cope";
		default:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: I have no idea what just happened - let us not try this again, shall we?";
		}
	}
}
