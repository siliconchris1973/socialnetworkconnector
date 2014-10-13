package de.comlineag.snc.appstate;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;


/**
 * 
 * @author 		Christina Guenther
 * @category	job control handler
 * @revision	0.1
 * @status		in development
 * 
 * @description	The Master Control Program (MCP) can be seen as the entry point for job
 * 				and trigger control. It is started by spring boot and takes control 
 * 				over the job execution
 * 
 * @changelog	0.1 (Chris) 	class created
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class MasterControlProgram implements JobFactory {
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());

	@Bean
	protected ServletContextListener listener() {
		return new ServletContextListener() {

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				logger.info("ServletContext initialized");
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				logger.info("ServletContext destroyed");
			}

		};
	}
	
	
	@RequestMapping("/")
    String home() {
        return "Hello World!";
    }
	
	
	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
			throws SchedulerException {
		// TODO Auto-generated method stub
		return null;
	}
	
	// the main method is started can be started from command line
    public static void main(String[] args) throws Exception {
        SpringApplication.run(MasterControlProgram.class, args);
    }
}
