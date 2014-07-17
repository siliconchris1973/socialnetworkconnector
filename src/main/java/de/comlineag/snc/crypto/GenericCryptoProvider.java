package de.comlineag.snc.crypto;

import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.constants.CryptoProvider;
import de.comlineag.snc.handler.AppContext;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		0.2
 * @status		deprecated
 * 
 * @description GenericCryptoProvider is the abstract base class for the 
 * 				configuration handler. It gets the active encryption provider 
 * 				(as defined in applicationContext.xml) and passes it along to the 
 * 				actual EncryptionHandler - like ConfigurationEncryption.
 * 
 * @changelog	0.1 (Chris)		first version as copy from GenericDataManager
 * 				0.2				marked deprecated as we now use ConfigurationCryptoHandler directly
 * 								this helps in case we need a second encryption provider for something different
 *
 */
@Deprecated
public abstract class GenericCryptoProvider {
	
	protected ICryptoProvider cryptoProvider;
	protected SocialNetworks sourceSocialNetwork;
	protected CryptoProvider sourceEncryptionProvider;

	protected GenericCryptoProvider() {
		cryptoProvider = (ICryptoProvider) AppContext.Context.getBean("cryptoProvider");
	}
	
	@SuppressWarnings("unused")
	private static String getEncryptionProvider() {
		return (String) AppContext.Context.getBean("cryptoProvider");
	}
}
