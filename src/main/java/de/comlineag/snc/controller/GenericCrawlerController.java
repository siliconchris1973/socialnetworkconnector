package de.comlineag.snc.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author 		Christian Guenther
 * @category 	abstract controller class
 * @version		0.1				- 06.01.2015
 * @status		productive
 *
 * @description This is the abstract controller class for all crawler controller. It provides
 * 				3 methods to work with lock files for crawler controller. 
 *
 * @changelog	0.1 (Chris)		class created
 * 
 */
public abstract class GenericCrawlerController {
	
	/**
	 * @description checks if a given file exists an returns true or false
	 * @param lockFilePath
	 * @return true or false
	 */
	public boolean lockFileExists(String lockFilePath){
		File file = new File(lockFilePath);
	   	if(file.exists())
	   		return true;
	   	else
	   		return false;
	}
	
	/**
	 * @description creates the given lock file and adds the current date and time to it.
	 * @param lockFilePath
	 * @throws IOException
	 */
	public void createLockFile(String lockFilePath) throws IOException {
		File file = new File(lockFilePath);
	   	if(!file.exists()) {
	   		file.createNewFile();
	   	}
	   	
	   	Calendar cal = Calendar.getInstance();
    	cal.getTime();
    	SimpleDateFormat sdf = new SimpleDateFormat("DD.MM.YYYY HH:mm:ss");
    	PrintWriter out = new PrintWriter(lockFilePath);
    	out.print(sdf);
    	out.close();
	}
	
	/**
	 * @description deletes the given file, if it exists
	 * @param lockFilePath
	 */
	public void deleteLockFile(String lockFilePath) {
		File file = new File(lockFilePath);
	   	if(file.exists()) {
	   		file.delete();
        }
	}
}
