package de.comlineag.snc.handler;

import org.apache.log4j.Logger;

import de.comlineag.snc.constants.CryptoProvider;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.crypto.ICryptoProvider;
import de.comlineag.snc.persistence.AppContext;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		0.2
 * @status		productive
 * 
 * @description	invokes the encryption provider as defined in applicationContext.xml
 * 
 * @changelog	0.1 (Chris)		first initial version
 * 				0.2				deleted methods for set/getEntropy
 * 
 */
public class DataCryptoHandler { 
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
		
	protected ICryptoProvider dataEncryptionProvider;
	protected SocialNetworks sourceSocialNetwork;							// currently not used
	protected CryptoProvider sourceEncryptionProvider;					// currently not used
	
	
	public DataCryptoHandler() {
		dataEncryptionProvider = (ICryptoProvider) AppContext.Context.getBean("dataEncryptionProvider");
	}
	
	@SuppressWarnings("unused")
	private static String getEncryptionProvider() {
		return (String) AppContext.Context.getBean("dataEncryptionProvider");
	}
	
	
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
	public String decryptValue(String param) throws GenericCryptoException {
		if (param == null)
			return null;
		
		if (param.length()>20)
			logger.trace("in-stream decrypting \""+param.substring(0, 20)+"...\" via " + dataEncryptionProvider.getClass().getSimpleName());
		else
			logger.trace("in-stream decrypting \""+param+"\" via " + dataEncryptionProvider.getClass().getSimpleName());
		return dataEncryptionProvider.decryptValue(param);
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
	public String encryptValue(String param) throws GenericCryptoException {
		if (param == null)
			return null;
		
		if (param.length()>20)
			logger.trace("in-stream encrypting \""+param.substring(0, 20)+"...\" via " + dataEncryptionProvider.getClass().getSimpleName());
		else
			logger.trace("in-stream encrypting \""+param+"\" via " + dataEncryptionProvider.getClass().getSimpleName());
		return dataEncryptionProvider.encryptValue(param);
	}
}