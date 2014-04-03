package de.comlineag.sbm.persistence;

import org.springframework.context.ApplicationContext;

public class AppContext {

	public static ApplicationContext Context;

	public static void setApplicationContext(ApplicationContext ctx) {
		Context = ctx;
	}

}
