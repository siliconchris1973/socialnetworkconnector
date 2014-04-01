package de.comlineag.sbm.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
//import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import de.comlineag.sbm.handler.*;


/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description this is the actual crawler of the twitter network. It is implemented as a 
 * 				job and, upon execution, will connect to the twitter api to grab new tweets 
 * 				as they are created on the network.
 *
 */
public class TwitterCrawler extends GenericCrawler implements Job{

	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	// Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream
	private BlockingQueue<String> msgQueue;
	//private BlockingQueue<Event> eventQueue;
	private TwitterParser post;
	
	// Set up the executor service to distribute the actual tasks 
	private ExecutorService service;
	private int numProcessingThreads;
	
	public TwitterCrawler() {
		logger.debug("constructor of class" + getClass().getName() + " called");
		
		//System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
		//System.setProperty(org.slf4j.impl.SimpleLogger.WARN_LEVEL_STRING_KEY, "DEBUG");
		
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(100000);
		//eventQueue = new LinkedBlockingQueue<Event>(1000);
		
		// instantiiere den Manager fuer das Twitter-Posting-Objekt als Ableitung von Posting
		post = new TwitterParser();
		
		// Create an executor service which will spawn threads to do the actual work of 
		// parsing the incoming messages and calling the listeners on each message
		numProcessingThreads = 4;
		service = Executors.newFixedThreadPool(numProcessingThreads);
	}

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.debug("method execute from class " + getClass().getName() + " called");
				
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

	    //endpoint.trackTerms(Lists.newArrayList("SocialBrandMonitor4HANA", "SocialBrandMonitor"));
		endpoint.trackTerms(Lists.newArrayList("SAP", "ERP", "BW", "BO", "CRM", "SCM", "SRM", "IDM",
												"NetWeaver", "ABAP", "HANA", "Business Objects",
												"Business Warehouse", "Customer Relationship Management",
												"Supply Chain Management", "Supplier Relationship Management",
												"Identity Management"));
	    
	    Authentication sn_Auth = new OAuth1(
	    		(String)arg0.getJobDetail().getJobDataMap().get("consumerKey")
				,(String)arg0.getJobDetail().getJobDataMap().get("consumerSecret")
				,(String)arg0.getJobDetail().getJobDataMap().get("token")
				,(String)arg0.getJobDetail().getJobDataMap().get("tokenSecret")
				);

	    // Create a new BasicClient. By default gzip is enabled.
	    Client client = new ClientBuilder()
	      .hosts(Constants.STREAM_HOST)
	      .endpoint(endpoint)
	      .authentication(sn_Auth)
	      .processor(new StringDelimitedProcessor(msgQueue))
	      .build();

	    // Establish a connection
	    client.connect();
	    logger.info("Client connected");
	    
	    // Do whatever needs to be done with messages
	    for (int msgRead = 0; msgRead < 1000; msgRead++) {
	    	
	    	String msg;
	    	msg = "";
	    	try {
	    		msg = msgQueue.take();
	    	} catch (InterruptedException e) {
	    	  	logger.error(e.toString());
	    	}
	      	logger.info("New Tweet " + msg);
	      	
	      	// Jede einzelne Message wird nun an den Parser TwitterParser (abgeleitet von GenericParser) uebergeben
	      	post.process(msg);
	      	logger.info("TWEETY IST AUS!!!!!!!!");
	    }
	    
	    client.stop();
	}
}

