package de.comlineag.sbm.persistence;

import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;

import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

public class HANAPersistence implements IPersistenceManager {
	// Servicelocation
	private String host;
	private String location;
	private String serviceUserEndpoint;
	private String servicePostEndpoint;
	// Credentials
	private String user;
	private String pass;

	private ODataConsumer userService;
	private final Logger logger = Logger.getLogger(getClass().getName());

	public HANAPersistence() {
		logger.debug("HANAPersistence called");

		// ODataConsumer.Builder builder = ODataConsumers.
		// ("http://192.168.131.30:8000/comline/sbm/services/saveUser.xsodata");
		// builder.setClientBehaviors(new
		// BasicAuthenticationBehavior("MLEINEMANN", "Magnus01"));
		// ODataConsumer c = builder.build();

		ODataConsumer.Builder builder = ODataConsumer.newBuilder("http://192.168.131.30:8000/comline/sbm/services/saveUser.xsodata");
		builder.setClientBehaviors(new BasicAuthenticationBehavior("MLEINEMANN", "ABCCCC"));
		userService = builder.build();
		logger.debug("HANAPersistence userService created");

	}

	public void saveUsers(UserData userData) {
		// TODO Auto-generated method stub
		logger.debug("HANAPersistence saveUsers called");

		EdmDataServices serviceMeta;
		try {

			serviceMeta = userService.getMetadata();
			for (int ii = 0; ii < serviceMeta.getSchemas().size(); ii++)
				logger.debug(serviceMeta.getSchemas().get(ii).toString());

			EdmEntitySet entityUser = serviceMeta.getEdmEntitySet("user");
			logger.debug("Entity found: " + entityUser.getName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(e.getMessage(), e);
		}

	}

	public void savePosts(PostData postData) {
		// TODO Auto-generated method stub

	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getServiceUserEndpoint() {
		return serviceUserEndpoint;
	}

	public void setServiceUserEndpoint(String serviceUserEndpoint) {
		this.serviceUserEndpoint = serviceUserEndpoint;
	}

	public String getServicePostEndpoint() {
		return servicePostEndpoint;
	}

	public void setServicePostEndpoint(String servicePostEndpoint) {
		this.servicePostEndpoint = servicePostEndpoint;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

}
