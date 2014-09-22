package de.comlineag.snc.job;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;


/**
*
* @author 		Christian Guenther
* @category 	tool
* @version		0.1				- 21.09.2014
* @status		productive
*
* @description 	A demo for a multi-threaded page fetcher. It takes an URL from command line and fetches that page 
* 
* @changelog	0.1 (Chris)		class created as copy http://www.javaworld.com/article/2078809/java-concurrency/java-101-the-next-generation-java-concurrency-without-the-pain-part-1.html?page=2
* 
* TODO adapt SimpleWebCrawler to make use of this same functionality 
*/
public class ReadWebPage {
	public static void main(final String[] args) {
		if (args.length != 1) {
			System.err.println("usage: java ReadWebPage url");
			return;
		}
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Callable<List<String>> callable;
		callable = new Callable<List<String>>() {
			
			@Override
			public List<String> call() throws IOException, MalformedURLException {
				List<String> lines = new ArrayList<>();
				URL url = new URL(args[0]);
				HttpURLConnection con;
				con = (HttpURLConnection) url.openConnection();
				InputStreamReader isr;
				isr = new InputStreamReader(con.getInputStream());
				BufferedReader br;
				br = new BufferedReader(isr);
				String line;
				
				while ((line = br.readLine()) != null)
					lines.add(line);
				
				return lines;
			}
		};
     
		Future<List<String>> future = executor.submit(callable);
		try {
			List<String> lines = future.get(5, TimeUnit.SECONDS);
			for (String line: lines)
				System.out.println(line);
		} catch (ExecutionException ee) {
			System.err.println("Callable through exception: "+ee.getMessage());
		} catch (InterruptedException | TimeoutException eite) {
			System.err.println("URL not responding");
		}
		
		executor.shutdown();
	}
}