package de.comlineag.sbm.handler;

import java.util.ArrayList;

public class CrawlerConfigurationManager extends GenericConfigurationManager {
	public ArrayList<String> getTrackTerms() {
		return configurationManager.getTrackTerms();
	}
	public ArrayList<String> getTrackLanguages() {
		return configurationManager.getTrackLanguages();
	}
	public ArrayList<String> getTrackUsers() {
		return configurationManager.getTrackUsers();
	}
	public ArrayList<String> getTrackSites() {
		return configurationManager.getTrackSites();
	}
	public ArrayList<String> getTrackLocations() {
		return configurationManager.getTrackLocations();
	}
}
