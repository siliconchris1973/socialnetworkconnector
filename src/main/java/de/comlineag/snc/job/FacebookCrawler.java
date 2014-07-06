package de.comlineag.snc.job;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.log4j.Logger;

import com.twitter.hbc.core.endpoint.Location;

import de.comlineag.snc.constants.ConfigurationConstants;
import de.comlineag.snc.constants.HttpStatusCode;
import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.handler.CrawlerConfiguration;
import de.comlineag.snc.handler.FacebookParser;
import de.comlineag.snc.helper.NoBase64EncryptedValue;

// TODO !!!!! IMPLEMENT THE FaceBookCrawler !!!!!!

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.1 - not yet implemented
 * 
 * @description this is the actual crawler of the facebook network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              facebook rest api to fetch posts and users
 * 
 * @changelog	0.1 copy of TwitterCrawler 0.9 plus dynamic config			Chris
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
	private String smallLogMessage = "";
	
	public FacebookCrawler() {
		
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(100000);
				
		// instantiate the Facebook-Posting-Manager
		post = new FacebookParser();
	}

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.debug("Facebook-Crawler START");
		
		// some static vars for the facebook crawler
		String _user = null;
		String _passwd = null;
		try {
			_user = decryptValue((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_USER_KEY));
			_passwd = decryptValue((String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.AUTHENTICATION_PASSWORD_KEY));
		} catch (NoBase64EncryptedValue e) {
			logger.error("EXCEPTION :: value for user or passwd is NOT base64 encrypted " + e.toString(), e);
			System.exit(-1);
		}
		
		final String PROTOCOL = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_PROTOCOL_KEY);
		final String SERVER_URL = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_SERVER_URL_KEY);
		final String PORT = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_PORT_KEY);
		final String REST_API_LOC = (String) arg0.getJobDetail().getJobDataMap().get(ConfigurationConstants.HTTP_ENDPOINT_REST_API_LOC_KEY);
		
		final String COMMUNITY_NAME = (String) arg0.getJobDetail().getJobDataMap().get("COMMUNITY_NAME");
		
		final String REST_API_URL = PROTOCOL + "://" + SERVER_URL + ":" + PORT + COMMUNITY_NAME + REST_API_LOC;
		
		logger.trace("setting up the rest endpoint at " + REST_API_URL + " with user " + _user);
		
		
		// setup restrictions on what to track
		// THESE ARE USED TO RESTRICT RESULTS TO SPECIFIC TERMS, LANGUAGES AND USERS
		logger.info("retrieving restrictions from configuration db");
		ArrayList<String> tTerms = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_TERM_TEXT, SocialNetworks.FACEBOOK);
		ArrayList<Long> tUsers = new CrawlerConfiguration<Long>().getConstraint(ConfigurationConstants.CONSTRAINT_USER_TEXT, SocialNetworks.FACEBOOK);
		ArrayList<String> tLangs = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_LANGUAGE_TEXT, SocialNetworks.FACEBOOK);
		ArrayList<String> tSites = new CrawlerConfiguration<String>().getConstraint(ConfigurationConstants.CONSTRAINT_SITE_TEXT, SocialNetworks.FACEBOOK);
		ArrayList<Location> tLocas = new CrawlerConfiguration<Location>().getConstraint(ConfigurationConstants.CONSTRAINT_LOCATION_TEXT, SocialNetworks.FACEBOOK);
		
		// simple log output
		if (tTerms.size()>0){
			smallLogMessage += "specific terms ";
		}
		if (tUsers.size()>0){
			smallLogMessage += "specific user ";
		}
		if (tLangs.size()>0){
			smallLogMessage += "specific languages ";
		}
		if (tSites.size()>0){
			smallLogMessage += "specific Sites ";
		}
		if (tLocas.size()>0) {
			smallLogMessage += "specific Locations ";
		}
		
		logger.debug("new facebook crawler instantiated - restricted to track " + smallLogMessage);
		
		//TODO implement authentication against facebook network
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), (String) arg0.getJobDetail()
				.getJobDataMap().get("consumerSecret"), (String) arg0.getJobDetail().getJobDataMap().get("token"), (String) arg0
				.getJobDetail().getJobDataMap().get("tokenSecret"));
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
	private String httpPost(String urlStr, String[] paramName, String[] paramVal) throws Exception {
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
	private String httpGet(String urlStr) throws IOException {
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
	
	/**
	 * 
	 * @description decrypt given text 
	 * 
	 * @param 		param
	 *          	  encrypted text 
	 * @return 		clear text
	 *
	 */
	private static String decryptValue(String param) throws NoBase64EncryptedValue {

		// the decode returns a byte-Array which needs to be converted to a string before returning
		byte[] base64Array;

		// validates that an encrypted value was returned
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new NoBase64EncryptedValue("Parameter " + param + " ist nicht Base64-verschluesselt");
		}
		// (re)convert into string and return
		return new String(base64Array);
	}
}
