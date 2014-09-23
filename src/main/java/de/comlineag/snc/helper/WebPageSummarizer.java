package de.comlineag.snc.helper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

public class WebPageSummarizer {

	/**
	* Return a Map of extracted attributes from the web page identified by url.
	* @param url the url of the web page to summarize.
	* @return a Map of extracted attributes and their values.
	*/
	public Map<String,Object> summarize(String url) throws Exception {
		Map<String,Object> summary = new HashMap<String,Object>();
		Source source = new Source(new URL(url));
		source.fullSequentialParse();
		summary.put("title", getTitle(source));
		summary.put("description", getMetaValue(source, "description"));
		summary.put("keywords", getMetaValue(source, "keywords"));
		summary.put("images", getElementText(source, HTMLElementName.IMG, "src", "alt"));
		summary.put("links", getElementText(source, HTMLElementName.A, "href"));
		return summary;
	}
	
	public String getTitle(Source source) {
	 Element titleElement=source.getNextElement(0, HTMLElementName.TITLE);
	 if (titleElement == null) {
	   return null;
	 }
	 // TITLE element never contains other tags so just decode it collapsing whitespace:
	 return CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
	}
	
	private String getMetaValue(Source source, String key) {
	 for (int pos = 0; pos < source.length(); ) {
	   StartTag startTag = source.getNextStartTag(pos, "name", key, false);
	   if (startTag == null) {
	     return null;
	   }
	   if (startTag.getName() == HTMLElementName.META) {
	     String metaValue = startTag.getAttributeValue("content");
	     if (metaValue != null) {
	    	 // Hm, I think I'm missing a dependency
	       //metaValue = LcppStringUtils.removeLineBreaks(metaValue);
	     }
	     return metaValue;
	   }
	   pos = startTag.getEnd();
	 }
	 return null;
	}
	
	private List<NameValuePair> getElementText(Source source, String tagName, 
	   String urlAttribute) {
	 return getElementText(source, tagName, urlAttribute, null);
	}
	
	@SuppressWarnings("unchecked")
	private List<NameValuePair> getElementText(Source source, String tagName, 
	   String urlAttribute, String srcAttribute) {
	 List<NameValuePair> pairs = new ArrayList<NameValuePair>();
	 List<Element> elements = source.getAllElements(tagName);
	 for (Element element : elements) {
	   String url = element.getAttributeValue(urlAttribute);
	   if (url == null) {
	     continue;
	   }
	   // A element can contain other tags so need to extract the text from it:
	   String label = element.getContent().getTextExtractor().toString();
	   if (label == null) {
	     // if text content is not available, get info from the srcAttribute
	     label = element.getAttributeValue(srcAttribute);
	   }
	   // if still null, replace label with the url
	   if (label == null) {
	     label = url;
	   }
	   pairs.add(new NameValuePair(label, url));
	 }
	 return pairs;
	}
}