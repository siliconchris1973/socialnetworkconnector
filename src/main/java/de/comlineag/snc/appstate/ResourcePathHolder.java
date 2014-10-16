package de.comlineag.snc.appstate;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

/**
 * 
 * @author 		Christian Guenther
 * @category	handler
 * @revision	0.1				- 16.10.2014
 * @status		productive
 * 
 * @description	this class provides access to the WEB-INF resource folder for any other
 * 				class in the application. It does so by implementing the holder pattern.
 * 				In provides a static field with the real path to the WEB-INF directory.
 * 				
 * 				The class is initialized by application context and Spring gives it a pointer 
 * 				to the ApplicationContext that will be a WebApplicationContext.
 * 				In the init method, the WebApplicationContext is used to get the path and 
 * 				store it in the static field. Thanks to Spring, we can be sure that the 
 * 				method is called once and only once before the application really starts.
 * 
 * 				Now anywhere in the SNC where we need to get the path to the WEB-INF directory, 
 * 				we can call the static getter.
 * 
 * @changelog	0.1 (Chris) 	class created
 * 
 */ 
public class ResourcePathHolder implements ApplicationContextAware, InitializingBean {
	// we use simple org.apache.log4j.Logger for lgging
	private static final Logger logger = Logger.getLogger("de.comlineag.snc.appstate.ResourcePathHolder");
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	private WebApplicationContext wac;
	private static String resourcePath;
	private static ServletContext sc;
	private String path = "/WEB-INF";
	
	public static String getResourcePath() {
		logger.info("resource path is " + resourcePath);
		return resourcePath;
	}
	
	public static ServletContext getServletContext() {
		return sc;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		wac = (WebApplicationContext) applicationContext;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	@Override
	public void afterPropertiesSet() {
		try { 
			ResourcePathHolder.sc = wac.getServletContext();
			ResourcePathHolder.resourcePath = sc.getResource(path).toString();
		} catch (Exception e) {
			logger.error("could not locate resource " + path + " inside servlet container");
		}
	}
}