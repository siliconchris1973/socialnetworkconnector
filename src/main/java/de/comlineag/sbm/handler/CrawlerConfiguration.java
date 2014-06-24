package de.comlineag.sbm.handler;

import java.util.ArrayList;

import twitter4j.Location;

public class CrawlerConfiguration extends GenericConfigurationManager {
	public ArrayList<String> getTrackTerms() {
		return configurationManager.getTrackTerms();
	}
	public ArrayList<String> getTrackLanguages() {
		return configurationManager.getTrackLanguages();
	}
	public ArrayList<String> getTrackSites() {
		return configurationManager.getTrackSites();
	}
	public ArrayList<Location> getTrackLocations() {
		return configurationManager.getTrackLocations();
	}
}
