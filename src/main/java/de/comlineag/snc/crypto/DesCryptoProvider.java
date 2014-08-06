package de.comlineag.snc.crypto;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.comlineag.snc.constants.CryptoProvider;

/**
 * 
 * @author		Christian Guenther
 * @category	helper class
 * @version		0.3
 * @status		in development
 * 
 * @description	this is the DES, the least secure, encryption provider
 * 
 * @changelog	0.1 (Chris)		initial version
 * 				0.2				added support for initial vector to be taken from applicationContext.xml
 * 				0.3				removed get/setEntropy and deleted code to get initial vector from applicationContext.xml
 * 
 */
public class DesCryptoProvider implements ICryptoProvider {

	byte[] keyBytes;
	byte[] ivBytes;
	
	int dec_len;
	int enc_len;
	
	// the decode returns a byte-Array - this is converted in a string and returned
	byte[] decrypted;
	byte[] encrypted;
	
	// how long must the initial vector be
	int MIN_INITIALVECTOR_SIZE = 64;
	
	// Logger Instanz
	private final Logger logger = LogManager.getLogger(getClass().getName());
	
	
	/**
	 * @description Decrypts a given string 
	 *
	 * @param 		String
	 *					the value to decrypt
	 * @return 		String
	 * 					the return value as clear text
	 *
	 */
	public String decryptValue(String param) throws GenericCryptoException {

		// wrap key data in Key/IV specs to pass to cipher
		SecretKeySpec key = new SecretKeySpec(keyBytes, CryptoProvider.DES.toString());
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
			throw new GenericCryptoException(CryptoProvider.DES, "EXCEPTION :: Parameter " + param + " not DES-encrypted: " + e.getLocalizedMessage());
		} catch (Exception e) {
			throw new GenericCryptoException(CryptoProvider.DES, "EXCEPTION :: unforseen error condition: " + e.getLocalizedMessage());
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
		logger.warn("NOT YET IMPLEMENTED");
		
		// TODO implement encryption method
		return param;
	}
}