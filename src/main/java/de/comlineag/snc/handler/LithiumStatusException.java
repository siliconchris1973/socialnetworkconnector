package de.comlineag.snc.handler;

/**
 * 
 * @author		Maic Rittmeier
 * @category	program flow and structure
 * @version 	0.1
 * @status		productive
 *
 * @description	throws a custom exception for the Lithium network
 * 
 * @changelog	0.1 (Maic)		class created
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
