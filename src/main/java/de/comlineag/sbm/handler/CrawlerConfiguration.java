package de.comlineag.sbm.handler;

import java.util.ArrayList;
import de.comlineag.sbm.data.SocialNetworks;

public class CrawlerConfiguration extends GenericConfigurationManager<SocialNetworks> {
	public ArrayList<String> getTrackTerms() {
		return configurationManager.getTrackTerms();
	}
	public ArrayList<String> getTrackLanguages() {
		return configurationManager.getTrackLanguages();
	}
	public ArrayList<String> getTrackSites() {
		return configurationManager.getTrackSites();
	}
	public ArrayList<String> getTrackLocations() {
		return configurationManager.getTrackLocations();
	}
	public ArrayList<String> getTrackUsers() {
		return configurationManager.getTrackUsers();
	}
}
