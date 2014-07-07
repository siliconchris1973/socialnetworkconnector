package de.comlineag.snc.handler;

import org.apache.log4j.Logger;

import de.comlineag.snc.constants.EncryptionProvider;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.helper.GenericEncryptionException;
import de.comlineag.snc.helper.IEncryptionProvider;
import de.comlineag.snc.persistence.AppContext;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @version		0.1
 * @status		productive
 * 
 * @description	invokes the encryption provider as defined in applicationContext.xml
 * 
 * @changelog	0.1 first initial version 
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
		return (String) AppContext.Context.getBean("encryptionProvider");
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
		logger.trace("decrypting "+param+" via " + configurationEncryptionProvider.getClass().getCanonicalName().toString());
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
		logger.trace("encrypting "+param+" via " + configurationEncryptionProvider.getClass().getCanonicalName().toString());
		return configurationEncryptionProvider.encryptValue(param);
	}

	/**
	 * @description feed entropy data to the encryption provider
	 * 				this can be a normal string or also a file vector passed as a string
	 * 				whatever is used, is hidden from the handler as it is part of the 
	 * 				implementation of the actual encryption provider 
	 *
	 * @param 		String
	 *					entropy source
	 */
	public void setEntropy(String param) {
		configurationEncryptionProvider.setEntropy(param);
	}
}
