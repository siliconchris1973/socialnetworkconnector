package de.comlineag.snc.constants;


/**
 *
 * @author 		Christian Guenther
 * @category	enum
 * @version 	0.1
 * @status		productive
 * 
 * @description contains all rsupported encryption provider and a value to indicate the priority
 * 				in which to use these. Priotity is an integer in which the highest number represents 
 * 				the best provider 
 * 
 * @changelog	0.1 (Chris)		class created
 * 
 */
public enum EncryptionProvider {
	NONE 				("None", -1),
	BASE64				("Base64", 1),
	LZW					("LZW", 2);

	private final int priority;
	private final String name;

	private EncryptionProvider(String name, int priority) {
		this.priority = priority;
		this.name = name;

	}

	public int getPriority() {
		return priority;
	}
	public String getName() {
		return name;
	}
	
	public static EncryptionProvider getEncryptionProvider(int provider){
		for (EncryptionProvider code : EncryptionProvider.values()) {
			if(code.getPriority() == provider)
				return code;
		}
		return EncryptionProvider.NONE;
	}
}