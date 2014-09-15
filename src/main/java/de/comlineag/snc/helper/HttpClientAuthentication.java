package de.comlineag.snc.helper;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


/**
 * A simple authenticator that uses HttpClient to execute an HTTP request against
 * a target site that requires user authentication.
 */
@Deprecated
public class HttpClientAuthentication {
	public static void HttpBasicAuthentication(String host, int port, String username, String password) throws Exception {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
					new AuthScope(host, port),
					new UsernamePasswordCredentials(username, password));
			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider)
					.build();
			try {
				HttpGet httpget = new HttpGet("http://"+host+":"+port+"/");
				
				System.out.println("Executing request " + httpget.getRequestLine());
				CloseableHttpResponse response = httpclient.execute(httpget);
				
				try {
					System.out.println("----------------------------------------");
					System.out.println(response.getStatusLine());
					EntityUtils.consume(response.getEntity());
				} finally {
					response.close();
				}
			} finally {
				httpclient.close();
			}
	}
}
