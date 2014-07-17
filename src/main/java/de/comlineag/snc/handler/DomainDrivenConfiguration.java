package de.comlineag.snc.handler;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.SocialNetworks;

/**
 * 
 * @author 		Christina Guenther
 * @category	job control handler
 * @revision	0.1
 * @status		in development
 * 
 * @description	this class is used to setup the overall domain driven and/or customer 
 * 				driven configuration for the snc. It is instantiated by the job control
 * 				from applicationContext.xml and sets the currently used domain and
 * 				customer. These values in turn are then accessed by the actual crawler
 * 				and passed to the crawler configuration handler to receive the correct 
 * 				constraints from the crawler configuration.
 * 
 * @changelog	0.1 (Chris) 	class created
 *
 */
public class DomainDrivenConfiguration implements Job {
	private static JSONObject configurationScope = new JSONObject();
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	// get the configured domains and customers from job control
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		configurationScope.put((String) ConfigurationConstants.domainIdentifier, (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.domainIdentifier));
		configurationScope.put((String) ConfigurationConstants.customerIdentifier, (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.customerIdentifier));
		logger.info("retrieving domain (as "+ configurationScope.get(ConfigurationConstants.domainIdentifier) +") and customer (as "+ configurationScope.get(ConfigurationConstants.customerIdentifier)+") from job control");
	}
	
	public static JSONObject getDomainSetup() {
		return configurationScope;
	}
}
