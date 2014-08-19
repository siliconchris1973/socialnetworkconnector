package de.comlineag.snc.job;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.comlineag.snc.handler.ConfigurationCryptoHandler;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.1
 * @status		in development
 * 
 * @description a crawler that searches the file system directory and tries to store missed 
 * 
 * @changelog	0.1 class created
 * 
 */
public class FsCrawler implements Job {
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private ConfigurationCryptoHandler configurationCryptoProvider = new ConfigurationCryptoHandler();
	
	public FsCrawler() {
		super();
	}
	
	// every crawler must implement a method execute
	public void execute(JobExecutionContext arg0) throws JobExecutionException{}
}