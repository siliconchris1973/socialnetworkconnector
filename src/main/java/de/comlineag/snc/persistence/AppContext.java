package de.comlineag.snc.persistence;

import org.springframework.context.ApplicationContext;

/**
 * 
 * @author 		Magnus Leinemann
 * @category	Configuration Manager
 * @version		0.1
 * @status		productive
 * 
 * @description defines the application context
 * 
 * @changelog	0.1 (Magnus)		class created
 * 
 */
public class AppContext {

	public static ApplicationContext Context;

	public static void setApplicationContext(ApplicationContext ctx) {
		Context = ctx;
	}

}
