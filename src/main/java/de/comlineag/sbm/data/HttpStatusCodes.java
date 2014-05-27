package de.comlineag.sbm.data;


/**
 * 
 * @author Christian Guenther
 * @description contains all relevant HTTP Status Codes
 * 				provides a query to see if a given http response code is good, as in OK, Accepted or Created
 * 				or if it is bad, as in Rejected, Bad Request etc.
 */
public enum HttpStatusCodes {
	SC_ACCEPTED							("OK"),			// 202
	SC_BAD_GATEWAY						("NOK"), 		// 502
	SC_BAD_REQUEST						("NOK"), 		// 400
	SC_CONFLICT							("NOK"), 		// 409
	SC_CREATED							("OK"), 		// 201
	SC_CONTINUE							("OK"), 		// 100
	SC_FOUND							("OK"), 		// 302
	SC_FORBIDDEN						("NOK"), 		// 403
	SC_GATEWAY_TIMEOUT					("NOK"), 		// 504
	SC_GONE								("NOK"), 		// 410
	SC_HTTP_VERSION_NOT_SUPPORTED		("NOK"), 		// 505
	SC_INSUFFICIENT_SPACE_ON_RESOURCE	("NOK"),		// 419
	SC_INSUFFICIENT_STORAGE				("NOK"), 		// 507
	SC_INTERNAL_SERVER_ERROR			("NOK"), 		// 500
	SC_METHOD_FAILURE					("NOK"),		// 420
	SC_METHOD_NOT_ALLOWED				("NOK"), 		// 405
	SC_NO_CONTENT						("NOK"), 		// 204
	SC_NOT_ACCEPTABLE					("NOK"), 		// 406
	SC_NOT_FOUND						("NOK"),		// 404
	SC_OK								("OK"), 		// 200
	SC_REQUEST_ENTITY_TOO_LARGE			("NOK"), 		// 413
	SC_REQUEST_TIMEOUT					("NOK"), 		// 408
	SC_REQUEST_URI_TOO_LONG				("NOK"),		// 414
	SC_SERVICE_UNAVAILABLE				("NOK"), 		// 503
	SC_UNAUTHORIZED						("NOK"), 		// 410
	SC_UNPROCESSABLE_ENTITY				("NOK"),		// 422
	// just repeating without SC_ in front because I'm not sure how codes are returned
	ACCEPTED							("OK"),			// 202
	BAD_GATEWAY							("NOK"), 		// 502
	BAD_REQUEST							("NOK"), 		// 400
	CONFLICT							("NOK"), 		// 409
	CREATED								("OK"), 		// 201
	CONTINUE							("OK"), 		// 100
	FOUND								("OK"), 		// 302
	FORBIDDEN							("NOK"), 		// 403
	GATEWAY_TIMEOUT						("NOK"), 		// 504
	GONE								("NOK"), 		// 410
	HTTP_VERSION_NOT_SUPPORTED			("NOK"), 		// 505
	INSUFFICIENT_SPACE_ON_RESOURCE		("NOK"),		// 419
	INSUFFICIENT_STORAGE				("NOK"), 		// 507
	INTERNAL_SERVER_ERROR				("NOK"), 		// 500
	METHOD_FAILURE						("NOK"),		// 420
	METHOD_NOT_ALLOWED					("NOK"), 		// 405
	NO_CONTENT							("NOK"), 		// 204
	NOT_ACCEPTABLE						("NOK"), 		// 406
	NOT_FOUND							("NOK"),		// 404
	OK									("OK"), 		// 200
	REQUEST_ENTITY_TOO_LARGE			("NOK"), 		// 413
	REQUEST_TIMEOUT						("NOK"), 		// 408
	REQUEST_URI_TOO_LONG				("NOK"),		// 414
	SERVICE_UNAVAILABLE					("NOK"), 		// 503
	UNAUTHORIZED						("NOK"), 		// 410
	UNPROCESSABLE_ENTITY				("NOK");		// 422
	
	private final String value;
	private final String name;       

    public boolean equalsName(String otherName){
        return (otherName == null)? false:name.equals(otherName);
    }

    public String toStringLiteral(){
       return name;
    }
	
	private HttpStatusCodes(String value, String name) {
		this.value = value;
		this.name = name;
	}
	
	//TODO this error needs to be checked 
	private HttpStatusCodes(String value) {
		this.value = value;
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

}

/* this one WOULD also contain the actual return code - but it does NOT work somehow
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
	
	
	// 
	// @param Status
	// @return OK or NOK
	// @description return if status code is ok (OK) or not ok (NOK)
	// 
	//
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
*/

