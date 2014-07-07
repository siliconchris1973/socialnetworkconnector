package de.comlineag.snc.helper;

import org.apache.commons.codec.binary.Base64;

import de.comlineag.snc.constants.EncryptionProvider;

/**
 * 
 * @author		Christian Guenther
 * @category	helper class
 * @version		0.2
 * @status		in development
 * 
 * @description	this is the Base64, the least secure, encryption provider
 * 
 * @changelog	0.1 (Chris)		initial version
 * 				0.2				added support for GenericEncryptionException
 * 
 */
public class Base64EncryptionProvider implements IEncryptionProvider {

	/**
	 * @description Decrypts a given string 
	 *
	 * @param 		String
	 *					the value to decrypt
	 * @return 		String
	 * 					the return value as clear text
	 *
	 */
	public String decryptValue(String param) throws GenericEncryptionException {

		// the decode returns a byte-Array - this is converted in a string and returned
		byte[] base64Array;

		// Check that the returned string is correctly coded as bas64
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new GenericEncryptionException(EncryptionProvider.BASE64, "EXCEPTION :: Parameter " + param + " not Base64-encrypted: " + e.getLocalizedMessage());
		}
		return new String(base64Array);
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
		// TODO implement encryption method
		return param;
	}

	/**
	 * @description Decrypts configuration values 
	 *
	 * @param 		String
	 *					entropy source
	 */
	public void setEntropy(String param){
		// NOT YET IMPLEMENTED
	}

}