package de.comlineag.snc.persistence;

/**
 * 
 * @author		Magnus Leinemann
 * @category	Error handling
 * @version		0.2
 * @status		productive
 * 
 * @description	Defines a unique identifier for errors related to the hana persistence
 *
 * @changelog	0.1 (Magnus)		class created
 * 				0.2 (Chris)			added text to call of super class 
 * 
 */
public class HANAConnectionFailed extends Exception {
	
	// Exception indicating Failures in backend Calls to SAP HANA
	private static final long serialVersionUID = 4751739156910409082L;

	public HANAConnectionFailed() {}

	public HANAConnectionFailed(String locationURI, String user, Throwable t) {
		super("ERROR :: Connection to HANA failed for " + locationURI + " with user " + user, t);
	}
}
