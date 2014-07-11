package de.comlineag.snc.crypto;

import de.comlineag.snc.constants.CryptoProvider;

/**
 * 
 * @author		Magnus Leinemann, Christian Guenther
 * @category	Error Handling
 * @version		0.3
 * @status		productive
 * 
 * @description	Error to show that a value is NOT encrypted coded
 * 
 * @changelog	0.1 (Magnus)		initial version
 * 				0.2 (Chris)			renamed to GenericCryptoException
 * 				0.3					added support for encryption providers to the super call
 * 
 */
public class GenericCryptoException extends Exception {
	
	private static final long serialVersionUID = 6300138787724088083L;
	
	public GenericCryptoException() {}
	
	public GenericCryptoException(CryptoProvider provider, String s) {
		super("The provider was unable to en/decrypt the value " + s);
	}
}
