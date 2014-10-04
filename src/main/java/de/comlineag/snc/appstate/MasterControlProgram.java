package de.comlineag.snc.appstate;

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
 * @description	The Master Control Program (MCP) can be seen as the entry point for job
 * 				and trigger control
 * 
 * @changelog	0.1 (Chris) 	class created
 */
public class MasterControlProgram implements JobFactory {

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
			throws SchedulerException {
		// TODO Auto-generated method stub
		return null;
	}

}
