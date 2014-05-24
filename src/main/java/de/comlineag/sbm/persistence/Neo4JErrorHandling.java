package de.comlineag.sbm.persistence;

public class Neo4JErrorHandling extends Exception {

	/**
	 * Error handling for graph actions
	 */
	private static final long serialVersionUID = 4751739156910409082L;

	public Neo4JErrorHandling() {

	}

	public Neo4JErrorHandling(String locationURI) {
		super("Something went wrong working with the graph at URL " + locationURI);
	}
}
