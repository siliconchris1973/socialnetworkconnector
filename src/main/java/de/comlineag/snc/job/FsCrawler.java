package de.comlineag.snc.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
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
    boolean bName = false;
    int allObjectsCount = 0;
    int postObjectsCount = 0;
    int userObjectsCount = 0;
    String networkCode = null;
    String networkName = null;
    
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
		String savePoint = (String) arg0.getJobDetail().getJobDataMap().get("savePoint");
		String fileNamePattern = (String) arg0.getJobDetail().getJobDataMap().get("fileNamePattern");
		
		try {
			File dir = new File(savePoint);
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
					
					parseContent(savePoint, fileName);
	            }
	        }
		} catch (IOException e) {
			logger.warn("Could not read file " + fileName);
		} catch (GenericCryptoException e) {
			logger.error("Could not decrypt content of file " + fileName);
			e.printStackTrace();
		} catch (ParseException e) {
			logger.error("EXCEPTION :: could not parse json twObject from file " + fileName);
			e.printStackTrace();
		}
		
		logger.info("FileSystem-Crawler END - processed "+allObjectsCount+" objects (successfully processed "+postObjectsCount+" post(s) and "+userObjectsCount+" user(s))\n");
		//System.exit(-1);
	}
	
	
	
	private void parseContent(String savePoint, String fileName) throws FileNotFoundException, IOException, GenericCryptoException, ParseException {
		// macht ein JSon Decode aus dem uebergebenen String
		JSONParser parser = new JSONParser();
		int first = fileName.indexOf("_");
		int second = fileName.indexOf("_", first + 1);
		String entryType = fileName.substring(0, first);
		
		// TODO check why the crawler does not work correctly in case the dataCryptoProvider is activated
		//Object obj = dataCryptoProvider.decryptValue(parser.parse(new FileReader(savePoint+File.separator+fileName)).toString());
		Object obj = parser.parse(new FileReader(savePoint+File.separator+fileName));
		JSONObject jsonObject = (JSONObject) obj;
		
		networkCode = (String) jsonObject.get("sn_id");
		networkName = SocialNetworks.getSocialNetworkConfigElement("name", networkCode);
		logger.trace("initializing "+networkName+" parser for json object");
					
		// currently, the SNC is able to work with two distinct twObject types, users and posts and therefore
		// we should only encounter files for these two types, if not, someone's playing tricks with us.
		if ("post".equals(entryType)) {
			postObjectsCount++;
			logger.debug("working on post " + 
					fileName.substring(first+1, second) );
			logger.trace("   content of file : " + jsonObject.toString());
			
			// TODO now create a new PostData object from the json content of the file
			//PostData pData = setTwitterPostDataFromJson(jsonObject);
			
			// TODO after creating a new PostData object from json content, store it in the persistence layer
			//HANAPersistence hana = new HANAPersistence();
			//hana.savePosts(pData)
			
		} else if ("user".equals(entryType)) {
			userObjectsCount++;
			logger.debug("working on user " + 
					fileName.substring(first+1, second) );
			logger.trace("   content of file : " + jsonObject.toString());
			
			// TODO now create a new UserData object from the json content of the file
			//UserData uData = setTwitterUserDataFromJson(jsonObject);
			
			// TODO after creating a new UserData object from json content, store it in the persistence layer
			//HANAPersistence hana = new HANAPersistence();
			//hana.savePosts(uData)
		} else {
			logger.warn("unrecognized entry type " + entryType + " encountered - ignoring file " + fileName);
		}
	}
	
	
	
	//
	//
	// BELOW THIS POINT, YOU WILL FIND THE METHODS TO CREATE THE OBJECTS WHICH ARE SEND TO PERSISTENCE MANAGER
	//
	// TODO there must be a better way to store the json files from disk in the persistence manager instead of recreating so much code
	//		imagine, we have like 6 more social networks, that sums up to hundreds of lines of duplicate code here. 
	//
	//
	/**
	 * @description		initializes the PostData object from a json object
	 * 
	 * @param 			jsonObject
	 * @return			UserData object
	 */
	private UserData setTwitterUserDataFromJson(JSONObject jsonObject) { 
		UserData uData = new UserData();
		
		uData.setId((Long) jsonObject.get("id"));
		uData.setUsername((String) jsonObject.get("name"));
		uData.setScreenName((String) jsonObject.get("screen_name"));
		uData.setLang((String) jsonObject.get("lang"));
		if (jsonObject.get("geoLocation") != null)
			uData.setGeoLocation((String) jsonObject.get("geoLocation"));
		if (jsonObject.get("followers_count") != null)
			uData.setFollowersCount((Long) jsonObject.get("followers_count"));
		if (jsonObject.get("friends_count") != null)
			uData.setFriendsCount((Long) jsonObject.get("friends_count"));
		if (jsonObject.get("statuses_count") != null)
			uData.setPostingsCount((Long) jsonObject.get("statuses_count"));
		if (jsonObject.get("favorites_count") != null)
			uData.setFavoritesCount((Long) jsonObject.get("favorites_count"));
		if (jsonObject.get("lists_and_groups_count") != null)
			uData.setListsAndGroupsCount((Long) jsonObject.get("lists_and_groups_count"));
		
		return uData;
	}
	
	/**
	 * @description		initializes the PostData object from a TWITTER json object
	 * 
	 * @param 			jsonObject
	 * @return			PostData object
	 */
	private PostData setTwitterPostDataFromJson(JSONObject jsonObject){
		PostData pData = new PostData();
		
		if (jsonObject.get("id")==null)
			pData.setId((Long) jsonObject.get("post_id"));
		else 
			pData.setId((Long) jsonObject.get("id"));
		pData.setLang((String) jsonObject.get("lang"));
		pData.setTime((String) jsonObject.get("created_at"));
		pData.setTruncated((Boolean) jsonObject.get("truncated"));
		pData.setText((String) jsonObject.get("text"));
		pData.setRawText((String) jsonObject.get("text"));
		pData.setTeaser((String) jsonObject.get("teaser"));
		pData.setSubject((String)jsonObject.get("subject"));
		pData.setClient((String) jsonObject.get("source"));
		if (jsonObject.get("in_reply_to_status_id") != null)
			pData.setInReplyTo((Long) jsonObject.get("in_reply_to_status_id"));
		if (jsonObject.get("in_reply_to_user_id") != null)
			pData.setInReplyToUser((Long) jsonObject.get("in_reply_to_user_id"));
		if (jsonObject.get("in_reply_to_screen_name") != null)
			pData.setInReplyToUserScreenName((String) jsonObject.get("in_reply_to_screen_name"));
		
		// TODO check how to get geo location elements from json file
		if (jsonObject.get("coordinates") != null) {
			/*
			pData.setGeoLongitude((String) jsonObject.get("geoLongitude"));
			pData.setGeoLatitude((String) jsonObject.get("geoLatitude"));
			*/
		}
		if (jsonObject.get("geoLocation") != null) {
			/*
			pData.setGeoLongitude((String) jsonObject.get("geoLongitude"));
			pData.setGeoLatitude((String) jsonObject.get("geoLatitude"));
			pData.setGeoPlaceId((String) jsonObject.get("geoPlaceId"));
			pData.setGeoPlaceName((String) jsonObject.get("geoPlaceName"));
			pData.setGeoPlaceCountry((String) jsonObject.get("geoPlaceCountry"));
			pData.setGeoAroundLongitude((String) jsonObject.get("geoAroundLongitude"));
			pData.setGeoAroundLatitude((String) jsonObject.get("geoAroundLongitude"));
			*/
		}
		//setHashtags((List<?>)jsonObject.get("hashtags"));
		//setSymbols((List<?>)jsonObject.get("symbols"));
		//setMentions((List<?>)jsonObject.get("user_mentions"));
		
		return pData;
	}
}