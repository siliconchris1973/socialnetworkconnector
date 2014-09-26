/**
 * 
 */
package de.comlineag.snc.helper;

/**
 * 
 * @author 		Christian Gunether
 * @category 	helper class
 * @version 	0.1					- 25.09.2014
 * @status		productive
 *
 * @description creates a Long from a given aphanumerical string  
 * 				The string is stripped of any non alphanumerical characters and then
 * 				any character is converted to a number (a=1, b=2, ..., z=26). 
 * 
 * @changelog	0.1 (Chris)			class created
 */
public final class UniqueIdServices {
	// the most primitive conversion map
	@SuppressWarnings("unused")
	private static final String[] CHARACTER_ARRAY = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
	@SuppressWarnings("unused")
	private static final int[] 	  NUMBER_ARRAY 	  = { 1,   2,   3,   4,   5,   6,   7,   8,   9,   10,  11,  12,  13,  14,  15,  16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26};
	
	// from http://www.wallstreet-online.de/userzentrum/REGISTRIERUNG.html
	// via	httpwwwwallstreetonlinedeuserzentrumregistrierunghtml
	// via	82020162323232311212192018552015141291454521195182651420182113185791920189518211478201312
	// via	82020162                                                                         78201312
	// to	7820131282020162
	
	public static String createId(String inputString) throws NullPointerException{
		assert (inputString != null) : "ERROR :: cannot operate on empty input";
		
		String finalString = transformInput(inputString);
		int length = finalString.length();
		int max = 24;
		if (length > max){
			String front = finalString.substring(0, 8);
			String back = finalString.substring((length-8), length);
			finalString = back+front;
		}
		
		if (finalString.length() >16)
			finalString = finalString.substring(0, 15);
		return finalString;
	}
	
	private static String transformInput(String inputString){
		return inputString.replaceAll("[^A-Za-z0-9]", "").toLowerCase()
				.replaceAll("a", "1")
				.replaceAll("b", "2")
				.replaceAll("c", "3")
				.replaceAll("d", "4")
				.replaceAll("e", "5")
				.replaceAll("f", "6")
				.replaceAll("g", "7")
				.replaceAll("h", "8")
				.replaceAll("i", "9")
				.replaceAll("j", "10")
				.replaceAll("k", "11")
				.replaceAll("l", "12")
				.replaceAll("m", "13")
				.replaceAll("n", "14")
				.replaceAll("o", "15")
				.replaceAll("p", "16")
				.replaceAll("q", "17")
				.replaceAll("r", "18")
				.replaceAll("s", "19")
				.replaceAll("t", "20")
				.replaceAll("u", "21")
				.replaceAll("v", "22")
				.replaceAll("w", "23")
				.replaceAll("x", "24")
				.replaceAll("y", "25")
				.replaceAll("z", "26");
	}
	/**
	 * the class is not to be instantiated
	 */
	private UniqueIdServices() {
		
	}
}
