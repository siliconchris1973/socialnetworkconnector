package de.comlineag.snc.job;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * 
 * @author 		Christina Guenther
 * @category	job control handler
 * @revision	0.1
 * @status		in development
 * 
 * @description	this class is used to setup the overall configuration of the SNC.
 * 				In this fall the domain driven and/or customer driven configuration for 
 * 				crawler but also runtime configuration options, like whether or not 
 * 				to warn on weak encryption or simple configuration options. 
 * 				It is instantiated by the job control from applicationContext.xml and 
 * 				sets the currently used domain and customer. 
 * 				These values in turn are then accessed by the actual crawler
 * 				and passed to the crawler configuration handler to receive the correct 
 * 				constraints from the crawler configuration.
 * 
 * @changelog	0.1 (Chris) 	class created *
 */
public class MasterControlProgram implements JobFactory {

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
			throws SchedulerException {
		// TODO Auto-generated method stub
		return null;
	}

}
