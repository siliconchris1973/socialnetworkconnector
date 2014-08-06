package de.comlineag.snc.handler;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.comlineag.snc.appstate.AppContext;
import de.comlineag.snc.constants.CryptoProvider;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.crypto.ICryptoProvider;

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
public class ConfigurationCryptoHandler { 
	
	private final Logger logger = LogManager.getLogger(getClass().getName());
		
	protected ICryptoProvider configurationCryptoProvider;
	protected SocialNetworks sourceSocialNetwork;							// currently not used
	protected CryptoProvider sourceEncryptionProvider;					// currently not used
	
	
	public ConfigurationCryptoHandler() {
		configurationCryptoProvider = (ICryptoProvider) AppContext.Context.getBean("configurationCryptoProvider");
	}
	
	public String getCryptoProviderName() {
		return (String) AppContext.Context.getBean("configurationCryptoProvider").getClass().getSimpleName();
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
		if (param.length()>20)
			logger.trace("decrypting "+param.substring(0, 20)+"... via " + configurationCryptoProvider.getClass().getSimpleName());
		else
			logger.trace("decrypting "+param+" via " + configurationCryptoProvider.getClass().getSimpleName());
		return configurationCryptoProvider.decryptValue(param);
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
		if (param.length()>20)
			logger.trace("encrypting "+param.substring(0, 20)+"... via " + configurationCryptoProvider.getClass().getSimpleName());
		else
			logger.trace("encrypting "+param+" via " + configurationCryptoProvider.getClass().getSimpleName());
		return configurationCryptoProvider.encryptValue(param);
	}
}
