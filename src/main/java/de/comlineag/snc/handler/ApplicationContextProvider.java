package de.comlineag.snc.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 
 * @author 		Magnus Leinemann
 * @category 	Configuration Manager
 * @version 	0.1			- 10.03.2014
 * @status		productive
 *  
 * @description Wiring the ApplicationContext into a static method and 
 * 				provides the application Context with metadata of used backend
 * 
 * @changelog	0.1 (Magnus)		class created
 * 
 */

public class ApplicationContextProvider implements ApplicationContextAware {

	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		AppContext.setApplicationContext(ctx);
	}
}