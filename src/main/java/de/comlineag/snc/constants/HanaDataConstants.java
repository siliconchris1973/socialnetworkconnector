package de.comlineag.snc.constants;
/**
 * 
 * @author 		Christian Guenther
 * @description	contains maximum field length as offered by the HANA database 
 * @version		0.1 	- 10.07.2014
 * @status		productive
 * 
 * @changelog	0.1 (Chris)		Class created according to DB layout from July, the 10th 2014
 * 
 */
public class HanaDataConstants {
	/* table setup for posts on sap hana
	sn_id					NVARCHAR	2
	post_id					NVARCHAR	20
	user_id					NVARCHAR	20
	timestamp				SECONDDATE	19
	postLang				NVARCHAR	64
	text					NVARCHAR	5000
	raw_text				NVARCHAR	5000
	subject					NVARCHAR	20
	teaser					NVARCHAR	256
	favoritecount			BIGINT
	viewcount				BIGINT
	geoLocation_longitude	NVARCHAR	40
	geoLocation_latitude	NVARCHAR	40
	client					NVARCHAR	2048
	truncated				TINYINT
	inReplyTo				NVARCHAR	20
	inReplyToUserID			NVARCHAR	20
	inReplyToScreenName		NVARCHAR	128
	placeID					NVARCHAR	16
	plName					NVARCHAR	256
	plCountry				NVARCHAR	128
	plAround_longitude		NVARCHAR	40
	plAround_latitude		NVARCHAR	40
	*/
	public static final int POSTING_TEXT_SIZE = 5000;
	public static final int SUBJECT_TEXT_SIZE = 20;
	public static final int TEASER_TEXT_SIZE = 256;
	public static final int POSTLANG_TEXT_SIZE = 64;
	public static final int LONGITUDE_TEXT_SIZE = 40;
	public static final int LATITUDE_TEXT_SIZE = 40;
	public static final int CLIENT_TEXT_SIZE = 2048;
	public static final int INREPLYTO_TEXT_SIZE = 20;
	public static final int INREPLYTOSCREENNAME_TEXT_SIZE = 128;
	public static final int PLACEID_TEXT_SIZE = 16;
	public static final int PLNAME_TEXT_SIZE = 256;
	public static final int PLCOUNTRY_TEXT_SIZE = 128;
	
	/* table structure for users on HANA
	 * sn_id				NVARCHAR	2
	 * user_id				NVARCHAR 	20
	 * userName 			NVARCHAR 	128
	 * nickName				NVARCHAR 	128
	 * userLang 			NVARCHAR 	64
	 * geoLocation 			NVARCHAR 	1024
	 * follower 			int
	 * friends				int
	 * postingscount		int
	 * favoritescount		int
	 * listsandgroupscount	int
	 */
	public static final int SNID_TEXT_SIZE = 2;
	public static final int USERID_TEXT_SIZE = 20;
	public static final int USERNAME_TEXT_SIZE = 128;
	public static final int USERNICKNAME_TEXT_SIZE = 128;
	public static final int USERLANG_TEXT_SIZE = 64;
	public static final int USERLOCATION_TEXT_SIZE = 1024;
}
