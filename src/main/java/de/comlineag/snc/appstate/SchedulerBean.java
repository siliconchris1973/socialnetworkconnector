package de.comlineag.snc.appstate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;


/**
 * 
 * @author Christian Guenther
 * 
 *
 */
@ManagedBean(name = "scheduler")
@SessionScoped
public class SchedulerBean implements Serializable {
	
	private static final long serialVersionUID = 1L;
	// Logger Instanz
	private final Logger logger = Logger.getLogger(getClass().getName());

	private Scheduler scheduler;
	
	private List<QuartzJob> quartzJobList = new ArrayList<QuartzJob>();
	
	public SchedulerBean() throws SchedulerException {
		logger.trace("scheduler bean started");
		
		ServletContext servletContext = (ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext();
		
		//Get QuartzInitializerListener 
		StdSchedulerFactory stdSchedulerFactory = (StdSchedulerFactory) servletContext
				.getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
		
		scheduler = stdSchedulerFactory.getScheduler();
		
		// loop jobs by group
		for (String groupName : scheduler.getJobGroupNames()) {
			
			// get jobkey
			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
					.jobGroupEquals(groupName))) {
				
				String jobName = jobKey.getName();
				String jobGroup = jobKey.getGroup();
				
				// get job's trigger
				List<Trigger> triggers = (List<Trigger>) scheduler
						.getTriggersOfJob(jobKey);
				Date nextFireTime = triggers.get(0).getNextFireTime();
				
				logger.trace("found " + jobName + " in " + jobGroup + " scheduled to start " + nextFireTime);
				
				quartzJobList.add(new QuartzJob(jobName, jobGroup, nextFireTime));
			}
		}
	}

	//trigger a job
	public void fireNow(String jobName, String jobGroup) throws SchedulerException {
		JobKey jobKey = new JobKey(jobName, jobGroup);
		scheduler.triggerJob(jobKey);
	}
	
	public List<QuartzJob> getQuartzJobList() {
		return quartzJobList;
	}

	public static class QuartzJob {
		
		private static final long serialVersionUID = 1L;
		
		String jobName;
		String jobGroup;
		Date nextFireTime;
		
		public QuartzJob(String jobName, String jobGroup, Date nextFireTime) {
			this.jobName = jobName;
			this.jobGroup = jobGroup;
			this.nextFireTime = nextFireTime;
		}
		
		public String getJobName() {
			return jobName;
		}
		public void setJobName(String jobName) {
			this.jobName = jobName;
		}
		public String getJobGroup() {
			return jobGroup;
		}
		public void setJobGroup(String jobGroup) {
			this.jobGroup = jobGroup;
		}
		public Date getNextFireTime() {
			return nextFireTime;
		}
		public void setNextFireTime(Date nextFireTime) {
			this.nextFireTime = nextFireTime;
		}
	}
}