package de.comlineag.snc.data;

/**
 * 
 * @author		Maic Rittmeier
 * @category	program flow and structure
 * @version 	1.0
 *
 * @description	throws a custom exception for the Lithium network
 * 
 * @changelog	1.0 class created
 * 
 */
public class LithiumStatusException extends Exception {
	
	private static final long serialVersionUID = -3476297335427110635L;

	public LithiumStatusException(String string) {
		super(string);
	}
	
	public LithiumStatusException(Throwable t){
		super(t);
	}
}
