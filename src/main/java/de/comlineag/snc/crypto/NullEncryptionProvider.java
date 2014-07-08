package de.comlineag.snc.crypto;

/**
 * 
 * @author		Christian Guenther
 * @category	helper class
 * @version		0.1
 * @status		productive
 * 
 * @description	this is the null encryption provider it just passes everything through without modification
 *  
 * @changelog	0.1 (Chris)		initial version
 * 
 */
public class NullEncryptionProvider implements IEncryptionProvider {

	/**
	 * @description just returns a given string 
	 *
	 * @param 		String
	 * @return 		String
	 *
	 */
	public String decryptValue(String param) throws GenericEncryptionException {
		return param;
	}

	/**
	 * @description just returns a given string 
	 *
	 * @param 		String
	 * @return 		String
	 *
	 */
	public String encryptValue(String param){
		return param;
	}

	/**
	 * @description does nothing
	 *
	 * @param 		String
	 * 
	 */
	public void setEntropy(String param){
		// NOT NEEDED AND NOT IMPLEMENTED
	}
	/**
	 * @description returns null
	 * 
	 * @return null
	 * 
	 */
	public String getEntropy(){
		return null;
	}
}