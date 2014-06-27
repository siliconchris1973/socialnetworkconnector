package de.comlineag.snc.data;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;

/**
 *
 * @author 		Christian Guenther and Maic Rittmeier
 * @category	enum
 * @version 	1.0
 * 
 * @description contains all relevant HTTP Status Codes
 * 				provides a query to see if a given http response code is good, as in OK, Accepted or Created
 * 				or if it is bad, as in Rejected, Bad Request etc.
 * 
 * @changelog	1.0 class created
 * 
 */
public enum HttpStatusCode {
	UNKNOWN 						(-1												, false),
	ACCEPTED						(HttpStatus.SC_ACCEPTED							, true),		// 202
	BAD_GATEWAY						(HttpStatus.SC_BAD_GATEWAY						, false), 		// 502
	BAD_REQUEST						(HttpStatus.SC_BAD_REQUEST						, false), 		// 400
	CONFLICT						(HttpStatus.SC_CONFLICT							, false), 		// 409
	CREATED							(HttpStatus.SC_CREATED							, true), 		// 201
	CONTINUE						(HttpStatus.SC_CONTINUE							, true), 		// 100
	MOVED_TEMPORARILY				(HttpStatus.SC_MOVED_TEMPORARILY				, false), 		// 302
	FORBIDDEN						(HttpStatus.SC_FORBIDDEN						, false), 		// 403
	GATEWAY_TIMEOUT					(HttpStatus.SC_GATEWAY_TIMEOUT					, false), 		// 504
	GONE							(HttpStatus.SC_GONE								, false), 		// 410
	HTTP_VERSION_NOT_SUPPORTED		(HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED		, false), 		// 505
	INSUFFICIENT_SPACE_ON_RESOURCE	(HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE	, false),		// 419
	INSUFFICIENT_STORAGE			(HttpStatus.SC_INSUFFICIENT_STORAGE				, false), 		// 507
	INTERNAL_SERVER_ERROR			(HttpStatus.SC_INTERNAL_SERVER_ERROR			, false), 		// 500
	METHOD_FAILURE					(HttpStatus.SC_METHOD_FAILURE					, false),		// 420
	METHOD_NOT_ALLOWED				(HttpStatus.SC_METHOD_NOT_ALLOWED				, false), 		// 405
	NO_CONTENT						(HttpStatus.SC_NO_CONTENT						, false), 		// 204
	NOT_ACCEPTABLE					(HttpStatus.SC_NOT_ACCEPTABLE					, false), 		// 406
	NOT_FOUND						(HttpStatus.SC_NOT_FOUND						, false),		// 404
	NOT_IMPLEMENTED					(HttpStatus.SC_NOT_IMPLEMENTED					, false),		// 501
	OK								(HttpStatus.SC_OK								, true), 		// 200
	REQUEST_TOO_LONG				(HttpStatus.SC_REQUEST_TOO_LONG					, false), 		// 413
	REQUEST_TIMEOUT					(HttpStatus.SC_REQUEST_TIMEOUT					, false), 		// 408
	REQUEST_URI_TOO_LONG			(HttpStatus.SC_REQUEST_URI_TOO_LONG				, false),		// 414
	SERVICE_UNAVAILABLE				(HttpStatus.SC_SERVICE_UNAVAILABLE				, false), 		// 503
	UNAUTHORIZED					(HttpStatus.SC_UNAUTHORIZED						, false), 		// 410
	UNPROCESSABLE_ENTITY			(HttpStatus.SC_UNPROCESSABLE_ENTITY				, false);		// 422

	private final int value;
	private final boolean name;

	private HttpStatusCode(int value, boolean name) {
		this.value = value;
		this.name = name;

	}

	public boolean isOk() {
		return name;
	}

	public int getErrorCode(){
		return value;
	}

	public static HttpStatusCode getHttpStatusCode(int errorCode){
		for (HttpStatusCode code : HttpStatusCode.values()) {
			if(code.getErrorCode() == errorCode)
				return code;
		}
		return HttpStatusCode.UNKNOWN;
	}

	public static HttpStatusCode getHttpStatusCode(HttpResponse execute) {
		System.out.println("all my headers: " + execute.getAllHeaders().toString());
		return HttpStatusCode.UNKNOWN;
	}
}