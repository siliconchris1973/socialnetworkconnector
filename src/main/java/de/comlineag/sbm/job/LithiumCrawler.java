package de.comlineag.sbm.job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.log4j.Logger;

import de.comlineag.sbm.data.HttpStatusCode;
import de.comlineag.sbm.handler.LithiumParser;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description this is the actual crawler of the lithium network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              lithium api to grab new posts as they are created on the
 *              network.
 * 
 */
public class LithiumCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	// Set up your blocking queues: Be sure to size these properly based on
	// expected TPS of your stream
	private BlockingQueue<String> msgQueue;
	private LithiumParser post;
	
	//TODO these need to come from applicationContext.xml configuration file
	private boolean restrictToTrackterms = true;
	private boolean restrictToLanguages = true;
	private boolean restrictToUsers = false;
	private boolean restrictToLocations = false; // locations does not yet work
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String bigLogMessage = "";
	private String smallLogMessage = "";
	
	// some static vars for the lithium crawler
	String _user = "cmral";
	String _passwd = "123Test123";
	
	private static final String SERVER_URL = "http://community.lithium.com";
	private static final String REST_API_LOC = "/community-name/restapi/vc";
	private static final String REST_API_URL = SERVER_URL + REST_API_LOC;
	private static final String DEVELOPER_URL = "http://community.lithium.com/t5/Developer-Network/ct-p/Developer";
	
	public LithiumCrawler() {
		
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(100000);
				
		// instantiate the Lithium-Posting-Manager
		post = new LithiumParser();
		
		// TODO check what about multithreading and executor services
		/*
		// Set up the executor service to distribute the actual tasks
		final int numProcessingThreads = 4;
		// Create an executor service which will spawn threads to do the actual work
		// of parsing the incoming messages and calling the listeners on each message
		ExecutorService service = Executors.newFixedThreadPool(numProcessingThreads);
		 */
	}

	/**
	 * 
	 * @description connects to an http endpoint
	 */
	private void connect(){
		//TODO write code
	}
	
	/**
	 * @description connects to the url and posts some Key Value pairs
	 * @param urlStr
	 * @param paramName
	 * @param paramVal
	 * @return
	 * @throws Exception
	 */
	public static String httpPost(String urlStr, String[] paramName, String[] paramVal) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn =
				(HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		conn.setRequestProperty("Content-Type",
								"application/x-www-form-urlencoded");

		// Create the form content
		OutputStream out = conn.getOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		for (int i = 0; i < paramName.length; i++) {
			writer.write(paramName[i]);
			writer.write("=");
			writer.write(URLEncoder.encode(paramVal[i], "UTF-8"));
			writer.write("&");
		}
		writer.close();
		out.close();
		
		HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(conn.getResponseCode());
		
		if (!statusCode.isOk()) {
			throw new IOException(conn.getResponseMessage());
		}
		
		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(
			new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		
		conn.disconnect();
		return sb.toString();
	}
	
	/**
	 * @description connects to the given url
	 * @param urlStr
	 * @return
	 * @throws IOException
	 */
	public static String httpGet(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn =
				(HttpURLConnection) url.openConnection();
		
		HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(conn.getResponseCode());
		
		if (!statusCode.isOk()) {
			throw new IOException(conn.getResponseMessage());
		}
		
		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(
				new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		
		conn.disconnect();
		return sb.toString();
	}
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.debug("Lithium-Crawler START");
		
		logger.trace("setting up the rest endpoint with ");
		
		
		// possible restrictions
		if (restrictToTrackterms) {
			//TODO check how to track specific terms
			//TODO the trackterms need to go in a configuration file or a database
			
			String[] ttTerms = {"SAP", "ERP", "SAP BW", "BO", "CRM", "SCM", "SRM", "IDM", 
								"NetWeaver", "ABAP", "HANA", "Business Objects", 
								"Business Warehouse", "Customer Relationship Management", 
								"Supply Chain Management", "Supplier Relationship Management", 
								"Identity Management", "Social Brand Monitor",
								"Social Activity Analyzer"};
			
			//String[] ttTerms = {"SocialActivityAnalyzer", "SocialNetworkAnalyzer", "SocialBrandMonitor", "SocialNetworkConnector"};
			ArrayList<String> tTerms = new ArrayList<String>();
			for (int i = 0 ; i < ttTerms.length ; i++) {
				tTerms.add(ttTerms[i]);
			}
			
			bigLogMessage += "\n                       restricted to terms: " + tTerms.toString() + "\n";
			smallLogMessage += "specific terms ";
		}
		
		// Restrict tracked messages to english and german
		if (restrictToLanguages) {
			ArrayList<String> langs = new ArrayList<String>();
			langs.add("de");
			langs.add("en");
			
			//endpoint.languages(langs);
			bigLogMessage += "                       restricted to languages: " + langs.toString() + "\n";
			smallLogMessage += "specific languages ";
		}
		
		// Restrict the tracked messages to specific users
		if (restrictToUsers) {
			ArrayList<Long> users = new ArrayList<Long>();
			users.add(754994L);			// Christian Guenther
			users.add(2412281046L);		// Magnus Leinemann
			
			//endpoint.followings(users);
			bigLogMessage += "                       restricted on user: " + users.toString() + "\n";
			smallLogMessage += "specific users ";
		}
		
		// Restrict the tracked messages to specific locations
		if (restrictToLocations) {
			
			// TODO check how to work with locations in hbc lithium api 
			ArrayList<String> locs = new ArrayList<String>(); 
			locs.add("Germany");
			locs.add("USA");
			
			//endpoint.locations(locs);
			
			bigLogMessage += "                       restricted on locations: " + locs.toString() + " (NOT IMPLEMENTED)";
			smallLogMessage += "specific locations ";
		}
		
		logger.debug("new lithium parser instantiated - restricted to track " + smallLogMessage);
		logger.trace("call for Endpoint POST: " //+ endpoint.getPostParamString() 
					+ bigLogMessage);
		
		//TODO implement authentication against lithium network
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), (String) arg0.getJobDetail()
				.getJobDataMap().get("consumerSecret"), (String) arg0.getJobDetail().getJobDataMap().get("token"), (String) arg0
				.getJobDetail().getJobDataMap().get("tokenSecret"));
		*/
		
		// TODO implement lithium connection handler
		// Create a new BasicClient. By default gzip is enabled.
		/*
		Client client = new ClientBuilder().hosts(Constants.STREAM_HOST).endpoint(endpoint).authentication(sn_Auth)
				.processor(new StringDelimitedProcessor(msgQueue)).connectionTimeout(1000).build();

		// Establish a connection
		try {
			client.connect();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		*/
		
		// Do whatever needs to be done with messages
		for (int msgRead = 0; msgRead < 1000; msgRead++) {
			String msg = "";
			try {
				// TODO check hwo to take the messages from client
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				logger.error("ERROR :: Message loop interrupted " + e.getMessage());
			} catch (Exception ee) {
				logger.error("EXCEPTION :: Exception in message loop " + ee.getMessage());
			}
			logger.info("New Post tracked from " + msg.substring(15, 45) + "...");
			logger.trace("complete post: " + msg );

			// Jede einzelne Message wird nun an den Parser LithiumParser
			// (abgeleitet von GenericParser) uebergeben
			post.process(msg);
		}
		
		logger.debug("Lithium-Crawler END");
		// TODO check if we need to stop the client
		//client.stop();
	}
}
