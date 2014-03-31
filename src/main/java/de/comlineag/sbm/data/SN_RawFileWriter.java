package de.comlineag.sbm.data;

import java.io.File;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 *
 */
public class SN_RawFileWriter extends File {

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SN_RawFileWriter(String pathname) {
		super(pathname);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + pathname);
				
	}

	public SN_RawFileWriter(URI uri) {
		super(uri);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + uri);
	}

	public SN_RawFileWriter(String parent, String child) {
		super(parent, child);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + parent + " and " + child);
	}

	public SN_RawFileWriter(File parent, String child) {
		super(parent, child);
		// TODO Auto-generated constructor stub
		// log the startup message
		logger.debug("constructor of class " + getClass().getName() + " called with " + parent.toString() + " and " + child);
	}
}
