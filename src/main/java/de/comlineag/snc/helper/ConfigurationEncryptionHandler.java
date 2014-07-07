package de.comlineag.snc.helper;

import org.apache.log4j.Logger;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		0.1
 * @status		productive
 * 
 * @description	invokes the encryption provider as defined in applicationContext.xml
 * 
 * @param <T>
 * 
 * @changelog	0.1 first initial version 
 * 
 */
public class ConfigurationEncryptionHandler extends GenericEncryptionProvider {
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	/**
	 * @description Decrypts a given string 
	 *
	 * @param 		String
	 *					the value to decrypt
	 * @return 		String
	 * 					the return value as clear text
	 * @throws GenericEncryptionException 
	 *
	 */
	public String decryptValue(String param) throws GenericEncryptionException {
		logger.debug("using " + encryptionProvider.getClass().getCanonicalName().toString() + " as encryption provider");
		return encryptionProvider.decryptValue(param);
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
	public String encryptValue(String param) throws GenericEncryptionException {
		return encryptionProvider.encryptValue(param);
	}

	/**
	 * @description Decrypts configuration values 
	 *
	 * @param 		String
	 *					entropy source
	 */
	public void setEntropy(String param) {
		encryptionProvider.setEntropy(param);
	}
}
