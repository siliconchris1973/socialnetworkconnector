package de.comlineag.snc.helper;

/**
 * 
 * @author		Christian Guenther
 * @category	helper class
 * @version		0.1
 * @status		in development
 * 
 * @description	abstract class every encryption class shall extend. Consumer classes
 * 				that need to encrypt/decrypt a value shall only call for a generic encryptor
 * 				and the actual implementation shall decide which algorithm to use
 * 
 * @changelog	0.1 (Chris)		initial version
 * 
 */
public abstract class GenericEncryptionProvider {

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
	public abstract String decryptValue(String param) throws GenericEncryptionException;

	/**
	 * @description Encrypts a given string 
	 *
	 * @param 		String
	 *					the value to encrypt
	 * @return 		String
	 * 					the return value as encrypted text
	 *
	 */
	public abstract String encryptValue(String param) throws GenericEncryptionException;

	/**
	 * @description Decrypts configuration values 
	 *
	 * @param 		String
	 *					entropy source
	 */
	public abstract void setEntropy(String param);

}