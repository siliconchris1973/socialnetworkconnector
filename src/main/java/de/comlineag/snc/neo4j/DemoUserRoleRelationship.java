package de.comlineag.snc.neo4j;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;
 
@RelationshipEntity(type = "HAS_ROLE")
public class DemoUserRoleRelationship {
 
	private String description;
	
	@StartNode private DemoUser user;
	
	@EndNode private DemoRole role;
 
	
}