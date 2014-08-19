package de.comlineag.snc.job;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.handler.DataCryptoHandler;

/**
 * 
 * @author 		Christian Guenther
 * @category 	Job
 * @version		0.1
 * @status		in development
 * 
 * @description a crawler that gets all files (with a specific name pattern) from a file system 
 * 				directory and hands them over to a persistence manager for storing.
 * 				This specific crawler can be used to retry the saving of posts and users after 
 * 				a persistence failure. 
 * 
 * @changelog	0.1 class created
 * 
 */
public class FsCrawler implements Job {
	// define from where the files shall be taken saved
	private String savePoint = RuntimeConfiguration.getJSON_BACKUP_STORAGE_PATH();
	private String objectStatusPriorSaving; // was storing of the object prior saving to disk (e.g. n a db) successful (ok) or not (fail)
	private String objectTypeToSave;		// can either be user or post
	String fileName = null;
    boolean bName = false;
    int iCount = 0;
    
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
		
		int messageCount = 0;
		
		try {
			File dir = new File(savePoint);
	        File[] files = dir.listFiles();
	        logger.trace("List Of Files ::");

	        for (File f : files) {

	            fileName = f.getName();
	            logger.trace("processing file " + fileName);

	            Pattern uName = Pattern.compile(".*_fail.json");
	            Matcher mUname = uName.matcher(fileName);
	            bName = mUname.matches();
	            if (bName) {
	                iCount++;

	            }
	        }
		} catch (Exception e) {
			logger.error("Error while processing messages", e);
		}
		logger.info("FileSystem-Crawler END - processed "+iCount+" messages\n");
	}
}