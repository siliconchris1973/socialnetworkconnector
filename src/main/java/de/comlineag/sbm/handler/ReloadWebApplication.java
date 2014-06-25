package de.comlineag.sbm.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 * @version		0.9 - currently not used
 * 
 * @description	Helper class to restart a given web application on the application server
 *
 */
public class ReloadWebApplication {
	
	public String reloadWebApplication(final String user, final String pwd, final String urlWithParameters, final boolean returnResponse) {
	    URL url = null;
	    try {
	        url = new URL(urlWithParameters);
	    } catch (MalformedURLException e) {
	        System.out.println("MalformedUrlException: " + e.getMessage());
	        e.printStackTrace();
	        return "-1";
	    }

	    URLConnection uc = null;
	    try {
	        uc = url.openConnection();
	    } catch (IOException e) {
	        System.out.println("IOException: " + e.getMessage());
	        e.printStackTrace();
	        return "-12";
	    }


	    String userpass = user + ":" + pwd;
	    String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

	    uc.setRequestProperty("Authorization", basicAuth);
	    InputStream is = null;
	    try {
	        is = uc.getInputStream();
	    } catch (IOException e) {
	        System.out.println("IOException: " + e.getMessage());
	        e.printStackTrace();
	        return "-13";
	    }
	    if (returnResponse) {
	        BufferedReader buffReader = new BufferedReader(new InputStreamReader(is));
	        StringBuffer response = new StringBuffer();

	        String line = null;
	        try {
	            line = buffReader.readLine();
	        } catch (IOException e) {
	            e.printStackTrace();
	            return "-1";
	        }
	        while (line != null) {
	            response.append(line);
	            response.append('\n');
	            try {
	                line = buffReader.readLine();
	            } catch (IOException e) {
	                System.out.println(" IOException: " + e.getMessage());
	                e.printStackTrace();
	                return "-14";
	            }
	        }
	        try {
	            buffReader.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	            return "-15";
	        }
	        System.out.println("Response: " + response.toString());
	        return response.toString();
	    }
	    return "0";
	}
}
