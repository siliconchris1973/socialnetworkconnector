package de.comlineag.sbm.data;
/**
 * 
 * @author		Christian Guenther
 * @type		program flow and structure
 *
 * @description	throws a custom exception for the Lithium network
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
