package de.comlineag.snc.helper;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * 
 * @author 		Christian Gunether
 * @category 	helper class
 * @version 	0.2					- 22.10.2014
 * @status		productive
 *
 * @description creates an MD5Hash from a given input string 
 * 
 * @changelog	0.1 (Chris)			class created
 * 				0.2					changed class to create an md5 hash
 *
 * TODO move this in the crypto package, as really MD5 is a crypto provider of sorts
 */
public final class UniqueIdServices {
	public static String createMessageDigest(String inputString) {
		assert (inputString != null) : "ERROR :: cannot operate on empty input";
		String digest = null; 
		try { 
			MessageDigest md = MessageDigest.getInstance("SHA-256"); 
			byte[] hash = md.digest(inputString.getBytes("UTF-8")); //converting byte array to Hexadecimal String 
			StringBuilder sb = new StringBuilder(2*hash.length); 
			for(byte b : hash){
				// we can only use 20 chars - thanks to the stupid hana schema layout
				if (sb.length()<20)
					sb.append(String.format("%02x", b&0xff)); 
			} 
			digest = sb.toString(); 
		} catch (UnsupportedEncodingException ex) { 
			ex.printStackTrace();
		} catch (NoSuchAlgorithmException ex) { 
			ex.printStackTrace(); 
		} 
		return digest;	
	}
	
	
	/**
	 * the class is not to be instantiated
	 */
	private UniqueIdServices() {
		
	}
}
