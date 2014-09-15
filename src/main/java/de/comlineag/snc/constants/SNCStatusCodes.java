package de.comlineag.snc.constants;

/**
 *
 * @author 		Christian Guenther
 * @category	enum
 * @version 	0.1
 * @status		productive
 * 
 * @description contains all relevant Status Codes for teh SNC
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */
public enum SNCStatusCodes {
	// CODE				VALUE			OK to continue
		UNKNOWN				(0				, false),
		OK					(1				, true),		// 
		WARN				(-1				, true),		//
		ERROR				(-2				, false),		//
		CRITICAL			(-4				, false),		//
		FATAL				(-8				, false);		//

	private final int value;
	private final boolean name;

	private SNCStatusCodes(int value, boolean name) {
		this.value = value;
		this.name = name;

	}

	public boolean isOk() {
		return name;
	}

	public int getErrorCode(){
		return value;
	}	
}