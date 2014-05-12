package de.comlineag.sbm.persistence;

public class HANAConnectionFailed extends Exception {

	/**
	 * Exception indicating Failures in backend Calls to SAP HANA
	 */
	private static final long serialVersionUID = 4751739156910409082L;

	public HANAConnectionFailed() {

	}

	public HANAConnectionFailed(String locationURI, String user) {
		super("Connection failed for " + locationURI + " with User " + user);
	}
}
