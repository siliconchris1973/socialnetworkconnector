package de.comlineag.sbm.job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.log4j.Logger;

import de.comlineag.sbm.data.HttpStatusCode;
import de.comlineag.sbm.handler.FacebookParser;

// TODO !!!!! IMPLEMENT THE FaceBookCrawler !!!!!!

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description this is the actual crawler of the facebook network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              facebook rest api to fetch posts and users
 * 
 */
public class FacebookCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	// Set up your blocking queues: Be sure to size these properly based on
	// expected TPS of your stream
	private BlockingQueue<String> msgQueue;
	private FacebookParser post;
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String bigLogMessage = "";
	private String smallLogMessage = "";
	
	public FacebookCrawler() {
		
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(100000);
				
		// instantiate the Facebook-Posting-Manager
		post = new FacebookParser();
	}

	/**
	 * 
	 * @description connects to an http endpoint
	 */
	private void connect(){
		//TODO write code to connect to facebook
	}
	
	/**
	 * @description connects to the url and posts some Key Value pairs to the endpoint
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
		logger.debug("Facebook-Crawler START");
		
		// some static vars for the facebook crawler
		final String _user = (String) arg0.getJobDetail().getJobDataMap().get("user");
		final String _passwd =  (String) arg0.getJobDetail().getJobDataMap().get("passwd");
		
		final String PROTOCOL = (String) arg0.getJobDetail().getJobDataMap().get("PROTOCOL");
		final String SERVER_URL = (String) arg0.getJobDetail().getJobDataMap().get("SERVER_URL");
		final String PORT = (String) arg0.getJobDetail().getJobDataMap().get("PORT");
		final String COMMUNITY_NAME = (String) arg0.getJobDetail().getJobDataMap().get("COMMUNITY_NAME");
		final String REST_API_LOC = (String) arg0.getJobDetail().getJobDataMap().get("REST_API_LOC");
		final String REST_API_URL = PROTOCOL + "://" + SERVER_URL + ":" + PORT + COMMUNITY_NAME + REST_API_LOC;
		
		logger.trace("setting up the rest endpoint at " + REST_API_URL + " with user " + _user);
		
		
		// setup restrictions on what to track
		// TODO check why these won't be fetched from applicationContext.xml
		final boolean restrictToTrackterms = true;	//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToTrackterms");
		final boolean restrictToLanguages = true;	//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToLanguages");
		final boolean restrictToUsers = false;		//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToUsers");
		final boolean restrictToLocations = false;	//(boolean) arg0.getJobDetail().getJobDataMap().get("restrictToLocations"); // locations does not yet work
		
		// possible restrictions
		if (restrictToTrackterms) {
			//TODO check how to track specific terms
			//TODO the trackterms need to go in a configuration file or a database
			
			/*
			String[] ttTerms = {"SAP", "ERP", "SAP BW", "BO", "CRM", "SCM", "SRM", "IDM", 
								"NetWeaver", "ABAP", "HANA", "Business Objects", 
								"Business Warehouse", "Customer Relationship Management", 
								"Supply Chain Management", "Supplier Relationship Management", 
								"Identity Management", "Social Brand Monitor",
								"Social Activity Analyzer"};
			*/
			
			String[] ttTerms = {"Tagesgeld", "Trading", "Depot", "Girokonto", "Wertpapier", "Kreditkarte", "HBCI"};
			
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
			
			// TODO check how to work with locations in hbc facebook api 
			ArrayList<String> locs = new ArrayList<String>(); 
			locs.add("Germany");
			locs.add("USA");
			
			//endpoint.locations(locs);
			
			bigLogMessage += "                       restricted on locations: " + locs.toString() + " (NOT IMPLEMENTED)";
			smallLogMessage += "specific locations ";
		}
		
		logger.debug("new facebook crawler instantiated - restricted to track " + smallLogMessage);
		logger.trace("call for Endpoint POST: " //+ endpoint.getPostParamString() 
					+ bigLogMessage);
		
		//TODO implement authentication against facebook network
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), (String) arg0.getJobDetail()
				.getJobDataMap().get("consumerSecret"), (String) arg0.getJobDetail().getJobDataMap().get("token"), (String) arg0
				.getJobDetail().getJobDataMap().get("tokenSecret"));
		*/
		
		// TODO implement facebook connection handler
		/*
		try {
			httpGet(REST_API_URL);
		} catch (IOException e1) {
			logger.error("EXCEPTION :: Could not connect to " + REST_API_URL + ": " + e1);
		}
		*/
		
		URL url;
		URLConnection con;
		try {
			url = new URL(REST_API_URL);
			con = url.openConnection();
		} catch (MalformedURLException e1) {
			logger.error("EXCEPTION :: the url " + REST_API_URL + " is malformed: " + e1);
		} catch (IOException e2){
			logger.error("EXCEPTION :: Could not connect to " + REST_API_URL + ": " + e2);
		}
		
		// Do whatever needs to be done with messages 
		/*
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

			// Jede einzelne Message wird nun an den Parser FacebookParser
			// (abgeleitet von GenericParser) uebergeben
			post.process(msg);
		}
		*/
		
		logger.debug("Facebook-Crawler END");
		
	}
}
