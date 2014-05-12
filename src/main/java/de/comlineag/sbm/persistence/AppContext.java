package de.comlineag.sbm.persistence;

import org.springframework.context.ApplicationContext;

/**
 * 
 * @author Magnus Leinemann
 * @category Configuration Manager
 * 
 * @description contains the Application Context used to connect to backend
 * @version 1.0
 */
public class AppContext {

	public static ApplicationContext Context;

	public static void setApplicationContext(ApplicationContext ctx) {
		Context = ctx;
	}

}
