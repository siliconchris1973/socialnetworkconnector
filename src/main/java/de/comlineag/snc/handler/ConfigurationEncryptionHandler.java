package de.comlineag.snc.handler;

import org.apache.log4j.Logger;

import de.comlineag.snc.constants.EncryptionProvider;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericEncryptionException;
import de.comlineag.snc.crypto.IEncryptionProvider;
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
public class ConfigurationEncryptionHandler { 
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
		
	protected IEncryptionProvider configurationEncryptionProvider;
	protected SocialNetworks sourceSocialNetwork;							// currently not used
	protected EncryptionProvider sourceEncryptionProvider;					// currently not used
	
	
	public ConfigurationEncryptionHandler() {
		configurationEncryptionProvider = (IEncryptionProvider) AppContext.Context.getBean("configurationEncryptionProvider");
	}
	
	@SuppressWarnings("unused")
	private static String getEncryptionProvider() {
		return (String) AppContext.Context.getBean("configurationEncryptionProvider");
	}
	
	
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
		if (param.length()>20)
			logger.trace("decrypting "+param.substring(0, 20)+"... via " + configurationEncryptionProvider.getClass().getSimpleName());
		else
			logger.trace("decrypting "+param+" via " + configurationEncryptionProvider.getClass().getSimpleName());
		return configurationEncryptionProvider.decryptValue(param);
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
		if (param.length()>20)
			logger.trace("encrypting "+param.substring(0, 20)+"... via " + configurationEncryptionProvider.getClass().getSimpleName());
		else
			logger.trace("encrypting "+param+" via " + configurationEncryptionProvider.getClass().getSimpleName());
		return configurationEncryptionProvider.encryptValue(param);
	}
}
