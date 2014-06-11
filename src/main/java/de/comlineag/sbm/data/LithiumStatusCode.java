package de.comlineag.sbm.data;

/**
 *
 * @author 	Christian Guenther
 * @description contains relevant Lithium Status Codes
 * 				provides a query to see if a given response code is good, as in OK, Accepted or Success
 * 				or if it is bad, as in Rejected, Bad Request etc.
 */
public enum LithiumStatusCode {
	UNKNOWN 						("unknown", false),
	SUCCESS							("success", true);
	
	

	private final String value;
	private final boolean name;

	private LithiumStatusCode(String value, boolean name) {
		this.value = value;
		this.name = name;

	}

	public boolean isOk() {
		return name;
	}

	public String getErrorCode(){
		return value;
	}

	/*
	public static LithiumStatusCode getHttpStatusCode(int errorCode){
		for (LithiumStatusCode code : LithiumStatusCode.values()) {
			if(code.getErrorCode() == errorCode)
				return code;
		}
		return LithiumStatusCode.UNKNOWN;
	}
	*/
}