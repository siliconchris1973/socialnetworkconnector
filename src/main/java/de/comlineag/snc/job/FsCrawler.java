package de.comlineag.snc.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.comlineag.snc.constants.SocialNetworks;
import de.comlineag.snc.crypto.GenericCryptoException;
import de.comlineag.snc.data.PostData;
import de.comlineag.snc.data.UserData;
import de.comlineag.snc.handler.DataCryptoHandler;
import de.comlineag.snc.persistence.HANAPersistence;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.2
 * @status		in development
 * 
 * @description a crawler that gets all files (with a specific name pattern) from a file system 
 * 				directory and hands them over to a persistence manager for storing.
 * 				This specific crawler can be used to retry the saving of posts and users after 
 * 				a persistence failure. 
 * 
 * @changelog	0.1 (Chris)		class created
 * 				0.2				skeleton for parsing and passing over
 */
public class FsCrawler implements Job {
	String fileName = null;
	String JsonBackupStoragePath = "json";
	String fileNamePattern = ".*_fail.json";
    boolean bName = false;
    int allObjectsCount = 0;
    int postObjectsCount = 0;
    int userObjectsCount = 0;
    String networkCode = null;
    String networkName = null;
    PostData pData = new PostData();
    UserData uData = new UserData();
    HANAPersistence hana = new HANAPersistence();
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
		
	// this provides for different encryption provider, the actual one is set in applicationContext.xml 
	private DataCryptoHandler dataCryptoProvider = new DataCryptoHandler();
	
	public FsCrawler() {
		super();
	}
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException{
		logger.info("FileSystem-Crawler START");
		JsonBackupStoragePath = (String) arg0.getJobDetail().getJobDataMap().get("JsonBackupStoragePath");
		fileNamePattern = (String) arg0.getJobDetail().getJobDataMap().get("fileNamePattern");
		
		try {
			File dir = new File(JsonBackupStoragePath);
			File[] files = dir.listFiles();
			
			for (File f : files) {
				fileName = f.getName();
				
				// the FsCrawler shall only process certain files within the directory, for example
				// only failed ones, therefore a pattern is retrieved from the xml configuration file e.g. ".*_fail.json"
				Pattern uName = Pattern.compile(fileNamePattern);
				Matcher mUname = uName.matcher(fileName);
				bName = mUname.matches();
				if (bName) {
					allObjectsCount++;
					
					try {
						parseContent(JsonBackupStoragePath, fileName);
					} catch (ParseException e) {
						logger.warn("could not parse json, skipping file " + fileName);
					}
	            }
	        }
		} catch (IOException e) {
			logger.warn("Could not read file " + fileName + " - " + e.getLocalizedMessage());
		} catch (GenericCryptoException e) {
			logger.error("Could not decrypt content of file " + fileName + " - " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		logger.info("FileSystem-Crawler END - processed "+allObjectsCount+" objects (successfully processed "+postObjectsCount+" post(s) and "+userObjectsCount+" user(s))\n");
	}
	
	
	
	private void parseContent(String savePoint, String fileName) throws FileNotFoundException, IOException, GenericCryptoException, ParseException {
		// macht ein JSon Decode aus dem uebergebenen String
		JSONParser parser = new JSONParser();
		int first = fileName.indexOf("_");
		int second = fileName.indexOf("_", first + 1);
		String entryType = fileName.substring(0, first);
		
		// TODO check why the crawler does not work correctly in case the dataCryptoProvider is activated
		//Object obj = dataCryptoProvider.decryptValue(parser.parse(new FileReader(JsonBackupStoragePath+File.separator+fileName)).toString());
		Object obj = parser.parse(new FileReader(savePoint+File.separator+fileName));
		JSONObject jsonObject = (JSONObject) obj;
		
		networkCode = (String) jsonObject.get("sn_id");
		networkName = SocialNetworks.getSocialNetworkConfigElement("name", networkCode);
		logger.info("initializing "+networkName+" parser for json object");
					
		// currently, the SNC is able to work with two distinct twObject types, users and posts and therefore
		// we should only encounter files for these two types, if not, someone's playing tricks with us.
		if ("post".equals(entryType)) {
			postObjectsCount++;
			logger.debug("working on post " + 
					fileName.substring(first+1, second) );
			logger.trace("   content of file : " + jsonObject.toString());
			
			// now create a new PostData object from the json content of the file
			setPostDataFromJson(jsonObject);
			logger.trace("data type initialized");
				
			// after creating a new PostData object from json content, store it in the persistence layer
			hana.savePosts(pData);
		} else if ("user".equals(entryType)) {
			userObjectsCount++;
			logger.debug("working on user " + 
					fileName.substring(first+1, second) );
			logger.trace("   content of file : " + jsonObject.toString());
			
			// now create a new UserData object from the json content of the file
			setUserDataFromJson(jsonObject);
			logger.trace("data type initialized");
			
			// after creating a new UserData object from json content, store it in the persistence layer
			hana.saveUsers(uData);
		} else {
			logger.warn("unrecognized entry type " + entryType + " encountered - ignoring file " + fileName);
		}
	}
	
	
	
	//
	//
	// BELOW THIS POINT, YOU WILL FIND THE METHODS TO CREATE THE OBJECTS WHICH ARE SEND TO PERSISTENCE MANAGER
	//
	//
	/**
	 * @description		initializes the UserData object from a json object
	 * 
	 * @param 			jsonObject
	 * @return			UserData object
	 */
	private void setUserDataFromJson(JSONObject jsonObject) { 
		logger.debug("creating UserData Object from json file " + fileName);
		/*  
		 * JSON Structure
		 *  {
		 *  	"sn_id":"CC"
		 *  	"user_id":"9",
		 *  	"userName":"Cortal_Consors",
		 *  	"nickName":"Cortal_Consors",
		 *  	"postingsCount":"0",
		 *  	"favoritesCount":"0",
		 *  	"friends":"0",
		 *  	"follower":"0",
		 *  	"userLang":null,
		 *  	"listsAndGroupsCount":"0",
		 *  	"geoLocation":"",

		 *  }
		 */
		if (jsonObject.get("sn_id") != null) {
			logger.trace("    sn_id\t"+jsonObject.get("sn_id").toString());
			uData.setSnId((String) jsonObject.get("sn_id"));
		}
		
		if (jsonObject.get("id") == null) {
			logger.trace("    user_id\t"+jsonObject.get("user_id").toString());
			uData.setId(Long.parseLong(jsonObject.get("user_id").toString()));
		} else {
			logger.trace("setting id\t"+jsonObject.get("id").toString());
			uData.setId(Long.parseLong(jsonObject.get("id").toString()));
		}
		
		if (jsonObject.get("userName") != null) {
			logger.trace("    userName\t"+jsonObject.get("userName").toString());
			uData.setUsername((String) jsonObject.get("userName"));
		}
		
		if (jsonObject.get("nickName") != null) {
			logger.trace("    nickName\t"+jsonObject.get("nickName").toString());
			uData.setScreenName((String) jsonObject.get("nickName"));
		}
		
		if (jsonObject.get("userLang") != null) {
			logger.trace("    userLang\t"+jsonObject.get("userLang").hashCode());
			uData.setLang((String) jsonObject.get("userLang"));
		}
		
		if (jsonObject.get("follower") != null) {
			logger.trace("    follower\t"+Long.parseLong(jsonObject.get("follower").toString()));
			uData.setFollowersCount(Long.parseLong(jsonObject.get("follower").toString()));
		}
		
		if (jsonObject.get("friends") != null) {
			logger.trace("    friends\t"+Long.parseLong(jsonObject.get("friends").toString()));
			uData.setFriendsCount(Long.parseLong(jsonObject.get("friends").toString()));
		}
		
		if (jsonObject.get("postingsCount") != null) {
			logger.trace("    postingsCount\t"+Long.parseLong(jsonObject.get("postingsCount").toString()));
			uData.setPostingsCount(Long.parseLong(jsonObject.get("postingsCount").toString()));
		}
		
		if (jsonObject.get("favoritesCount") != null) {
			logger.trace("    favoritesCount\t"+Long.parseLong(jsonObject.get("favoritesCount").toString()));
			uData.setFavoritesCount(Long.parseLong(jsonObject.get("favoritesCount").toString()));
		}
		
		if (jsonObject.get("listsAndGroupsCount") != null) {
			logger.trace("    listsAndGroupsCount\t"+Long.parseLong(jsonObject.get("listsAndGroupsCount").toString()));
			uData.setListsAndGroupsCount(Long.parseLong(jsonObject.get("listsAndGroupsCount").toString()));
		}
		
		if (jsonObject.get("geoLocation") != null) {
			logger.trace("    geoLocation\t"+jsonObject.get("geoLocation"));
			uData.setGeoLocation((String) jsonObject.get("geoLocation"));
		}
	}
	
	/**
	 * @description		initializes the PostData object from a json object
	 * 
	 * @param 			jsonObject
	 * @return			PostData object
	 */
	private void setPostDataFromJson(JSONObject jsonObject){
		logger.debug("creating PostData Object from json file " + fileName);
		/*
		 * JSON Structure
		 * {
		 * 		"user_id":"9",
				"sn_id":"CC"
				"post_id":"5017",
				"postLang":"de",
				
				"inReplyTo":"0",
				"inReplyToUserID":"0",
				"inReplyToScreenName":"null",
				"client":"\/boards\/id\/Boersenlexikon",
				"timestamp":"2014-01-15T16:14:30.000",
				"favoritecount":"0",
				"viewcount":"74",
				"truncated":"false",
				
				"geoLocation_longitude":"null",
				"geoLocation_latitude":"null",
				"plAround_longitude":"null",
				"plAround_latitude":"null",
				"plCountry":"null",
				"plName":"null",
				"placeID":"null",
				
				"text":"Junge Aktien sind die bei einer Kapitalerh?hung neu ausgegebenen Aktien. Sie sind f?r das laufende Gesch?ftsjahr nicht bzw. noch nicht voll dividendenberechtigt. Nach erster Gewinnaussch?ttung f?llt der Zusatz \"jung\" weg. Die bisherigen Gesellschafter haben einen rechtlichen Anspruch auf den Bezug solcher jungen Aktien, wobei sich der Umfang nach der Zahl ihrer alten Aktien richtet. Gegenteil: alte Aktien",
				"raw_text":"<DIV class=\"lia-message-template-content-zone\">Junge Aktien sind die bei einer Kapitalerh?hung neu ausgegebenen Aktien. Sie sind f?r das laufende Gesch?ftsjahr nicht bzw. noch nicht voll dividendenberechtigt. Nach erster Gewinnaussch?ttung f?llt der Zusatz \"jung\" weg. Die bisherigen Gesellschafter haben einen rechtlichen Anspruch auf den Bezug solcher jungen Aktien, wobei sich der Umfang nach der Zahl ihrer alten Aktien richtet. Gegenteil: alte Aktien\n<\/DIV>",
				"teaser":"Junge Aktien sind die bei einer Kapitalerh?hung neu ausgegebenen Aktien. Sie sind f?r das laufende Gesch?ftsjahr nicht bzw. noch nicht voll dividendenberechtigt. Nach erster Gewinnaussch?ttung f?llt der Zusatz \"jung\" weg. Die bisherigen Gesellschafter h...",
				"subject":"Junge Aktien",
			}
		 */
		if (jsonObject.get("sn_id") != null) {
			logger.trace("    sn_id \t" + jsonObject.get("sn_id").toString());
			pData.setSnId((String) jsonObject.get("sn_id"));
		}
		
		if (jsonObject.get("id") == null) {
			logger.trace("    post_id \t" + Long.parseLong(jsonObject.get("post_id").toString()));
			pData.setId(Long.parseLong(jsonObject.get("post_id").toString()));
		} else {
			logger.trace("    id \t" + jsonObject.get("id").toString());
			pData.setId(Long.parseLong(jsonObject.get("id").toString()));
		}
		
		if (jsonObject.get("postLang") != null) {
			logger.trace("    postLang \t" + jsonObject.get("postLang").toString());
			pData.setLang((String) jsonObject.get("postLang"));
		}
		
		if (jsonObject.get("client") != null) {
			logger.trace("    client \t" + jsonObject.get("client").toString());
			pData.setClient((String) jsonObject.get("client"));
		}
		
		if (jsonObject.get("truncated") != null) {
			logger.trace("    truncated " + jsonObject.get("truncated").toString());
			if ("true".equals(jsonObject.get("truncated")))
				pData.setTruncated((Boolean) true);
			else
				pData.setTruncated((Boolean) false);
		}
		
		if (jsonObject.get("text") != null) {
			logger.trace("    text\t" + jsonObject.get("text").toString().substring(0, 20) + "...");
			pData.setText((String) jsonObject.get("text"));
		}
		
		if (jsonObject.get("raw_text") != null) {
			logger.trace("    raw_text\t" + jsonObject.get("raw_text").toString().substring(0, 20) + "...");
			pData.setRawText((String) jsonObject.get("raw_text"));
		}
		
		if (jsonObject.get("teaser") != null) {
			logger.trace("    teaser\t" + jsonObject.get("teaser").toString());
			pData.setTeaser((String) jsonObject.get("teaser"));
		}
		
		if (jsonObject.get("subject") != null) {
			logger.trace("    subject\t" + jsonObject.get("subject").toString());
			pData.setSubject((String) jsonObject.get("subject"));
		}
		
		// TODO check what source is
		if (jsonObject.get("source") != null){
			logger.trace("    source\t" + jsonObject.get("source").toString());
			pData.setClient((String) jsonObject.get("source"));
		}
		
		if (jsonObject.get("timestamp") != null) {
			String timestring = jsonObject.get("timestamp").toString();
			LocalDateTime timetime = new LocalDateTime(timestring);
			
			logger.trace("    timestamp\t" + timestring);
			pData.setTimestamp((LocalDateTime) timetime);
		}
		
		if (jsonObject.get("inReplyToStatusID") != null) {
			logger.trace("    inReplyToStatusID\t" + Long.parseLong(jsonObject.get("inReplyToStatusID").toString()));
			pData.setInReplyTo(Long.parseLong(jsonObject.get("inReplyToStatusID").toString()));
		}
		
		if (jsonObject.get("inReplyToUserID") != null) {
			logger.trace("    inReplyToUserID\t" + Long.parseLong(jsonObject.get("inReplyToUserID").toString()));
			pData.setInReplyToUser(Long.parseLong(jsonObject.get("inReplyToUserID").toString()));
		}
		
		if (jsonObject.get("inReplyToScreenName") != null) {
			logger.trace("    inReplyToScreenName\t" + jsonObject.get("inReplyToScreenName").toString());
			pData.setInReplyToUserScreenName((String) jsonObject.get("inReplyToScreenName"));
		}
		
		// TODO check how to get geo location elements from json file
		if (jsonObject.get("coordinates") != null) {
			logger.trace("    geoLongitude and geoLatitude");
			pData.setGeoLongitude((String) jsonObject.get("geoLongitude"));
			pData.setGeoLatitude((String) jsonObject.get("geoLatitude"));
		}
		if (jsonObject.get("geoLocation") != null) {
			logger.trace("    geoLongitude and geoLatitude");
			pData.setGeoLongitude((String) jsonObject.get("geoLongitude"));
			pData.setGeoLatitude((String) jsonObject.get("geoLatitude"));
			
			logger.trace("    geoPlace");
			pData.setGeoPlaceId((String) jsonObject.get("geoPlaceId"));
			pData.setGeoPlaceName((String) jsonObject.get("geoPlaceName"));
			pData.setGeoPlaceCountry((String) jsonObject.get("geoPlaceCountry"));
			
			logger.trace("    geoAroundLongitude and geoAroundLatitude");
			pData.setGeoAroundLongitude((String) jsonObject.get("geoAroundLongitude"));
			pData.setGeoAroundLatitude((String) jsonObject.get("geoAroundLongitude"));
			
		}
		
		//setHashtags((List<?>)jsonObject.get("hashtags"));
		//setSymbols((List<?>)jsonObject.get("symbols"));
		//setMentions((List<?>)jsonObject.get("user_mentions"));
	}
}