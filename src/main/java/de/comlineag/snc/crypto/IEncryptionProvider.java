package de.comlineag.snc.crypto;

/**
 * 
 * @author		Christian Guenther
 * @category	interface
 * @version		0.2
 * @status		in development
 * 
 * @description	interface class every encryption class shall implement. Consumer classes
 * 				that need to encrypt/decrypt a value only call for a generic encryptionProvider
 * 				and the actual implementation is decided based on applicationContext.xml 
 * 
 * @changelog	0.1 (Chris)		initial version
 * 				0.2				changed from abstract class to interface
 * 
 */
public interface IEncryptionProvider {

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