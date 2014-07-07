package de.comlineag.snc.helper;

/**
 * 
 * @author		Magnus Leinemann
 * @category	Error Handling
 * @version		0.1
 * @status		productive
 * 
 * @description	Error to show that a value is NOT Base64 coded
 * 
 * @changelog	0.1 (Magnus)		initial version
 * 
 */
public class NoBase64EncryptedValue extends Exception {
	
	private static final long serialVersionUID = 6300138787724088083L;
	
	public NoBase64EncryptedValue() {}
	
	public NoBase64EncryptedValue(String s) {
		super(s);
	}
}
