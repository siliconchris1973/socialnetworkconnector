package de.comlineag.sbm.persistence;

import java.io.File;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 *
 */
public final class FileWriterToCSV extends File {

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FileWriterToCSV(String pathname) {
		super(pathname);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + pathname);
				
	}

	public FileWriterToCSV(URI uri) {
		super(uri);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + uri);
	}

	public FileWriterToCSV(String parent, String child) {
		super(parent, child);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + parent + " and " + child);
	}

	public FileWriterToCSV(File parent, String child) {
		super(parent, child);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + parent.toString() + " and " + child);
	}
}
