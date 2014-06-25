package de.comlineag.sbm.persistence;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 
 * @author 		Magnus Leinemann
 * @category 	Configuration Manager
 * @version 	1.0
 *  
 * @description provides the application Context with metadata of used backend
 * 
 */

public class ApplicationContextProvider implements ApplicationContextAware {

	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		// Wiring the ApplicationContext into a static method
		AppContext.setApplicationContext(ctx);
	}
}