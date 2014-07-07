package de.comlineag.snc.constants;


/**
 *
 * @author 		Christian Guenther
 * @category	enum
 * @version 	0.2
 * @status		productive
 * 
 * @description contains all supported encryption provider, a value to indicate the grade and one for 
 * 				the strength of the encryption (both correspond).
 * 				
 * @changelog	0.1 (Chris)		class created
 * 				0.2				added support to query for the strength of an encryption provider
 * 								and support to determine which encryption algorithm provides the desired grade  
 * 
 */
public enum EncryptionProvider {
//	enum code			name			int / String encryptionGrade/Strength	
	NONE 				("None", 		0,	"none"),
	BASE64				("Base64", 		1,	"low"),
	DES					("DES", 		2,	"medium"),
	DES3				("Triple DES", 	3,	"good"),
	AES					("AES", 		4,	"best");

	private final String name;
	private final int encryptionGrade;
	private final String encryptionStrength;

	private EncryptionProvider(String name, int encryptionGrade, String encryptionStrength) {
		this.name = name;
		this.encryptionGrade = encryptionGrade;
		this.encryptionStrength = encryptionStrength;
	}

	public int getEncryptionGrade() {
		return encryptionGrade;
	}
	public String getName() {
		return name;
	}
	public String getEncryptionStrength(){
		return encryptionStrength;
	}
	
	public static EncryptionProvider getEncryptionProvider(int encryptionGrade){
		// run through encryption provider and determine which one of them has the desired grade
		for (EncryptionProvider code : EncryptionProvider.values()) {
			if(code.getEncryptionGrade() == encryptionGrade)
				return code;
		}
		return EncryptionProvider.NONE;
	}
	public static EncryptionProvider getEncryptionProvider(String encryptionStrength){
		// run through encryption provider and determine which one of them has the desired strength
		for (EncryptionProvider code : EncryptionProvider.values()) {
			if(code.getEncryptionStrength() == encryptionStrength)
				return code;
		}
		return EncryptionProvider.NONE;
	}
}