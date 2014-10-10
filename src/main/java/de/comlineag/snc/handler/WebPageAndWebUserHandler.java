package de.comlineag.snc.handler;

import java.net.URL;
import java.util.Objects;

import org.json.simple.JSONObject;

import de.comlineag.snc.helper.UniqueIdServices;

public class WebPageAndWebUserHandler {

	
	@SuppressWarnings("unchecked")
	protected JSONObject createPageJsonObject(String title, String description, String page, String text, URL url, Boolean truncated){
		JSONObject pageJson = new JSONObject();
		//truncated = Boolean.parseBoolean("false");
		
		// put some data in the json
		pageJson.put("sn_id", "WC"); // TODO implement proper sn_id handling for websites
		pageJson.put("subject", title);
		pageJson.put("teaser", description);
		pageJson.put("raw_text", page);
		pageJson.put("text", text);
		pageJson.put("source", url.toString());
		pageJson.put("page_id", UniqueIdServices.createId(url.toString()).toString()); // the url is parsed and converted into a long number (returned as a string)
		pageJson.put("lang", "DE"); // TODO implement language recognition
		pageJson.put("truncated", truncated);
		String s = Objects.toString(System.currentTimeMillis(), null);
		pageJson.put("created_at", s);
		pageJson.put("user_id", pageJson.get("page_id"));
		
		JSONObject userJson = new JSONObject();
		userJson.put("sn_id", "WC"); // TODO implement proper sn_id handling for users from websites
		userJson.put("id", pageJson.get("page_id"));
		userJson.put("username", url.getHost());
		
		
		pageJson.put("user", userJson);
		return pageJson;
	}
}
