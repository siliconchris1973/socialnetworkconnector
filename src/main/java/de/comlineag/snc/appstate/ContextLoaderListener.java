package de.comlineag.snc.appstate;

import javax.servlet.ServletContext;

import org.springframework.web.context.WebApplicationContext;

public class ContextLoaderListener extends org.springframework.web.context.ContextLoaderListener{
	
	private static ServletContext SERVLETCONTEXT = null;

	@Override
	public WebApplicationContext initWebApplicationContext(ServletContext args) {
		SERVLETCONTEXT = args;
		return super.initWebApplicationContext(args);
	}
	
	public static ServletContext getServletContext(){
		return SERVLETCONTEXT;
	}
}
