package de.comlineag.snc.helper;

import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.EncryptionProvider;
import de.comlineag.snc.persistence.AppContext;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		0.1
 * @status		productive
 * 
 * @description GenericEncryptionProvider is the abstract base class for the 
 * 				configuration handler. It gets the active encryption provider 
 * 				(as defined in applicationContext.xml) and passes it along to the 
 * 				actual EncryptionHandler - like ConfigurationEncryption.
 * 
 * @changelog	0.1 (Chris)		first version as copy from GenericDataManager
 *
 */
public abstract class GenericEncryptionProvider {
	
	protected IEncryptionProvider encryptionProvider;
	protected SocialNetworks sourceSocialNetwork;
	protected EncryptionProvider sourceEncryptionProvider;

	protected GenericEncryptionProvider() {
		encryptionProvider = (IEncryptionProvider) AppContext.Context.getBean("encryptionProvider");
	}
	
	@SuppressWarnings("unused")
	private static String getEncryptionProvider() {
		return (String) AppContext.Context.getBean("encryptionProvider");
	}
}
