package de.comlineag.snc.crypto;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

/**
 * 
 * @author		Christian Guenther
 * @category	helper class
 * @version		0.1
 * @status		not implemented
 * 
 * @description	this is the AES (strongest) encryption provider 
 * 
 * 				ATTENTION: NOT YET IMPLEMENTED
 *  
 * @changelog	0.1 (Chris)		initial version
 * 
 */
public class AesCryptoProvider implements ICryptoProvider {
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
		
	// how long must the initial vector be
	int MIN_INITIALVECTOR_SIZE = 256;
		
	/**
	 * @description Decrypts a given string 
	 *
	 * @param 		String
	 *					the value to decrypt
	 * @return 		String
	 * 					the return value as clear text
	 *
	 */
	public String decryptValue(String param) throws GenericCryptoException {
		logger.warn("NOT YET IMPLEMENTED");
		
		return param;
	}

	/**
	 * @description Encrypts a given string 
	 *
	 * @param 		String
	 *					the value to encrypt
	 * @return 		String
	 * 					the return value as encrypted text
	 *
	 */
	public String encryptValue(String param){
		logger.warn("NOT YET IMPLEMENTED");
		
		return param;
	}
}