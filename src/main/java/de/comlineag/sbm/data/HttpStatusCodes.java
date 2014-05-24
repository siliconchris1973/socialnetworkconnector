package de.comlineag.sbm.data;

/**
 * 
 * @author Christian Guenther
 * @description contains all relevant HTTP Status Codes
 * 				provides a query to see if a given http response code is good, as in OK, Accepted or Created
 * 				or if it is bad, as in Rejected, Bad Request etc.
 */
public enum HttpStatusCodes {
	SC_ACCEPTED							("OK", 200),		// 202
	SC_BAD_GATEWAY						("NOK", 502), 		// 502
	SC_BAD_REQUEST						("NOK", 400), 		// 400
	SC_CONFLICT							("NOK", 409), 		// 409
	SC_CREATED							("OK", 201), 		// 201
	SC_CONTINUE							("OK", 100), 		// 100
	SC_FOUND							("OK", 302), 		// 302
	SC_FORBIDDEN						("NOK", 403), 		// 403
	SC_GATEWAY_TIMEOUT					("NOK", 504), 		// 504
	SC_GONE								("NOK", 410), 		// 410
	SC_HTTP_VERSION_NOT_SUPPORTED		("NOK", 505), 		// 505
	SC_INSUFFICIENT_SPACE_ON_RESOURCE	("NOK", 419),		// 419
	SC_INSUFFICIENT_STORAGE				("NOK", 507), 		// 507
	SC_INTERNAL_SERVER_ERROR			("NOK", 500), 		// 500
	SC_METHOD_FAILURE					("NOK", 420),		// 420
	SC_METHOD_NOT_ALLOWED				("NOK", 405), 		// 405
	SC_NO_CONTENT						("NOK", 204), 		// 204
	SC_NOT_ACCEPTABLE					("NOK", 406), 		// 406
	SC_NOT_FOUND						("NOK", 404),		// 404
	SC_OK								("OK", 200), 		// 200
	SC_REQUEST_ENTITY_TOO_LARGE			("NOK", 413), 		// 413
	SC_REQUEST_TIMEOUT					("NOK", 408), 		// 408
	SC_REQUEST_URI_TOO_LONG				("NOK", 414),		// 414
	SC_SERVICE_UNAVAILABLE				("NOK", 503), 		// 503
	SC_UNAUTHORIZED						("NOK", 410), 		// 410
	SC_UNPROCESSABLE_ENTITY				("NOK", 422);		// 422
	
	private final String value;
	private final int code;
	
	private HttpStatusCodes(final String value, final int code) {
		this.value = value;
		this.code = code;
	}
	
	/**
	 * 
	 * @param Status
	 * @return OK or NOK
	 * @description return if status code is ok (OK) or not ok (NOK)
	 * 
	 */
	public String getValue() {
		return value;
	}
	public String toString() {
		return getValue();
	}
	
	public int getCode(){
		return code;
	}
}



