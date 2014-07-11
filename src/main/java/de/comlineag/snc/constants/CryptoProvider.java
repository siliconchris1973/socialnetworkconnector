package de.comlineag.snc.constants;


/**
 *
 * @author 		Christian Guenther
 * @category	enum
 * @version 	0.4
 * @status		productive
 * 
 * @description contains all supported encryption provider, a value to indicate the grade and one for 
 * 				the strength of the encryption (both correspond).
 * 				
 * @changelog	0.1 (Chris)		class created
 * 				0.2				added support to query for the strength of an encryption provider
 * 								and support to determine which encryption algorithm provides the desired grade 
 * 				0.3				changed name of cryptographic providers according to class names 
 * 				0.4				renamed crypto provider from Encryption... to Crypto...
 * 
 */
public enum CryptoProvider {
//	enum code			name						int / String encryptionGrade/Strength	
	NONE 				("NullCryptoProvider", 		0,	"none"),	// implemented
	BASE64				("Base64CryptoProvider", 	1,	"low"),		// implemented
	DES					("DesCryptoProvider", 		2,	"medium"),	// in development
	DES3				("Des3CryptoProvider", 		3,	"good"),	// 
	AES					("AesCryptoProvider", 		4,	"best");	// 
	
	private final String name;
	private final int encryptionGrade;
	private final String encryptionStrength;
	
	private CryptoProvider(String name, int encryptionGrade, String encryptionStrength) {
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
	
	public static CryptoProvider getCryptoProvider(int encryptionGrade){
		// run through encryption provider and determine which one of them has the desired grade
		for (CryptoProvider code : CryptoProvider.values()) {
			if(code.getEncryptionGrade() == encryptionGrade)
				return code;
		}
		return CryptoProvider.NONE;
	}
	public static CryptoProvider getCryptoProvider(String encryptionStrength){
		// run through encryption provider and determine which one of them has the desired strength
		for (CryptoProvider code : CryptoProvider.values()) {
			if(code.getEncryptionStrength() == encryptionStrength)
				return code;
		}
		return CryptoProvider.NONE;
	}
}