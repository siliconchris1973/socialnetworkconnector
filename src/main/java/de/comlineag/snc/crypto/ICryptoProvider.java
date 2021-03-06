package de.comlineag.snc.crypto;

/**
 * 
 * @author		Christian Guenther
 * @category	interface
 * @version		0.3
 * @status		productive
 * 
 * @description	interface class every encryption class shall implement. Consumer classes
 * 				that need to encrypt/decrypt a value only call for a generic cryptoProvider
 * 				and the actual implementation is decided based on applicationContext.xml 
 * 
 * @changelog	0.1 (Chris)		initial version
 * 				0.2				changed from abstract class to interface
 * 				0.3				deleted set/getEntropy from interface
 * 
 */
public interface ICryptoProvider {
	
	/**
	 * @description Decrypts a given string 
	 *
	 * @param 		String
	 *					the value to decrypt
	 * @return 		String
	 * 					the return value as clear text
	 * @throws GenericCryptoException 
	 *
	 */
	public abstract String decryptValue(String param) throws GenericCryptoException;
	
	/**
	 * @description Encrypts a given string 
	 *
	 * @param 		String
	 *					the value to encrypt
	 * @return 		String
	 * 					the return value as encrypted text
	 *
	 */
	public abstract String encryptValue(String param) throws GenericCryptoException;
}