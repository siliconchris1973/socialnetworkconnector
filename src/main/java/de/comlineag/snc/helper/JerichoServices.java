package de.comlineag.snc.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;


public class JerichoServices {
	private static final Set<String> ALLOWED_HTML_TAGS = new HashSet<String>(Arrays.asList(
			HTMLElementName.ABBR,
			HTMLElementName.ACRONYM,
			HTMLElementName.SPAN,
			HTMLElementName.SUB,
			HTMLElementName.SUP)
			);
			 
	public static String removeNotAllowedTags(String htmlFragment) {
		Source source = new Source(htmlFragment);
		OutputDocument outputDocument = new OutputDocument(source);
		List<Element> elements = source.getAllElements();
		for (Element element : elements) {
			if (!ALLOWED_HTML_TAGS.contains(element.getName())) {
				outputDocument.remove(element.getStartTag());
				if (!element.getStartTag().isSyntacticalEmptyElementTag()) {
					outputDocument.remove(element.getEndTag());
				}
			}
		}
		return outputDocument.toString();
	}
	
	public static String getTitle(Source source) {
		Element titleElement=source.getFirstElement(HTMLElementName.TITLE);
		if (titleElement==null) return null;
		// TITLE element never contains other tags so just decode it collapsing whitespace:
		return CharacterReference.decodeCollapseWhiteSpace(titleElement.getContent());
	}
}
