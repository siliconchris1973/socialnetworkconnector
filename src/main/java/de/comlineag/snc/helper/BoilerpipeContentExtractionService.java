package de.comlineag.snc.helper;

import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
 
public class BoilerpipeContentExtractionService {
 
    public String returnArticleContent(String rawPage) {
    	try {
        	final HTMLDocument htmlDoc = new HTMLDocument(rawPage);
        	final TextDocument doc = new BoilerpipeSAXInput(htmlDoc.toInputSource()).getTextDocument();
        	String title = doc.getTitle();
        	
        	String content = ArticleExtractor.INSTANCE.getText(doc);
        	
        	return content;
        	//return new HtmlContent(title, content, null);
    	} catch (Exception e) {
    		
    		return null;
    	}
    }
}