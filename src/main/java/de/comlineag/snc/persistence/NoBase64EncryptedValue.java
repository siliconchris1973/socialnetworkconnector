package de.comlineag.snc.persistence;

/**
 * 
 * @author		Magnus Leinemann
 * @category	Error Handling
 * @version		1.0
 * 
 * @description	Error to show that a value is NOT Base64 coded
 * 
 * @changelog	1.0 initial version
 * 
 */
public class NoBase64EncryptedValue extends Exception {
	
	private static final long serialVersionUID = 6300138787724088083L;
	
	public NoBase64EncryptedValue() {}
	
	public NoBase64EncryptedValue(String s) {
		super(s);
	}
}
