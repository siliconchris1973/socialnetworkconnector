package de.comlineag.snc.parser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.json.simple.JSONObject;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.TextExtractor;
import de.comlineag.snc.appstate.RuntimeConfiguration;
import de.comlineag.snc.handler.SimpleWebPosting;
import de.comlineag.snc.helper.UniqueIdServices;



/**
 * 
 * @author 		Christian Guenther
 * @category 	Parser
 * @version		0.9a			- 09.10.2014
 * @status		beta
 * 
 * @description SimpleWebParser is the simplest implementation of the generic web parser for web sites.
 * 				It retrieves a number of words (currently 30) before and after the given track term from
 * 				the site and calls the persistence manager to store the text in the persistence layer
 * 
 * @changelog	0.1 (Chris)		first skeleton
 * 				0.2				try and error with jericho
 * 				0.3				implemented boilerpipe
 * 				0.4				removed boilerpipe
 * 				0.5				rewritten jericho implementation
 * 				0.6				implemented my own parser based on jericho
 * 				0.7				added boolean return value for method parse 
 * 				0.8				removed Wallstreet Online specific implementation 
 *				0.9				implemented productive code to get substring of a page
 *								around the searched track terms 
 *				0.9a			moved helkper methods returnTokenPosition and trimStringAtPosition
 *								into GenericWebParser as it is also neede for other web parser
 * 
 * TODO 1 implement correct threaded parser to aid in multithreading
 * TODO 2 implement language detection (possibly with jroller http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with)
 * 
 */
public final class SimpleWebParser extends GenericWebParser implements IWebParser {
	// this holds a reference to the runtime cinfiguration
	private RuntimeConfiguration rtc = RuntimeConfiguration.getInstance();
	
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	public SimpleWebParser() {}
	// this constructor is used to call the parser in a multi threaded environment
	public SimpleWebParser(String page, URL url, ArrayList<String> tTerms) {
		parse(page, url, tTerms);
	}
	
	
	@Override
	public List<SimpleWebPosting> parse(String page, URL url, List<String> tokens) {
		List<SimpleWebPosting> postings = new ArrayList<SimpleWebPosting>();
		
		// log the startup message
		logger.info("Simple Web parser START for url " + url.toString());
		
		JSONObject parsedPageJson = null;
		try {
			parsedPageJson = extractContent(page, url, tokens);
			SimpleWebPosting parsedPageSimpleWebPosting = new SimpleWebPosting(parsedPageJson);
			
			//logger.trace("PARSED PAGE AS JSON >>> " + parsedPageJson.toString());
			
			// now check if we really really have the searched word within the text and only if so,
			// write the content to disk. We should probably put this before calling the persistence
			if (findNeedleInHaystack(parsedPageJson.toString(), tokens)){
				// add the parsed site to the message list for saving in the DB
				postings.add(parsedPageSimpleWebPosting);
			}
		} catch (Exception e) {
			logger.error("EXCEPTION :: " + e.getMessage() + " " + e);
		}
		
		logger.info("Simple Web parser END\n");
		return postings;
	}
	
	
	
	
	
	
	// START THE SPECIFIC PARSER
	/**
	 * @description	parses a given html-site, removes all header information and extracts 30 words before and after 
	 * 				the track term.
	 * 
	 * @param		page 	- the page to parse as a string containing the html sourcecode
	 * @param		url		- the url to the site
	 * @param		tokens	- a list of tokens we searched for when finding this page
	 * @return		json	- a json object containing the following fields:
	 * 						  text = the plain text of the main content of the site
	 * 						  raw_text = the raw html markup sourcecode
	 * 						  title = the page title
	 * 						  description = the meta tag description of the page
	 * 						  truncated = a boolean field indicating whether the page was truncated or not - usually true
	 * 						  source = the url to the site
	 * 						  created_at = a time value of the millisecond the page was parsed
	 * 						  page_id = a long value created from the url by substituting every character to a number
	 * 						  user_id = 0 
	 */
	protected JSONObject extractContent(String page, URL url, List<String> tokens) {
		logger.debug("parsing site " + url.toString() + " and removing clutter");
		String title = null;
		String description = null;
		String keywords = null;
		String text = null;
		String plainText = null;
		boolean truncated = Boolean.parseBoolean("false");
		
		// vars for the token extraction
		int lowBorderMark = 300;
		int highBorderMark = 300;
		ArrayList<Integer> positions = new ArrayList<Integer>();
		
		try {
			// if so, get the plain text with
			Source source = new Source(page);
			source.fullSequentialParse();
			TextExtractor genericSiteTextExtractor = new TextExtractor(source) {
				public boolean excludeElement(StartTag startTag) {
					return startTag.getName()==HTMLElementName.TITLE
							|| startTag.getName()==HTMLElementName.THEAD
							|| startTag.getName()==HTMLElementName.SCRIPT
							|| startTag.getName()==HTMLElementName.HEAD
							|| startTag.getName()==HTMLElementName.META;
				}
			};
			
			
			plainText = genericSiteTextExtractor.setIncludeAttributes(true).toString();
			//String plainText = aBigPlainText();
			title = getTitle(source);
			description = getMetaValue(source, "Description");
			keywords = getMetaValue(source, "keywords");
			
			
			/* uses a combination of returnTokenPosition and trimStringAtPosition
			 *   - works! returns exactly one string if the words are near to each other
			 *     and multiple concatinated strings in case the words are far apart
			 */      
			for (int i=0; i < tokens.size(); i++) {
				String token = tokens.get(i);
				logger.trace("working on token " + token);
				
				positions = returnTokenPosition(plainText, token, positions, lowBorderMark, highBorderMark);
			}
			
			String segmentText = "";
			for (int i=0;i<positions.size();i++) {
				int position = positions.get(i);
				segmentText += trimStringAtPosition(plainText, position, 
													rtc.getWC_WORD_DISTANCE_CUTOFF_MARGIN(), 
													rtc.getWC_WORD_DISTANCE_CUTOFF_MARGIN());
			}
			//logger.trace("TruncatedText: >>> " + segmentText);
			
			// now put the reduced text in the original text variable, so that it gets added to the json below
			text = segmentText;
			// and also make sure that the truncated flag is set correctly
			//logger.trace("plainText was " + plainText.length() + " and segmentText is " + segmentText.length());
			if (plainText.length() > segmentText.length()) {
				truncated = Boolean.parseBoolean("true");
			} else {
				truncated = Boolean.parseBoolean("false");
			} 
				
			
		} catch (Exception e) {
			logger.error("EXCEPTION :: error during parsing of site content ", e );
			e.printStackTrace();
		}
		
		JSONObject pageJson = createPageJsonObject(title, description, plainText, text, url, truncated);
		return pageJson;
	}
	
	
	// START OF JERICHO SPECIFIC PARSER STUFF
	private static String getTitle(Source source) {
		Element titleElement=source.getFirstElement(HTMLElementName.TITLE);
		if (titleElement==null) return null;
		// TITLE element never contains other tags so just decode it collapsing whitespace:
		return CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
	}
	private static String getMetaValue(Source source, String key) {
		for (int pos=0; pos<source.length();) {
			StartTag startTag=source.getNextStartTag(pos,"name",key,false);
			if (startTag==null) return null;
			if (startTag.getName()==HTMLElementName.META)
				return startTag.getAttributeValue("content"); // Attribute values are automatically decoded
			pos=startTag.getEnd();
		}
		return null;
	}
	// END OF JERICHO PARSER SPECIFIC STUFF
	
	
	
	
	
	
	
	
	@Override
	public Object execute(String page, URL url) {
		// TODO implement execute method in SompleWebParser to make it thread save
		return null;
	}
	
	@Override
	public boolean canExecute(String page, URL url) {
		// the simple parser will work on any page - thus returning true
		return true;
	}
	
	@Override
	protected Boolean parse(String page) {logger.warn("method not impleented");return false;}
	@Override
	protected Boolean parse(InputStream is) {logger.warn("method not impleented");return false;}
	
	

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
		userJson.put("name", url.getHost());
		userJson.put("screen_name", url.getHost());
		userJson.put("lang", "DE"); // TODO implement language recognition
		
		
		pageJson.put("user", userJson);
		
		logger.trace("the json object:: " + pageJson.toJSONString());
		return pageJson;
	}
	
	
	// this is just a debug method. I use it to create a very long text to test the regular expression and position
	// finding methods
	private String aBigPlainText() {
		
		String bigtext = "Ladies others the six desire age. Bred am soon park past read by lain. As excuse eldest no moment. An delight beloved up garrets am cottage private. The far attachment discovered celebrated decisively Cortal surrounded for and. Sir new the particular frequently indulgence excellence how. Wishing an if he Consors sixteen visited tedious subject it. Mind mrs yet did quit high even you went. Sex against the cortal consors two however not nothing prudent colonel greater. Up husband removed parties staying he subject mr. At every tiled on ye defer do. No attention suspected oh difficult Cortal consors. Fond his say old meet cold find come whom. The sir park sake bred. Wonder matter now can estate esteem assure fat roused. Am performed on existence as discourse is. Pleasure friendly at marriage blessing or. Little afraid its eat looked now. Very ye lady girl them good me make. It hardly cousin me always. An Cortal shortly village is raising we shewing replied. She the favourable partiality inhabiting travelling impression put two. His six are entreaties instrument acceptance unsatiable her. Amongst as or on herself chapter entered carried no. Sold old ten are quit lose deal his sent. You correct how sex several far distant believe journey parties. We shyness enquire uncivil affixed it carried to. An country demesne message it. Bachelor domestic extended doubtful as Consors concerns Cortal at. Morning prudent removal an letters by. On could my in order never it. Or excited certain sixteen it to parties colonel. Depending conveying direction has led immediate. Law gate her well bed life feet seen rent. On nature or no except it sussex. Sussex result matter any end see. It Cortal consors speedily me addition weddings vicinity in pleasure. Happiness commanded an conveying breakfast in. Regard her say warmly elinor. Him these are visit front end for seven walls. Money eat scale now ask law learn. Side its they just any upon see last. He prepared no shutters perceive do greatest. Ye at unpleasant solicitude in companions interested. Give lady of they such they sure it. Me contained explained my education. Vulgar as hearts by garret. Perceived determine departure explained no forfeited he something an. Contrasted dissimilar get joy you instrument out reasonably. Again keeps at no meant stuff. To perpetual do existence northward as difficult preserved daughters. Continued at up to zealously necessary breakfast. Surrounded sir moti cortal onless she end literature. Gay direction neglected but supported yet her. consors Denote simple fat denied add worthy little use. As some he so high down am week. Conduct esteems by cottage to pasture we winding. On assistance he cultivated considered frequently. Person how having tended direct own day man. Saw sufficient indulgence one own you inquietude sympathize. Sudden looked elinor off gay estate nor silent. Son read such next see the rest two. Was use extent old entire sussex. Curiosity remaining own see repulsive household advantage son additions. Supposing exquisite daughters eagerness why repulsive for. Praise turned it lovers be warmly by. Little do it eldest former be if. Barton waited twenty always repair in within we do. An delighted offending curiosity my is dashwoods at. Boy prosperous increasing surrounded companions her nor advantages sufficient put. John on time down give meet help as of. Him waiting and correct believe now cottage she another. Vexed six shy yet along learn maids her tiled. Through studied shyness evening bed him winding present. Become excuse hardly on my thirty it wanted. Answer misery adieus add wooded how nay men before though. Pretended belonging contented mrs suffering favourite you the continual. Mrs civil nay least means tried drift. Natural end law whether but and towards certain. Furnished unfeeling his sometimes see day promotion. Quitting informed concerns can men now. Projection to or up conviction uncommonly delightful continuing. In appetite ecstatic opinions hastened by handsome admitted.  cortalconsors Ich sagen genie macht klare augen tag. Darin stube mu da litze zu indes wu. Pa he gepfiffen hochstens dunkelrot kammertur. Man nur gefallt die frieden erstieg dunklem gut. Schuchtern da dienstmagd bangigkeit pa he ja freundlich. Schuftet en doppelte em brauchst schweren am eigentum gespielt. Hab sei zog hubsche manchen schurze besorgt obenhin bosheit ach. Mir stockwerk schwemmen her des kraftiger zur. Hatt wu eben er ging du bart hell du. Nur weibern speisen schaute frieden brachte bat. Mehrere stiefel gelegen lag tod oha traurig die. Weil sa se sein ward darf. In brotlose an unbeirrt du behutsam schlafen heiraten funkelte. Aussah all bis solche sitzen gro volles fallen. Halblaut erzahlte wu zu funkelte gefunden eleonora gefallts um. Mi sauber drehte la schlug. Was sag lustig des unterm madele zeigen ich diesem. Ja sprach gehabt wirtin werden wo bi ja. Alten uns kam kerze zur kinde das herrn leise. Gefunden geholfen gesichts bi so jahrlich hubschen. Vertreiben gab kam wei getunchten federdecke dienstmagd hausdacher. Oha hut hatt ist wege der nein. So wu sparlich ansprach doppelte. An herrn te um szene mager hause danke. Dichten instand da schritt traurig ku. Du gerbers fu beschlo trocken. En hinabsah so gedanken wo gedichte burschen. Angenommen erkundigte vertreiben grundstuck abendsuppe aus ach wei wie. Man dabei kalte ahren hof. Te da leuchter gekommen leichter verlohnt. Frieden he gefallt so brachte kindern argerte pa. Eia tod erstaunt nochmals feinheit blo gespielt. Vor schwachen betrubtes abstellte den stuckchen behaglich hellroten. Wie ein befehlen geschirr funkelte gut. Sah husten ein besser musset lauter kochen weiter war. Lustig luften mir redete ich winter man das begann. Wasser regnet sto herein gerade gar. Heut ach fein sind vers ware brot die dir. Ertastete ab schlupfte wunderbar er ausdenken. Befangenen es mu geheiratet vorpfeifen. Weiterhin verodeten zufrieden hochstens zu hemdarmel kellnerin im. Feucht ei ab gelben machte sorgen er. Warmer zu himmel brauen fehlts fu. Stiege ungern heraus see sommer ich ers was schwer fallen. Muhlenrad wachsamen bewirtung nun vermodert ausgeruht ans aus verodeten. Tur horte wei herum habet blies leise. Gerufen sie das namlich was ich argerte sonntag. Schonsten geheimnis dem ertastete das wie aufraumen. Madchens gefallen arbeiter ehe gru die. Hufschmied he zu so schuchtern drechslers grundstuck kuchenture. Es gebracht ziemlich brauchte tadellos ku ratloses gelaufig. Nichtstun an geschickt studieren so bewirtung. Wasserkrug bi kindlichen ri frohlicher zu erhaltenen. Das trostlos allerlei konntest zwischen ein blo. Dort ich eile zaun das acht voll. Je wo es darf dies wohl wird ware. Ruhig still ihn indes ach ten ihren gutes. Reinlich wo bezahlen schlafer ja du schaffte so halbwegs. Du wo wind arme nein aber rief ubel he wo. Seid gebe zu ja tate orte alle. Sie indes war einem nun ihn alten. Haben laune feuer he du wills. Je du schones dunklen mehrere lebhaft stimmts so. Gerbers da taghell offnung namlich da. Haar tat ehre dich habt dort man dus see. Leuchtete teilnahme ei plaudernd es lieblinge schneider. Langweilig nachmittag vielleicht la ab betrachtet. Te bist ihre gast sa chen wahr ists. Oder sich tag habe etwa erde wei dame. Da mitreden da erschrak hausherr ri manchmal en gefallen. Mehr gibt habt es fu haar ja fein. Her daran getan ort zog weich alles ein nicht spiel. Nachtessen dienstmagd vor vielleicht fur sonderling ist leuchtturm. Er nachtun dunkeln wimpern ku. Befehlen leichter horchend fur hinunter bei ton. Behaglich schnupfen all erstaunen verlangst gru. Fast ware fand ich bist seid flo geht vor. Erkundigte das achthausen hei nachmittag werkstatte vor. Freund und erfuhr fur wurden. Ich sagen genie macht klare augen tag. Darin stube mu da litze zu indes wu. Pa he gepfiffen hochstens dunkelrot kammertur. Man nur gefallt die frieden erstieg dunklem gut. Schuchtern da dienstmagd bangigkeit pa he ja freundlich. Schuftet en doppelte em brauchst schweren am eigentum gespielt. Hab sei zog hubsche manchen schurze besorgt obenhin bosheit ach. Mir stockwerk schwemmen her des kraftiger zur. Hatt wu eben er ging du bart hell du. ur weibern speisen schaute frieden brachte bat. Mehrere stiefel gelegen lag tod oha traurig die. Weil sa se sein ward darf. In brotlose an unbeirrt du behutsam schlafen heiraten funkelte. Aussah all bis solche sitzen gro volles fallen. Halblaut erzahlte wu zu funkelte gefunden eleonora gefallts um. Mi sauber drehte la schlug. Was sag lustig des unterm madele zeigen ich diesem. Ja sprach gehabt wirtin werden wo bi ja. Alten uns kam kerze zur kinde das herrn leise. Gefunden geholfen gesichts bi so jahrlich hubschen. Vertreiben gab kam wei getunchten federdecke dienstmagd hausdacher. Oha hut hatt ist wege der nein. So wu sparlich ansprach doppelte. An herrn te um szene mager hause danke. Dichten instand da schritt traurig ku. Du gerbers fu beschlo trocken. En hinabsah so gedanken wo gedichte burschen. Angenommen erkundigte vertreiben grundstuck abendsuppe aus ach wei wie. Man dabei kalte ahren hof. Te da leuchter gekommen leichter verlohnt. Frieden he gefallt so brachte kindern argerte pa. Eia tod erstaunt nochmals feinheit blo gespielt. Vor schwachen betrubtes abstellte den stuckchen behaglich hellroten. Wie ein befehlen geschirr funkelte gut. cORTAL Sah husten ein besser musset lauter kochen weiter war. Lustig luften mir redete ich winter man das begann. Wasser regnet sto herein gerade gar. Heut ach fein sind vers ware brot die dir. Ertastete ab schlupfte wunderbar er ausdenken. Befangenen es mu geheiratet vorpfeifen. Weiterhin verodeten zufrieden hochstens zu hemdarmel kellnerin im. Feucht ei ab gelben machte sorgen er. Warmer zu himmel brauen fehlts fu. Stiege ungern heraus see sommer ich ers was schwer fallen. Muhlenrad wachsamen bewirtung nun vermodert ausgeruht ans aus verodeten. Tur horte wei herum habet blies leise. Gerufen sie das namlich was ich argerte sonntag. Schonsten geheimnis dem ertastete das wie aufraumen. Madchens gefallen arbeiter ehe gru die. Hufschmied he zu so schuchtern drechslers grundstuck kuchenture. Es gebracht ziemlich brauchte tadellos ku ratloses gelaufig. consors Nichtstun an geschickt studieren so bewirtung. Wasserkrug bi kindlichen ri frohlicher zu erhaltenen. Das trostlos allerlei konntest zwischen ein blo. Dort ich eile zaun das acht voll. Je wo es darf dies wohl wird ware. Ruhig still ihn indes ach ten ihren gutes. Reinlich wo bezahlen schlafer ja du schaffte so halbwegs. Du wo wind arme nein aber rief ubel he wo. Seid gebe zu ja tate orte alle. Sie indes war einem nun ihn alten. Haben laune feuer he du wills. Je du schones dunklen mehrere lebhaft stimmts so. Gerbers da taghell offnung namlich da. Haar tat ehre dich habt dort man dus see. Leuchtete teilnahme ei plaudernd es lieblinge schneider. Langweilig nachmittag vielleicht la ab betrachtet. Te bist ihre gast sa chen wahr ists. Oder sich tag habe etwa erde wei dame. Da mitreden da erschrak hausherr ri manchmal en gefallen. Mehr gibt habt es fu haar ja fein. Her daran getan ort zog weich alles ein nicht spiel. Nachtessen dienstmagd vor vielleicht fur sonderling ist leuchtturm.Er nachtun dunkeln wimpern ku. Befehlen leichter horchend fur hinunter bei ton. Behaglich schnupfen all erstaunen verlangst gru. Fast ware fand ich bist seid flo geht vor. Erkundigte das achthausen hei nachmittag werkstatte vor. Freund und erfuhr fur wurden.Ich sagen genie macht klare augen tag. Darin stube mu da litze zu indes wu. Pa he gepfiffen hochstens dunkelrot kammertur. Man nur gefallt die frieden erstieg dunklem gut. Schuchtern da dienstmagd bangigkeit pa he ja freundlich. Schuftet en doppelte em brauchst schweren am eigentum gespielt. Hab sei zog hubsche manchen schurze besorgt obenhin bosheit ach. Mir stockwerk schwemmen her des kraftiger zur. Hatt wu eben er ging du bart hell du.Nur weibern speisen cortal schaute frieden brachte bat. Mehrere stiefel gelegen lag tod oha traurig die. Weil sa se sein ward darf. In brotlose an unbeirrt du behutsam schlafen heiraten funkelte. Aussah all bis solche sitzen gro volles fallen. Halblaut erzahlte wu zu funkelte gefunden eleonora gefallts um. Mi sauber drehte la schlug. Was sag lustig des unterm madele zeigen ich diesem. Ja sprach gehabt wirtin werden wo bi ja. Alten uns kam kerze zur kinde das herrn leise.Gefunden geholfen gesichts bi so jahrlich hubschen. Vertreiben gab kam wei getunchten federdecke dienstmagd hausdacher. Oha hut hatt ist wege der nein. So wu sparlich ansprach doppelte. An herrn te um szene mager hause danke. Dichten instand da schritt traurig ku. Du gerbers fu beschlo trocken. En hinabsah so gedanken wo gedichte burschen.Angenommen erkundigte vertreiben grundstuck abendsuppe aus ach wei wie. Man dabei kalte ahren hof. Te da leuchter gekommen leichter verlohnt. Frieden he gefallt so brachte kindern argerte pa. Eia tod erstaunt nochmals feinheit blo gespielt. Vor schwachen betrubtes abstellte den stuckchen behaglich hellroten. Wie ein befehlen geschirr funkelte gut.Sah husten ein besser musset lauter kochen weiter war. Lusg luften mir redete ich winter man das begann. Wasser regnet sto herein gerade gar. Heut ach fein sind vers ware brot die dir. Ertastete ab schlupfte wunderbar er ausdenken. Befangenen es mu geheiratet vorpfeifen. Weiterhin verodeten zufrieden hochstens zu hemdarmel kellnerin im. Feucht ei ab gelben machte sorgen er. Warmer zu himmel brauen fehlts fu. Stiege ungern heraus see sommer ich ers was schwer fallen. Muhlenrad wachsamen bewirtung nun vermodert ausgeruht ans aus verodeten. Tur horte wei herum habet blies leise. Gerufen sie das namlich was ich argerte sonntag. Schonsten geheimnis dem ertastete das wie aufraumen. Madchens gefallen arbeiter ehe gru die. Hufschmied he zu so schuchtern drechslers grundstuck kuchenture. Es gebracht ziemlich brauchte tadellos ku ratloses gelaufig. Nichtstun an geschickt studieren so bewirtung. Wasserkrug bi kindlichen ri frohlicher zu erhaltenen. Das trostlos allerlei konntest zwischen ein blo. Dort ich eile zaun das acht voll. Je wo es darf dies wohl wird ware. Ruhig still ihn indes ach ten ihren gutes. Reinlich wo bezahlen schlafer ja du schaffte so halbwegs. Du wo wind arme nein aber rief ubel he wo. Seid gebe zu ja tate orte alle. Sie indes war einem nun ihn alten. Haben laune feuer he du wills. Je du schones dunklen mehrere lebhaft stimmts so. Gerbers da taghell offnung namlich da. Haar tat ehre dich habt dort man dus see. Leuchtete teilnahme ei plaudernd es lieblinge schneider. Langweilig nachmittag vielleicht la ab betrachtet. Te bist ihre gast sa chen wahr ists. Oder sich tag habe etwa erde wei dame. Da mitreden da erschrak hausherr ri manchmal en gefallen. Mehr gibt habt es fu haar ja fein. Her daran getan ort zog weich alles ein nicht spiel. Nachtessen dienstmagd vor vielleicht fur sonderling ist leuchtturm. Er nachtun dunkeln wimpern ku. Befehlen leichter horchend fur hinunter bei ton. Behaglich Cortal Consors hnupfen all erstaunen verlangst gru. Fast ware fand ich bist seid flo geht vor. Erkundigte das achthausen hei nachmittag werkstatte vor. Freund und erfuhr fur wurden. Ich sagen genie macht klare augen tag. Darin stube mu da litze zu indes wu. Pa he gepfiffen hochstens dunkelrot kammertur. Man nur gefallt die frieden erstieg dunklem gut. Schuchtern da dienstmagd bangigkeit pa he ja freundlich. Schuftet en doppelte em brauchst schweren am eigentum gespielt. Hab sei zog hubsche manchen schurze besorgt obenhin bosheit ach. Mir stockwerk schwemmen her des kraftiger zur. Hatt wu eben er ging du bart hell du. Nur weibern speisen schaute frieden brachte bat. Mehrere stiefel gelegen lag tod oha traurig die. Weil sa se sein ward darf. In brotlose an unbeirrt du behutsam schlafen heiraten funkelte. Aussah all bis solche sitzen gro volles fallen. Halblaut erzahlte wu zu funkelte gefunden eleonora gefallts um. Mi sauber drehte la schlug. Was sag lustig des unterm madele zeigen ich diesem. Ja sprach gehabt wirtin werden wo bi ja. Alten uns kam kerze zur kinde das herrn leise. Gefunden geholfen gesichts bi so jahrlich hubschen. Vertreiben gab kam wei getunchten federdecke dienstmagd hausdacher. Oha hut hatt ist wege der nein. So wu sparlich ansprach doppelte. An herrn te um szene mager hause danke. Dichten instand da schritt traurig ku. Du gerbers fu beschlo trocken. En hinabsah so gedanken wo gedichte burschen. Angenommen erkundigte vertreiben grundstuck abendsuppe aus ach wei wie. Man dabei kalte ahren hof. Te da leuchter gekommen leichter verlohnt. Frieden he gefallt so brachte kindern argerte pa. Eia tod erstaunt nochmals feinheit blo gespielt. Vor schwachen betrubtes abstellte den stuckchen behaglich hellroten. Wie ein befehlen geschirr funkelte gut. Sah husten ein besser musset lauter kochen weiter war. Lustig luften mir redete ich winter man das begann. Wasser regnet sto herein gerade gar. Heut ach fein sind vers ware brot die dir. Ertastete ab schlupfte wunderbar er ausdenken. Befangenen es mu geheiratet vorpfeifen. Weiterhin verodeten zufrieden hochstens zu hemdarmel kellnerin im. Feucht ei ab gelben machte sorgen er. Warmer zu himmel brauen fehlts fu. Stiege ungern heraus see sommer ich ers was schwer fallen. Muhlenrad wachsamen bewirtung nun vermodert ausgeruht ans aus verodeten. Tur horte wei herum habet blies leise. Gerufen sie das namlich was ich argerte sonntag. Schonsten geheimnis dem ertastete das wie aufraumen. Madchens gefallen arbeiter ehe gru die. Hufschmied he zu so schuchtern drechslers grundstuck kuchenture. Es gebracht ziemlich brauchte tadellos ku ratloses gelaufig.Nichtstun an geschickt studieren so bewirtung. Wasserkrug bi kindlichen ri frohlicr zu erhaltenen. Das trostlos allerlei konntest zwischen ein blo. Dort ich eile zaun das acht voll. Je wo es darf dies wohl wird ware. Ruhig still ihn indes ach ten ihren gutes. Reinlich wo bezahlen schlafer ja du schaffte so halbwegs. Du wo wind arme nein aber rief ubel he wo. Seid gebe zu ja tate orte alle. Sie indes war einem n ihn alten. Haben laune feuer he du wills. Je du schones dunklen mehrere lebhaft stimmts so. Gerbers da taghell offnung namlich da. Haar tat ehre dich habt dort man dus see. Leuchtete teilnahme ei plaudernd es lieblinge schneider. Langweilig nachmittag vielleicht la ab betrachtet. Te bist ihre gast sa chen wahr ists. Oder sich tag habe etwa erde wei dame. Da mitreden da erschrak hausherr ri manchmal en gefallen. Mehr gibt habt es fu haar ja fein. Her daran getan ort zog weich alles ein nicht spiel. Nachtessen dienstmagd vor vielleicht fur sonderling ist leuchtturm. Er nachtun dunkeln wimpern ku. Befehlen leichter horchend fur hinunter bei ton. Behaglich schnupfen all erstaunen verlangst gru. Fast ware fand ich bist seid flo geht vor. Erkundigte das achthausen hei nachmittag werkstatte vor. Freund und erfuhr fur wurden. Ich sagen genie macht klare augen tag. Darin stube mu da litze zu indes wu. Pa he gepfiffen hochstens dunkelrot kammertur. Man nur gefallt die frieden erstieg dunklem gut. Schuchtern da dienstmagd bangigkeit pa he ja freundlich. Schuftet en doppelte em brauchst schweren am eigentum gespielt. Hab sei zog hubsche manchen schurze besorgt obenhin bosheit ach. Mir stockwerk schwemmen her des kraftiger zur. Hatt wu eben er ging du bart hell du. Nur weibern speisen schaute frieden brachte bat. Mehrere stiefel gelegen lag tod oha traurig die. Weil sa se sein ward darf. In brotlose an unbeirrt du behutsam schlafen heiraten funkelte. Aussah all bis solche sitzen gro volles fallen. Halblaut erzahlte wu zu funkelte gefunden eleonora gefallts um. Mi sauber drehte la schlug. Was sag lustig des unterm madele zeigen ich diesem. Ja sprach gehabt wirtin werden wo bi ja. Alten uns kam kerze zur kinde das herrn leise. Gefunden geholfen gesichts bi so jahrlich hubschen. Vertreiben gab kam wei getunchten federdecke dienstmagd hausdacher. Oha hut hatt ist wege der nein. So wu sparlich ansprach doppelte. An herrn te um szene mager hause danke. Dichten instand da schritt traurig ku. Du gerbers fu beschlo trocken. En hinabsah so gedanken wo gedichte burschen. Angenommen erkundigte vertreiben grundstuck abendsuppe aus ach wei wie. Man dabei kalte ahren hof. Te da leuchter gekommen leichter verlohnt. Frieden he gefallt so brachte kindern argerte pa. Eia tod erstaunt nochmals feinheit blo gespielt. Vor schwachen betrubtes abstellte den stuckchen behaglich hellroten. Wie ein befehlen geschirr funkelte gut. Sah husten ein besser musset lauter kochen weiter war. Lustig luften mir redete ich winter man das begann. Wasser regnet sto herein gerade gar. Heut ach fein sind vers ware brot die dir. Ertastete ab schlupfte wunderbar er ausdenken. Befangenen es mu geheiratet vorpfeifen. Weiterhin verodeten zufrieden hochstens zu hemdarmel kellnerin im. Feucht ei ab gelben machte sorgen er. Warmer zu himmel brauen fehlts fu. tiege ungern heraus see sommer ich ers was schwer fallen. Muhlenrad wachsamen bewirtung nun vermodert ausgeruht ans aus verodeten. Tur horte wei herum habet blies leise. Gerufen sie das namlich was ich argerte sonntag. Schonsten geheimnis dem ertastete das wie aufraumen. Madchens gefallen arbeiter ehe gru die. Hufschmied he zu so schuchtern drechslers grundstuck kuchenture. Es gebracht ziemlich brauchte tadellos ku ratloses gelaufig. Nichtstun an geschickt studieren so bewirtung. Wasserkrug bi kindlichen ri frohlicher zu erhaltenen. Das trostlos allerlei konntest zwischen ein blo. Dort ich eile zaun das acht voll. Je wo es darf dies wohl wird ware. Ruhig still ihn indes ach ten ihren gutes. Reinlich wo bezahlen schlafer ja du schaffte so halbwegs. Du wo wind arme nein aber rief ubel he wo. Seid gebe zu ja tate orte alle. Sie indes war einem nun ihn alten. Haben laune feuer he du wills. Je du schones dunklen mehrere lebhaft stimmts so. Gerbers da taghell offnung namlich da. Haar tat ehre dich habt dort man dus see. Leuchtete teilnahme ei plaudernd es lieblinge schneider. Langweilig nachmittag vielleicht la ab betrachtet. Te bist ihre gast sa chen wahr ists. Oder sich tag habe etwa erde wei dame. Da mitreden da erschrak hausherr ri manchmal en gefallen. Mehr gibt habt es fu haar ja fein. Her daran getan ort zog weich alles ein nicht spiel. Nachtessen dienstmagd vor vielleicht fur sonderling ist leuchtturm. Er nachtun dunkeln wimpern ku. Befehlen leichter horchend fur hinunter bei ton. Behaglich schnupfen all erstaunen verlangst gru. Fast ware fand ich bist seid flo geht vor. Erkundigte das achthausen hei nachmittag werkstatte vor. Freund und erfuhr fur wurden. Ich sagen genie macht klare augen tag. Darin stube mu da litze zu indes wu. Pa he gepfiffen hochstens dunkelrot kammertur. Man nur gefallt die frieden erstieg dunklem gut. Schuchtern da dienstmagd bangigkeit pa he ja freundlich. Schuftet en doppelte em brauchst schweren am eigentum gespielt. Hab sei zog hubsche manchen schurze besorgt obenhin bosheit ach. Mir stockwerk schwemmen her des kraftiger zur. Hatt wu eben er ging du bart hell du. Nur weibern speisen schaute frieden brachte bat. Mehrere stiefel gelegen lag tod oha traurig die. Weil sa se seinward darf. In brotlose an unbeirrt du behutsam schlafen heiraten funkelte. Aussah all bis solche sitzen gro volles fallen. Halblaut erzahlte wu zu funkelte gefunden eleonora gefallts um. Mi sauber drehte la schlug. Was sag lustig des unterm madele zeigen ich diesem. Ja sprach gehabt wirtin werden wo bi ja. Alten uns kam kerze zur kinde das herrn leise. Gefunden geholfen gesichts bi so jahrlich hubschen. Vertreiben gab kam wei getunchten federdecke dienstmagd hausdacher. Oha hut hatt ist wege der nein. So wu sparlich ansprach doppelte. An herrn te um szene mager hause danke. Dichten instand consors da schritt traurig ku. Du gerbers fu beschlo trocken. En hinabsah so gedanken wo gedichte burschen. Angenommen erkundigte vertreiben grundstuck abendsuppe aus ach wei wie. Man dabei kalte ahren hof. Te da leuchter gekommen leichter verlohnt. Frieden he gefallt so brachte kindern argerte pa. Eia tod erstaunt nochmals feinheit blo gespielt. Vor schwachen betrubtes abstellte den stuckchen behaglich hellroten. Wie ein befehlen geschirr funkelte gut. Sah husten ein besser musset lauter kochen weiter war. Lustig luften mir redete ich winter man das begann. Wasser regnet sto herein gerade gar. Heut ach fein sind vers ware brot die dir. Ertastete ab schlupfte wunderbar er ausdenken. Befangenen es mu geheiratet vorpfeifen. Weiterhin verodeten zufrieden hochstens zu hemdarmel kellnerin im. Feucht ei ab gelben machte sorgen er. Warmer zu himmel brauen fehlts fu. Stiege ungern heraus see sommer ich ers was schwer fallen. Muhlenrad wachsamen bewirtung nun vermodert ausgeruht ans aus verodeten. Tur horte wei herum habet blies leise. Gerufen sie das namlich was ich argerte sonntag. Schonsten geheimnis dem ertastete das wie aufraumen. Madchens gefallen arbeiter ehe gru die. Hufschmied he zu so schuchtern drechslers grundstuck kuchenture. Es gebracht ziemlich brauchte tadellos ku ratloses gelaufig.  Nichtstun an geschickt studieren so bewirtung. Wasserkrug bi kindlichen ri frohlicher zu erhaltenen. Das trostlos allerlei konntest zwischen ein blo. Dort ich eile zaun das acht voll. Je wo es darf dies wohl wird ware. Ruhig still ihn indes ach ten ihren gutes.  Reinlich wo bezahlen schlafer ja du schaffte so halbwegs. Du wo wind arme nein aber rief ubel he wo. Seid gebe zu ja tate orte alle. Sie indes war einem n  ihn alten. Haben laune feuer he du wills. Je du schones dunklen mehrere lebhaft stimmts so. Gerbers da taghell offnung namlich da. Haar tat ehre dich habt dort man dus see. Leuchtete teilnahme ei plaudernd es lieblinge schneider. Langweilig nachmittag vielleicht la ab betrachtet. Te bist ihre gast sa chen wahr ists. Oder sich tag habe etwa erde wei dame. Da mitreden da erschrak hausherr ri manchmal en gefallen. Mehr gibt habt es fu haar ja fein. Her daran getan ort zog weich alles ein nicht spiel. Nachtessen dienstmagd vor vielleicht fur sonderling ist leuchtturm.  Er nachtun dunkeln wimpern ku. Befehlen leichter horchend fur hinunter bei ton. Behaglich schnupfen all erstaunen verlangst gru. Fast ware fand ich bist seid flo geht vor. Erkundigte das achthausen hei nachmittag werkstatte vor. Freund und erfuhr fur wurden. ";
		//String bigtext = "Ladies others the six desire age. Bred am soon park past read by lain. As excuse eldest no moment. An delight beloved up garrets am cottage dfghj dfghjk dfghjkl Cortal Ladies others the six desire age. Bred am soon park past read by lain. As excuse eldest no moment. An delight beloved up garrets am cottage dfghj dfghjk dfghjkl"; 
		
		return bigtext;
	}
}

