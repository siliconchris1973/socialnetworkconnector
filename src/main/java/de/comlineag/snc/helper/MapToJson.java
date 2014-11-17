package de.comlineag.snc.helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;

public class MapToJson {
	JSONObject json = new JSONObject();
	JsonGenerator jGen;
	
	public MapToJson() {
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		Map<String, Object> mapObject = new HashMap<String, Object>();
		
		mapObject.put("domain", "JavaCodeGeeks.com");
		mapObject.put("interest", "Java");
		mapObject.put("Members", 400);
		
		List<Object> myList = new ArrayList<Object>();
		
		myList.add("Jonh");
		myList.add("Jack");
		myList.add("James");
		
		mapObject.put("names", myList);
		
		try {
			objectMapper.writeValue(jGen, mapObject);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
