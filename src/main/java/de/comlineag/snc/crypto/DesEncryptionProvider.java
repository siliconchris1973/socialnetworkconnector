package de.comlineag.snc.crypto;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import de.comlineag.snc.constants.EncryptionProvider;

/**
 * 
 * @author		Christian Guenther
 * @category	helper class
 * @version		0.1
 * @status		in development
 * 
 * @description	this is the DES, the least secure, encryption provider
 * 
 * @changelog	0.1 (Chris)		initial version
 * 				0.2				added support for initial vector to be taken from applicationContext.xml
 * 
 */
public class DesEncryptionProvider implements IEncryptionProvider {

	byte[] keyBytes;
	byte[] ivBytes;
	int dec_len;
	int enc_len;
	// the decode returns a byte-Array - this is converted in a string and returned
	byte[] decrypted;
	byte[] encrypted;
	
	// how long must the initial vector be
	int MIN_INITIALVECTOR_SIZE = 8;
	
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	
	/**
	 * @description Decrypts a given string 
	 *
	 * @param 		String
	 *					the value to decrypt
	 * @return 		String
	 * 					the return value as clear text
	 *
	 */
	public String decryptValue(String param) throws GenericEncryptionException {

		// wrap key data in Key/IV specs to pass to cipher
		SecretKeySpec key = new SecretKeySpec(keyBytes, EncryptionProvider.DES.toString());
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		// create the cipher with the algorithm you choose
		// see javadoc for Cipher class for more info
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			decrypted = new byte[cipher.getOutputSize(enc_len)];
			int dec_len = cipher.update(encrypted, 0, enc_len, decrypted, 0);
			dec_len += cipher.doFinal(decrypted, dec_len);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
			throw new GenericEncryptionException(EncryptionProvider.DES, "EXCEPTION :: Parameter " + param + " not DES-encrypted: " + e.getLocalizedMessage());
		} catch (Exception e) {
			throw new GenericEncryptionException(EncryptionProvider.DES, "EXCEPTION :: unforseen error condition: " + e.getLocalizedMessage());
		}
		return new String(decrypted);
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
	public String encryptValue(String param){
		// TODO implement encryption method
		return param;
	}

	/**
	 * @description set the entropy source 
	 *
	 * @param 		String
	 *					entropy source
	 */
	public void setEntropy(String param){
		if (param != null && param.length()>MIN_INITIALVECTOR_SIZE) {
			logger.trace("using provided initial vector");
			// convert passed string to ivBytes[]
		} else {
			logger.trace("using initial vector from applicationContext.xml");
			// TODO get initial vector form applicationContext.xml
		}
	}
	
	/**
	 * @description get an initial vector
	 */
	public String getEntropy(){
		return null;
	}
}