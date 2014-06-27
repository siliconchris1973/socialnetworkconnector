package de.comlineag.snc.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 
 * @author 		Christian Guenther
 * @category	Handler
 * @version		0.2
 * 
 * @description	This handler shall be used to implement parallel execution of tasks
 * 
 * @changelog	0.1 copy from the web
 * 				0.2 refactoring from output to GenericExecutorService
 * 				
 */
public class GenericExecutorService {
	public List<GenericExecutorService> processInputs(List<GenericInputType> genericInputTypes)
	        throws InterruptedException, ExecutionException {

	    int threads = Runtime.getRuntime().availableProcessors();
	    ExecutorService service = Executors.newFixedThreadPool(threads);

	    List<Future<GenericExecutorService>> futures = new ArrayList<Future<GenericExecutorService>>();
	    for (final GenericInputType genericInputType : genericInputTypes) {
	        Callable<GenericExecutorService> callable = new Callable<GenericExecutorService>() {
	            public GenericExecutorService call() throws Exception {
	                GenericExecutorService genericExecutorService = new GenericExecutorService();
	                // process your input here and compute the output
	                return genericExecutorService;
	            }
	        };
	        futures.add(service.submit(callable));
	    }

	    service.shutdown();

	    List<GenericExecutorService> genericExecutorServices = new ArrayList<GenericExecutorService>();
	    for (Future<GenericExecutorService> future : futures) {
	        genericExecutorServices.add(future.get());
	    }
	    return genericExecutorServices;
	}
}
