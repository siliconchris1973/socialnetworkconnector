package de.comlineag.snc.helper;

import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Path;

/**
 * 
 * @author		Christian Guenther
 * @category 	Helper Class
 * @version 	0.1
 * @status		in development 
 * 
 * @description removes all unnecessary or annoying clutter from a webpage and returns
 * 				only the  
 * 				
 * 
 * @changelog	0.1 (Chris)		class created 
 * 
 */
public class UnclutterWebPage {
	
	public UnclutterWebPage() {}
	public UnclutterWebPage(URL url) {}
	public UnclutterWebPage(Path path) {}
	public UnclutterWebPage(FileInputStream file) {}
	
	
	public static String processPage(String inputPage){
		String processedPage = null;
		
		// TODO write code to remove all unnecessary code from a web age and return only pur text content
		
		return processedPage;
	}
}
