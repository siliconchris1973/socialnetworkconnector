package de.comlineag.snc.persistence;

import org.springframework.context.ApplicationContext;

/**
 * 
 * @author 		Magnus Leinemann
 * @category	Configuration Manager
 * @version		1.0
 * 
 * @description defines the application context
 * 
 */
public class AppContext {

	public static ApplicationContext Context;

	public static void setApplicationContext(ApplicationContext ctx) {
		Context = ctx;
	}

}
