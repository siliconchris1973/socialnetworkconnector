package de.comlineag.sbm.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 * @author Magnus Leinemann
 * @category Handler
 * 
 * @description generic Implementation for Crawler Job Scheduler
 */
public abstract class GenericCrawler implements Job {

	public GenericCrawler() {
		super();
	}

	// every crawler must implement a method execute
	public abstract void execute(JobExecutionContext arg0) throws JobExecutionException;
}