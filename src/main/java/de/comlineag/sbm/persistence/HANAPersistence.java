package de.comlineag.sbm.persistence;

import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;

public class HANAPersistence extends GenericPersistenceManager {

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
		builder.setClientBehaviors(new BasicAuthenticationBehavior("CGUENTHER", "gaga3n+M"));
		userService = builder.build();
		logger.debug("HANAPersistence userService created");

	}

	@Override
	public void saveUsers() {
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

	@Override
	public void savePosts() {
		// TODO Auto-generated method stub

	}

}
