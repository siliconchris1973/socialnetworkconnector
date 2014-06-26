package de.comlineag.snc.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Output {
	public List<Output> processInputs(List<Input> inputs)
	        throws InterruptedException, ExecutionException {

	    int threads = Runtime.getRuntime().availableProcessors();
	    ExecutorService service = Executors.newFixedThreadPool(threads);

	    List<Future<Output>> futures = new ArrayList<Future<Output>>();
	    for (final Input input : inputs) {
	        Callable<Output> callable = new Callable<Output>() {
	            public Output call() throws Exception {
	                Output output = new Output();
	                // process your input here and compute the output
	                return output;
	            }
	        };
	        futures.add(service.submit(callable));
	    }

	    service.shutdown();

	    List<Output> outputs = new ArrayList<Output>();
	    for (Future<Output> future : futures) {
	        outputs.add(future.get());
	    }
	    return outputs;
	}
}
