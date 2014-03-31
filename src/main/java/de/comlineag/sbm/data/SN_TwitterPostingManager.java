package de.comlineag.sbm.data;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.TweetEntity;
import twitter4j.api.TweetsResources;


/**
 * 
 * @author		Christian Guenther
 * @category	Handler
 *
 * @description	Extends SN_ElementManager for the actual Posting(s). 
 * 				SN_TwitterPostingManager is called after a posting with all relevant information
 * 				is decoded by 
 * 
 */
public class SN_TwitterPostingManager extends SN_ElementManager<SN_TwitterPosting> {
	/*
	 * Die nachfolgenden Elemente des Tweets sollen weiter verarbeitet und gespeichert werden
	 * 
	 * key="cl_postID" 					value="id" 
	 * key="cl_postTime" 				value="created_at" 
	 * key="cl_posting" 				value="text" 
	 * key="cl_postClient" 				value="source" 
	 * key="cl_postTruncated" 			value="truncated" 
	 * key="cl_postInReplyTo" 			value="in_reply_to_status_id" 
	 * key="cl_postInReplyToUserID" 	value="in_reply_to_user_id" 
	 * key="cl_postInReplyToScreenName"	value="in_reply_to_screen_name" 
	 * key="cl_postGeoLocation" 		value="coordinates" 
	 * key="cl_postPlace" 				value="place" 
	 * key="cl_postLang" 				value="lang" 
	 * key="cl_postHashtags" 			value="hashtags" 
	 * key="cl_postSymbols" 			value="symbols" 			
	 * 
	 */
	
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	public SN_TwitterPostingManager() {		
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void save(List<SN_TwitterPosting> posting){
		// log the startup message
		logger.debug("method save from class " + getClass().getName() + " called");

	}	
}
