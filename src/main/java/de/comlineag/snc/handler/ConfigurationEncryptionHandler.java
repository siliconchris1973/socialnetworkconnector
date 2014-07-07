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
 * @param <T>
 * 
 * @changelog	0.1 first initial version 
 * 
 */
public class ConfigurationEncryptionHandler { //extends GenericEncryptionProvider {
		
	protected IEncryptionProvider configurationEncryptionProvider;
	protected SocialNetworks sourceSocialNetwork;
	protected EncryptionProvider sourceEncryptionProvider;

	
	
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
		return configurationEncryptionProvider.encryptValue(param);
	}

	/**
	 * @description Decrypts configuration values 
	 *
	 * @param 		String
	 *					entropy source
	 */
	public void setEntropy(String param) {
		configurationEncryptionProvider.setEntropy(param);
	}
}
