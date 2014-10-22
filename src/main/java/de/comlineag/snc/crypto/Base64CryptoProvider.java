package de.comlineag.snc.crypto;

import org.apache.commons.codec.binary.Base64;

import de.comlineag.snc.constants.CryptoProvider;

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
 * 				0.2				added support for GenericCryptoException
 * 
 */
public class Base64CryptoProvider implements ICryptoProvider {

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

		// the decode returns a byte-Array - this is converted in a string and returned
		byte[] base64Array;

		// Check that the returned string is correctly coded as bas64
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new GenericCryptoException(CryptoProvider.BASE64, "EXCEPTION :: Parameter " + param + " not Base64-encrypted: " + e.getLocalizedMessage());
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
	 * @description set the entropy source 
	 *
	 * @param 		String
	 *					entropy source
	 *
	 */
	public void setEntropy(String param){}
	/**
	 * @description get an initial vector
	 * 
	 * @return null
	 * 
	 */
	public String getEntropy(){return null;}
}