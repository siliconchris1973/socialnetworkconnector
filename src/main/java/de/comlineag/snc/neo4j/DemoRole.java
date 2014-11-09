package de.comlineag.snc.neo4j;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
 
@NodeEntity
public class DemoRole {
	
	@GraphId
	private Long id;
	private DemoUser user;
	private Integer role;
	
	
}