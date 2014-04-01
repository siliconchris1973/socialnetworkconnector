package de.comlineag.sbm.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public abstract class GenericCrawler implements Job{

	public GenericCrawler() {
		super();
	}
	
	// every crawler must implement a method execute
	public abstract void execute(JobExecutionContext arg0) throws JobExecutionException;
}