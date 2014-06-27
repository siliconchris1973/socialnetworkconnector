package de.comlineag.snc.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 * @author 		Magnus Leinemann
 * @category 	Job
 * @version		1.0
 * 
 * @description abstract definition for a crawler to be executed by the job control
 * 
 * @changelog	1.0 class created
 * 
 */
public abstract class GenericCrawler implements Job {

	public GenericCrawler() {
		super();
	}

	// every crawler must implement a method execute
	public abstract void execute(JobExecutionContext arg0) throws JobExecutionException;
}