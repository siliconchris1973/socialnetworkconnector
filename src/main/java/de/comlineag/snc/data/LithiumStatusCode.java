package de.comlineag.snc.data;

/**
 *
 * @author 		Christian Guenther
 * @category	enum
 * @version 	1.0
 * 
 * @description contains relevant Lithium Status Codes
 * 				provides a query to see if a given response code is good, as in OK, Accepted or Success
 * 				or if it is bad, as in Rejected, Bad Request etc.
 * 
 * @changelog	1.0 enum created
 * 
 */
public enum LithiumStatusCode {
	UNKNOWN 						("unknown"										, false),
	FORBIDDEN						("forbidden"									, false),
	SUCCESS							("success"										, true);
	//UNKNOWN 						("UNKNOWN"										, false),
	//FORBIDDEN						("FORBIDDEN"									, false),
	//SUCCESS							("SUCCESS"										, true);
	

	private String value;
	private boolean isOk;

	private LithiumStatusCode(String value, boolean isOk) {
		this.value = value;
		this.isOk = isOk;
	}

	public boolean isOk() {
		return isOk;
	}

	public String getValue(){
		return value;
	}

	@Override
	public String toString() {
		return getValue();
	}
	
	public boolean isEqual(String errorCode){
		return this.value.equals(errorCode);
	}
	
	public static LithiumStatusCode getLithiumStatusCode(String errorCode){
		for (LithiumStatusCode code : values()) {
			if(code.value.equals(errorCode))
				return code;
		}
		return LithiumStatusCode.UNKNOWN;
	}
}