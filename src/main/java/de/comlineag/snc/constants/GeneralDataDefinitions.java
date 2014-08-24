package de.comlineag.snc.constants;

/**
 * 
 * @author 		Christian Guenther
 * @version		0.1a 			- 21.08.2014
 * @status		productive
 * 
 * @description	contains general data definitions for use by all social networks. For example
 * 				the maximum length for a teaser is set to 256 characters and NO markups.
 * 
 * @changelog	0.1 (Chris)		Class created according to DB layout from July, the 10th 2014
 * 				0.1a			configuration elements moved to SNC_Runtime_Configuration.xml 
 * 								in section GeneralDataDefinitions, but code to retrieve values
 * 								not yet implemented
 * 
 * @TODO move this to the runtime configuration
 */
public class GeneralDataDefinitions {
	public static boolean TEASER_WITH_MARKUP = false;
	public static int TEASER_MAX_LENGTH = 256;
	public static int TEASER_MIN_LENGTH = 20;
	
	public static boolean SUBJECT_WITH_MARKUP = false;
	public static int SUBJECT_MAX_LENGTH = 20;
	public static int SUBJECT_MIN_LENGTH = 7;
	
	public static boolean TEXT_WITH_MARKUP = false;
	public static boolean RAW_TEXT_WITH_MARKUP = true;
}
