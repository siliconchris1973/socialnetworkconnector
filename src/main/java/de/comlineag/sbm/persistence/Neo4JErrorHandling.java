package de.comlineag.sbm.persistence;

import org.apache.commons.httpclient.HttpStatus;


//import de.comlineag.sbm.data.HttpStatusCode;

public class Neo4JErrorHandling { // extends Exception {
//	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Error handling for graph actions
	 */
	//private static final long serialVersionUID = 4751739156910409082L;
//	private String httpErrorText;

	//	public String returnHttpErrorText(HttpStatusCode inputString) {
//	public Neo4JErrorHandling(String inputString){
//		httpErrorText = setHttpErrorText(inputString);
//	}


//	public Neo4JErrorHandling(){}

//	public String getHttpErrorText() {
//		return httpErrorText;
//	}

	// TODO this function must be eliminated by a constructor
//	public String returnHttpErrorText(String inputString){
//		logger.debug("returnHttpErrorText for " + inputString + " called");
//
//		switch(inputString) {
//		case "BAD_GATEWAY":
//			httpErrorText = inputString + " :: the intermediate gateway is wrong - why do we use it anyway?";
//		case "BAD_REQUEST":
//			httpErrorText = inputString + " :: malformed request sent - come on, you can do better than that";
//		case "CONFLICT":
//			httpErrorText = inputString + " :: conflicting requests send to server - you guys should play nice together";
//		case "FORBIDDEN":
//			httpErrorText = inputString + " :: the requested operation is forbidden on this server!";
//		case "GATEWAY_TIMEOUT":
//			httpErrorText = inputString + " :: the intermediate gateway did not respond in time - maybe it takes a nap";
//		case "GONE":
//			httpErrorText = inputString + " :: the requested object does no longer exist - did it ever?";
//		case "HTTP_VERSION_NOT_SUPPORTED":
//			httpErrorText = inputString + " :: the request was formatted in an unsupported http version - how can that ever be?";
//		case "INSUFFICIENT_SPACE_ON_RESOURCE":
//			httpErrorText = inputString + " :: ressources are limited and at the moment there simply is not enough (of whatever we needed) for your request";
//		case "INSUFFICIENT_STORAGE":
//			httpErrorText = inputString + " :: oops, seems like we ran out of diskspace on the server side";
//		case "INTERNAL_SERVER_ERROR":
//			httpErrorText = inputString + " :: guru meditation! the server encountered an internal error";
//		case "METHOD_FAILURE":
//			httpErrorText = inputString + " :: the invoked method failed, for unknown reasons";
//		case "METHOD_NOT_ALLOWED":
//			httpErrorText = inputString + " :: whoa, wait a minute - you are not allowed to do that on my server";
//		case "NO_CONTENT":
//			httpErrorText = inputString + " :: your request ended in void space - there is not content";
//		case "NOT_ACCEPTABLE":
//			httpErrorText = inputString + " :: this is simply not acceptable - try something else";
//		case "NOT_FOUND":
//			httpErrorText = inputString + " :: the requested object could not be found. To be absolutely clear: This is NOT my fault. Please recheck your query!";
//		case "REQUEST_ENTITY_TOO_LARGE":
//			httpErrorText = inputString + " :: could you phrase your request in a shorter way?";
//		case "REQUEST_TIMEOUT":
//			httpErrorText = inputString + " :: I tried! Really I tried, but my ressources are limited and I could not fullfill your request in time";
//		case "REQUEST_URI_TOO_LONG":
//			httpErrorText = inputString + " :: Hey, I am just a simple machine, and my brain is not big enough to handle such long ressource identifier";
//		case "SERVICE_UNAVAILABLE":
//			httpErrorText = inputString + " :: We are sorry to inform you that the requested service is permanently or at least temporarly closed. Please come back later.";
//		case "UNAUTHORIZED":
//			httpErrorText = inputString + " :: I am perfectly capable of fullfilling your request. BUT YOU ARE NOT AUTHORIZED! so please go ayway.";
//		case "UNPROCESSABLE_ENTITY":
//			httpErrorText = inputString + " :: Whatever it is you wanted from me, I am not able to cope";
//
//		default:
//			httpErrorText = "I have no idea what just happened - let us not try this again, shall we?";
//		}
//
//		return httpErrorText;
//	}


	 // TODO this must be done somehow else
//	public String setHttpErrorText(String inputString){
//		switch(inputString) {
//		case "BAD_GATEWAY":
//			httpErrorText = inputString + " :: the intermediate gateway is wrong - why do we use it anyway?";
//		case "BAD_REQUEST":
//			httpErrorText = inputString + " :: malformed request sent - come on, you can do better than that";
//		case "CONFLICT":
//			httpErrorText = inputString + " :: conflicting requests send to server - you guys should play nice together";
//		case "FORBIDDEN":
//			httpErrorText = inputString + " :: the requested operation is forbidden on this server!";
//		case "GATEWAY_TIMEOUT":
//			httpErrorText = inputString + " :: the intermediate gateway did not respond in time - maybe it takes a nap";
//		case "GONE":
//			httpErrorText = inputString + " :: the requested object does no longer exist - did it ever?";
//		case "HTTP_VERSION_NOT_SUPPORTED":
//			httpErrorText = inputString + " :: the request was formatted in an unsupported http version - how can that ever be?";
//		case "INSUFFICIENT_SPACE_ON_RESOURCE":
//			httpErrorText = inputString + " :: ressources are limited and at the moment there simply is not enough (of whatever we needed) for your request";
//		case "INSUFFICIENT_STORAGE":
//			httpErrorText = inputString + " :: oops, seems like we ran out of diskspace on the server side";
//		case "INTERNAL_SERVER_ERROR":
//			httpErrorText = inputString + " :: guru meditation! the server encountered an internal error";
//		case "METHOD_FAILURE":
//			httpErrorText = inputString + " :: the invoked method failed, for unknown reasons";
//		case "METHOD_NOT_ALLOWED":
//			httpErrorText = inputString + " :: whoa, wait a minute - you are not allowed to do that on my server";
//		case "NO_CONTENT":
//			httpErrorText = inputString + " :: your request ended in void space - there is not content";
//		case "NOT_ACCEPTABLE":
//			httpErrorText = inputString + " :: this is simply not acceptable - try something else";
//		case "NOT_FOUND":
//			httpErrorText = inputString + " :: the requested object could not be found. To be absolutely clear: This is NOT my fault. Please recheck your query!";
//		case "REQUEST_ENTITY_TOO_LARGE":
//			httpErrorText = inputString + " :: could you phrase your request in a shorter way?";
//		case "REQUEST_TIMEOUT":
//			httpErrorText = inputString + " :: I tried! Really I tried, but my ressources are limited and I could not fullfill your request in time";
//		case "REQUEST_URI_TOO_LONG":
//			httpErrorText = inputString + " :: Hey, I am just a simple machine, and my brain is not big enough to handle such long ressource identifier";
//		case "SERVICE_UNAVAILABLE":
//			httpErrorText = inputString + " :: We are sorry to inform you that the requested service is permanently or at least temporarly closed. Please come back later.";
//		case "UNAUTHORIZED":
//			httpErrorText = inputString + " :: I am perfectly capable of fullfilling your request. BUT YOU ARE NOT AUTHORIZED! so please go ayway.";
//		case "UNPROCESSABLE_ENTITY":
//			httpErrorText = inputString + " :: Whatever it is you wanted from me, I am not able to cope";
//
//		default:
//			httpErrorText = "I have no idea what just happened - let us not try this again, shall we?";
//		}
//
//		return httpErrorText;
//	}


	public static String getHttpErrorText(int errorCode){
		String inputString = HttpStatus.getStatusText(errorCode);
		switch(errorCode) {
		case HttpStatus.SC_BAD_GATEWAY:
			return inputString + " :: the intermediate gateway is wrong - why do we use it anyway?";
		case HttpStatus.SC_BAD_REQUEST:
			return inputString + " :: malformed request sent - come on, you can do better than that";
		case HttpStatus.SC_CONFLICT:
			return inputString + " :: conflicting requests send to server - you guys should play nice together";
		case HttpStatus.SC_FORBIDDEN:
			return inputString + " :: the requested operation is forbidden on this server!";
		case HttpStatus.SC_GATEWAY_TIMEOUT:
			return inputString + " :: the intermediate gateway did not respond in time - maybe it takes a nap";
		case HttpStatus.SC_GONE:
			return inputString + " :: the requested object does no longer exist - did it ever?";
		case HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED:
			return inputString + " :: the request was formatted in an unsupported http version - how can that ever be?";
		case HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE:
			return inputString + " :: ressources are limited and at the moment there simply is not enough (of whatever we needed) for your request";
		case HttpStatus.SC_INSUFFICIENT_STORAGE:
			return inputString + " :: oops, seems like we ran out of diskspace on the server side";
		case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			return inputString + " :: guru meditation! the server encountered an internal error";
		case HttpStatus.SC_METHOD_FAILURE:
			return inputString + " :: the invoked method failed, for unknown reasons";
		case HttpStatus.SC_METHOD_NOT_ALLOWED:
			return inputString + " :: whoa, wait a minute - you are not allowed to do that on my server";
		case HttpStatus.SC_NO_CONTENT:
			return inputString + " :: your request ended in void space - there is not content";
		case HttpStatus.SC_NOT_ACCEPTABLE:
			return inputString + " :: this is simply not acceptable - try something else";
		case HttpStatus.SC_NOT_FOUND:
			return inputString + " :: the requested object could not be found. To be absolutely clear: This is NOT my fault. Please recheck your query!";
		case HttpStatus.SC_REQUEST_TOO_LONG:
			return inputString + " :: could you phrase your request in a shorter way?";
		case HttpStatus.SC_REQUEST_TIMEOUT:
			return inputString + " :: I tried! Really I tried, but my ressources are limited and I could not fullfill your request in time";
		case HttpStatus.SC_REQUEST_URI_TOO_LONG:
			return inputString + " :: Hey, I am just a simple machine, and my brain is not big enough to handle such long ressource identifier";
		case HttpStatus.SC_SERVICE_UNAVAILABLE:
			return inputString + " :: We are sorry to inform you that the requested service is permanently or at least temporarly closed. Please come back later.";
		case HttpStatus.SC_UNAUTHORIZED:
			return inputString + " :: I am perfectly capable of fullfilling your request. BUT YOU ARE NOT AUTHORIZED! so please go ayway.";
		case HttpStatus.SC_UNPROCESSABLE_ENTITY:
			return inputString + " :: Whatever it is you wanted from me, I am not able to cope";
		default:
			return inputString + " :: I have no idea what just happened - let us not try this again, shall we?";
		}
	}
}
