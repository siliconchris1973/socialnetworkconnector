package de.comlineag.sbm.persistence;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.BasicAuthenticationBehavior;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;
import org.odata4j.edm.EdmDataServices;

import de.comlineag.sbm.data.PostData;
import de.comlineag.sbm.data.UserData;

public class HANAPersistence implements IPersistenceManager {

	// Servicelocation
	private String host;
	private String port;
	private String protocol;
	private String location;
	private String serviceUserEndpoint;
	private String servicePostEndpoint;
	// Credentials
	private String user;
	private String pass;

	private static ODataConsumer userService;
	private static ODataConsumer postService;

	private final Logger logger = Logger.getLogger(getClass().getName());

	public HANAPersistence() {
		logger.debug("HANAPersistence called");
	}

	public void savePosts(PostData postData) {
		// TODO Auto-generated method stub
		logger.debug("HANAPersistence savePosts called");

		try {
			if (postService == null)
				prepareConnections();

			OEntity newpost = postService.createEntity("post")
					.properties(OProperties.string("sn_id", postData.getSnId()))
					.properties(OProperties.string("post_id", new String(new Long(postData.getId()).toString())))
					.properties(OProperties.string("user_id", new String(new Long(postData.getUserId()).toString())))
					// .properties(OProperties.datetime("timestamp", new LocalDateTime(postData.getTime())))
					.properties(OProperties.string("lang", postData.getLang()))
					.properties(OProperties.string("text", postData.getText()))
					.execute();

			/*
			 * <Property Name="sn_id" Type="Edm.String" Nullable="false" MaxLength="2"/>
			 * <Property Name="post_id" Type="Edm.String" Nullable="false" MaxLength="20"/>
			 * <Property Name="user_id" Type="Edm.String" MaxLength="20"/>
			 * <Property Name="timestamp" Type="Edm.DateTime"/>
			 * <Property Name="lang" Type="Edm.String" MaxLength="64"/>
			 * <Property Name="text" Type="Edm.String" MaxLength="1024"/>
			 * <Property Name="geoLocation.longitude" Type="Edm.String" MaxLength="40"/>
			 * <Property Name="geoLocation.latitude" Type="Edm.String" MaxLength="40"/>
			 * <Property Name="client" Type="Edm.String" MaxLength="2048"/>
			 * <Property Name="truncated" Type="Edm.Int32"/>
			 * <Property Name="inReplyTo" Type="Edm.String" MaxLength="20"/>
			 * <Property Name="inReplyToUserID" Type="Edm.String" MaxLength="20"/>
			 * <Property Name="inReplyToScreenName" Type="Edm.String" MaxLength="128"/>
			 * <Property Name="placeID" Type="Edm.String" MaxLength="16"/>
			 * <Property Name="plName" Type="Edm.String" MaxLength="256"/>
			 * <Property Name="plCountry" Type="Edm.String" MaxLength="128"/>
			 * <Property Name="plAround.longitude" Type="Edm.String" MaxLength="40"/>
			 * <Property Name="plAround.latitude" Type="Edm.String" MaxLength="40"/>
			 */

		} catch (NoBase64EncryptedValue e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(e.getMessage());
		}

	}

	public void saveUsers(UserData userData) {
		// TODO Auto-generated method stub
		logger.debug("HANAPersistence saveUsers called");
		EdmDataServices serviceMeta;

		try {
			if (userService == null)
				prepareConnections();

			OEntity newUser = userService.createEntity("user")
					.properties(OProperties.string("sn_id", userData.getSnId()))
					.properties(OProperties.string("user_id", new String(new Long(userData.getId()).toString())))
					.properties(OProperties.string("name", userData.getUsername()))

					.properties(OProperties.string("nickName", userData.getScreenName()))
					.properties(OProperties.string("lang", userData.getLang()))
					.properties(OProperties.string("location", "default location")) // userData.getLocation().get(0).toString()))

					.properties(OProperties.int32("follower", new Integer((int) userData.getFollowersCount())))
					.properties(OProperties.int32("friends", new Integer((int) userData.getFriendsCount())))
					.properties(OProperties.int32("postingsCount", new Integer((int) userData.getPostingsCount())))
					.properties(OProperties.int32("favoritesCount", new Integer((int) userData.getFavoritesCount())))
					.properties(OProperties.int32("listsAndGroupsCount", new Integer((int) userData.getListsAndGrooupsCount())))

					.execute();

			logger.debug("neuer User " + newUser.getEntityKey().toKeyString());

		} catch (NoBase64EncryptedValue e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(e.getMessage());
		}
	}

	private void prepareConnections() throws NoBase64EncryptedValue {

		logger.debug("Starte prepareConnection");

		String _user = decryptValue(this.user);
		String _pw = decryptValue(this.pass);
		// logger.debug("b64 USER: " + _user);
		// logger.debug("b64 PASS: " + _pw);

		BasicAuthenticationBehavior bAuth = new BasicAuthenticationBehavior(_user, _pw);
		String baseLocation = new String("http://" + this.host + ":" + this.port + this.location);
		String userURI = new String(baseLocation + "/" + this.serviceUserEndpoint);
		String postURI = new String(baseLocation + "/" + this.servicePostEndpoint);

		ODataConsumer.Builder builder = ODataConsumer.newBuilder(userURI);
		builder.setClientBehaviors(bAuth);
		userService = builder.build();

		builder = ODataConsumer.newBuilder(postURI);
		builder.setClientBehaviors(bAuth);
		postService = builder.build();

		logger.debug("HANAPersistence Services connected");

	}

	/**
	 * Entschlüsselt Werte aus der Konfig für die Connection
	 * 
	 * @param param
	 *            der Wert der entschlüsselt werden soll
	 * @return Klartext
	 * 
	 */
	private String decryptValue(String param) throws NoBase64EncryptedValue {

		// byte-Array kommt vom Decoder zurück und kann dann in String übernommen und zurückgegeben werden
		byte[] base64Array;

		// Validierung das auch ein verschlüsselter Wert da angekommen ist
		if (Base64.isBase64(param)) {
			base64Array = Base64.decodeBase64(param.getBytes());
		} else {
			throw new NoBase64EncryptedValue("Parameter " + param + " ist nicht Base64-verschlüsselt");
		}
		// konvertiere in String
		return new String(base64Array);

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

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

}
