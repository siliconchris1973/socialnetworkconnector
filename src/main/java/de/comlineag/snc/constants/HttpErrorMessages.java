package de.comlineag.snc.constants;

import org.apache.commons.httpclient.HttpStatus;

/**
 * 
 * @author 		Christian Guenther
 * @category	helper class
 * @version 	0.2
 * 
 * @description	provides for some funny error messages to http errors
 *
 * @changelog	0.1 class created
 * 				0.2 changed text elements
 * 
 */
public class HttpErrorMessages { // extends Exception {
	
	// simple static text function to return funny messages on http errors
	public static String getHttpErrorText(int errorCode){
		String inputString = HttpStatus.getStatusText(errorCode);
		switch(errorCode) {
		case HttpStatus.SC_ACCEPTED:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: look honey; if I say ACCEPTED, that means I already accepted your request. So there really is no need to look for an error here.";
		case HttpStatus.SC_OK:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: Hey, what do you want from me? Ok is Ok. There is no error here. Go stick your nose someplace else.";
		case HttpStatus.SC_BAD_GATEWAY:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: the intermediate gateway you choose is the completely wrong guy for your request - why don\'t you try a different one?";
		case HttpStatus.SC_BAD_REQUEST:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: malformed request sent - come on, I\'m sure you can do better than that.";
		case HttpStatus.SC_CONFLICT:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: conflicting requests send to server - you guys should play nice together.";
		case HttpStatus.SC_CONTINUE:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: you may continue doing, whatever it is that pleases you.";
		case HttpStatus.SC_CREATED:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: well done, well done - the object was created.";
		case HttpStatus.SC_FORBIDDEN:
			return inputString.toUpperCase().replaceAll(" ", "_") 
					+ " :: the requested operation is forbidden on this server!";
		case HttpStatus.SC_GATEWAY_TIMEOUT:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the intermediate gateway did not respond in time - maybe it takes a nap?";
		case HttpStatus.SC_GONE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the requested object does no longer exist - did it ever?";
		case HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the request was formatted in an unsupported http version - how can that ever be?";
		case HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: ressources are limited and at the moment there simply is not enough (of whatever we needed) for your request.";
		case HttpStatus.SC_INSUFFICIENT_STORAGE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: oops, seems like we ran out of diskspace on the server side. Gosh, storage is so boring.";
		case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: guru meditation! The server encountered an internal error and I\'m giving up.";
		case HttpStatus.SC_METHOD_FAILURE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the invoked method failed, for unknown reasons. Maybe there is more knowledge in the log files, but I fear you need to enable trace or debug mode.";
		case HttpStatus.SC_MOVED_TEMPORARILY:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: look, the object you requested is not here, ok? So could you please be so kind and also just move on?";
		case HttpStatus.SC_METHOD_NOT_ALLOWED:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: whoa, wait a minute - you are not allowed to do that on my server! Go away!";
		case HttpStatus.SC_NO_CONTENT:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: your request ended in void space - there is not content here.";
		case HttpStatus.SC_NOT_ACCEPTABLE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: this is simply not acceptable - try something else!";
		case HttpStatus.SC_NOT_FOUND:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the requested object could not be found. To be absolutely clear: This is NOT my fault. Please recheck your query!";
		case HttpStatus.SC_NOT_IMPLEMENTED:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: the path you gave me, leads to nowhere. Please recheck your query!";
		case HttpStatus.SC_REQUEST_TOO_LONG:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: could you phrase your request in a shorter way?";
		case HttpStatus.SC_REQUEST_TIMEOUT:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: I tried! Really I tried, but my ressources are limited and I could not fullfill your request in time.";
		case HttpStatus.SC_REQUEST_URI_TOO_LONG:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: Hey, I am just a simple machine, and my brain is not big enough to handle such long ressource identifier.";
		case HttpStatus.SC_SERVICE_UNAVAILABLE:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: We are sorry to inform you that the requested service is permanently or at least temporarly closed. Please come back later.";
		case HttpStatus.SC_UNAUTHORIZED:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: I am perfectly capable of fullfilling your request. BUT YOU ARE NOT AUTHORIZED! So please go ayway.";
		case HttpStatus.SC_UNPROCESSABLE_ENTITY:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: Whatever it is you wanted from me, I am not able to cope, because the entity is simply not processable.";
		default:
			return inputString.toUpperCase().replaceAll(" ", "_")
					+ " :: I have no idea what just happened, could be good, could be bad. I don\'t know, so let\'s not try this again, shall we?";
		}
	}
}
