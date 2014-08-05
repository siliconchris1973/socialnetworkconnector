package de.comlineag.snc.appstate;

import org.springframework.context.ApplicationContext;

/**
 * 
 * @author 		Magnus Leinemann
 * @category	Configuration Manager
 * @version		0.1					- 10.03.2014
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
