package de.comlineag.sbm.job;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.log4j.Logger;

import de.comlineag.sbm.data.HttpErrorMessages;
import de.comlineag.sbm.data.HttpStatusCode;
import de.comlineag.sbm.handler.LithiumParser;
import de.comlineag.sbm.persistence.NoBase64EncryptedValue;

/**
 * 
 * @author Christian Guenther
 * @category Handler
 * 
 * @description this is the actual crawler of the lithium network. It is
 *              implemented as a job and, upon execution, will connect to the
 *              lithium rest api to fetch posts and users
 * 
 */
public class LithiumCrawler extends GenericCrawler implements Job {

	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	
	// Set up your blocking queues: Be sure to size these properly based on
	// expected TPS of your stream
	private BlockingQueue<String> msgQueue;
	private LithiumParser post;
	
	
	// this string is used to compose all the little debug messages from the different restriction possibilities
	// on the posts, like terms, languages and the like. it is only used in debugging afterwards.
	private String smallLogMessage = "";
	
	
	public LithiumCrawler() {
		logger.trace("Instantiated LithiumCrawler Class");
		// Define message and event queue
		msgQueue = new LinkedBlockingQueue<String>(100000);
				
		// instantiate the Lithium-Posting-Manager
		post = new LithiumParser();
	}

	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		// log the startup message
		logger.debug("Lithium-Crawler START");
		
		// some static vars for the lithium crawler taken from applicationContext.xml
		final String PROTOCOL = (String) arg0.getJobDetail().getJobDataMap().get("PROTOCOL");
		final String SERVER_URL = (String) arg0.getJobDetail().getJobDataMap().get("SERVER_URL");
		final String PORT = (String) arg0.getJobDetail().getJobDataMap().get("PORT");
		final String REST_API_LOC = (String) arg0.getJobDetail().getJobDataMap().get("REST_API_LOC");
		final String REST_API_URL = PROTOCOL + "://" + SERVER_URL + ":" + PORT + REST_API_LOC;
		// authentication to lithium
		String _user = null;
		String _passwd = null;
		try {
			_user = decryptValue((String) arg0.getJobDetail().getJobDataMap().get("user"));
			_passwd = decryptValue((String) arg0.getJobDetail().getJobDataMap().get("passwd"));
		} catch (NoBase64EncryptedValue e4) {
			logger.error("EXCEPTION :: value for user or passwd is NOT base64 encrypted " + e4.toString());
		}
		
		logger.trace("setting up the rest endpoint at " + REST_API_URL + " with user " + _user);
		
		
		// setup restrictions on what to get from lithium - also says where to look
		String[] tSites = {"/Girokonto-Zahlungsverkehr/bd-p/Girokonto-Zahlungsverkehr",
							"/Sparen-Anlegen/bd-p/Sparen-und-Anlegen",
							"/Wertpapierhandel/bd-p/Wertpapierhandel",
							"/Finanzieren/bd-p/Finanzieren",
							"/Sonstige-Themen/bd-p/Sonstige-Themen"};
		smallLogMessage += "specific Sites ";
		
		String[] tTerms = {"Tagesgeld", "Trading", "Depot", "Girokonto", "Wertpapier", "Kreditkarte", "HBCI"};
		smallLogMessage += "specific terms ";
		
		String[] tLangs = {"de", "en"};
		smallLogMessage += "specific languages ";
		
		logger.debug("new lithium parser instantiated - restricted to track " + smallLogMessage);
		
		
		//TODO implement authentication against lithium network
		/*
		Authentication sn_Auth = new OAuth1((String) arg0.getJobDetail().getJobDataMap().get("consumerKey"), 
											(String) arg0.getJobDetail().getJobDataMap().get("consumerSecret"), 
											(String) arg0.getJobDetail().getJobDataMap().get("token"), 
											(String) arg0.getJobDetail().getJobDataMap().get("tokenSecret"));
		*/
		
		// this is the connection object and the status - I need this outside the try catch clause
		HttpsURLConnection conn = null;
		HttpStatusCode statusCode = null;
		
		try {
			String tURL = REST_API_URL + "/messages"; //+ "/Girokonto-Zahlungsverkehr/bd-p/Girokonto-Zahlungsverkehr";
			//String mURL = REST_API_URL + 
			
			logger.trace("initiating ssl-connection to " + tURL);
			
			URL url = new URL(tURL);
			conn = (HttpsURLConnection) url.openConnection();
			
			statusCode = HttpStatusCode.getHttpStatusCode(conn.getResponseCode());
			
			if (!statusCode.isOk()){
				if (statusCode == HttpStatusCode.FORBIDDEN){
					//TODO implement proper authorization handling
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode()));
				} else {
					logger.error(HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode())+" could not connect to " + tURL.toString() + " " + conn.getResponseMessage());
				}
			} else {
				logger.debug("connection established (status is " + statusCode + ") now checking returned xml");
			}	
			
			//Create the parser instance
	        LithiumParser parser = new LithiumParser();
	 
	        //Parse the file
	        ArrayList errors = parser.parseXml(conn.getInputStream());
	 
	        //Verify the result
	        System.out.println(errors);
			
			// CODE to simply output XML content
			/*
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			InputSource inputSource = new InputSource(in);
			String inputLine;
			while ((inputLine = in.readLine()) != null) 
				logger.trace(inputLine.toString());
			in.close();
			*/
			
			
			conn.disconnect();
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.toString());
		}
		
		/*
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
		*/
		
		logger.debug("Lithium-Crawler END");
	}
	
	
	// some useful functions
	/**
	 * 
	 */
	private void basicAuthentication(){ 
		Authenticator.setDefault( new Authenticator() {
			@Override protected PasswordAuthentication getPasswordAuthentication() {
				System.out.printf( "url=%s, host=%s, ip=%s, port=%s%n",
	                       getRequestingURL(), getRequestingHost(),
	                       getRequestingSite(), getRequestingPort() );
				
				return new PasswordAuthentication( "user", "abc".toCharArray() );
			}
		});
	}
	
	/**
	 * @description connects to the url and posts some Key Value pairs to the endpoint
	 * @param urlStr
	 * @param paramName
	 * @param paramVal
	 * @return
	 * @throws Exception
	 */
	public String httpPost(URL url, String[] paramName, String[] paramVal) throws Exception {
		HttpURLConnection conn =
				(HttpURLConnection) url.openConnection();

		HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(conn.getResponseCode());
		
		if (!statusCode.isOk()){
			throw new IOException("EXCEPTION :: "+HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode())+" could not connect to " + url.toString() + " " + conn.getResponseMessage());
		} else {
			logger.info("connection established " + statusCode);
		}

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
		
		HttpStatusCode.getHttpStatusCode(conn.getResponseCode());
		
		if (!statusCode.isOk()) {
			throw new IOException("EXCEPTION :: could not post to " + url.toString() + " " + conn.getResponseMessage());
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
	 * @description connects to the given url using the GET method
	 * @param urlStr
	 * @return
	 * @throws IOException
	 */
	public String httpGet(URL url) throws IOException {
		logger.trace("initiating url connection now...");
		
		HttpURLConnection conn =
				(HttpURLConnection) url.openConnection();
		
		HttpStatusCode statusCode = HttpStatusCode.getHttpStatusCode(conn.getResponseCode());
		
		if (!statusCode.isOk()){
			throw new IOException("EXCEPTION :: "+HttpErrorMessages.getHttpErrorText(statusCode.getErrorCode())+" could not connect to " + url.toString() + " " + conn.getResponseMessage());
		} else {
			logger.info("connection established " + statusCode);
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
	 * @description Entschluesselt Werte aus der Konfig fuer die Connection
	 *
	 * @param param
	 *            der Wert der entschluesselt werden soll
	 * @return Klartext
	 *
	 */
	@SuppressWarnings("unused")
	private String decryptValue(String param) throws NoBase64EncryptedValue {

		// byte-Array kommt vom Decoder zurueck und kann dann in String uebernommen und zurueckgegeben werden
		byte[] base64Array;

		// Validierung das auch ein verschluesselter Wert da angekommen ist
		try {
			base64Array = Base64.decodeBase64(param.getBytes());
		} catch (Exception e) {
			throw new NoBase64EncryptedValue("Parameter " + param + " ist nicht Base64-verschluesselt");
		}
		// konvertiere in String
		return new String(base64Array);
	}
}
