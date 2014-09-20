package de.comlineag.snc.helper;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import de.comlineag.snc.data.HtmlContent;
import de.comlineag.snc.data.Image;
import de.comlineag.snc.helper.ImageExtractor;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.HTMLFetcher;
 
public class BoilerpipeContentExtractionService {
	// we use simple org.apache.log4j.Logger for lgging
	private final Logger logger = Logger.getLogger(getClass().getName());
	// in case you want a log-manager use this line and change the import above
	//private final Logger logger = LogManager.getLogger(getClass().getName());
	
	//public BoilerpipeContentExtractionService(){}
	
    public HtmlContent getPageContent(String url) {
    	logger.debug("fetching page from url " + url.toString());
        try {
            final HTMLDocument htmlDoc = HTMLFetcher.fetch(new URL(url));
            final TextDocument doc = new BoilerpipeSAXInput(htmlDoc.toInputSource()).getTextDocument();
            String title = doc.getTitle();
 
            String content = ArticleExtractor.INSTANCE.getText(doc);
 
            final BoilerpipeExtractor extractor = CommonExtractors.KEEP_EVERYTHING_EXTRACTOR;
            final ImageExtractor ie = ImageExtractor.INSTANCE;
            
            List<Image> images = ie.process(new URL(url), extractor);
 
            Collections.sort(images);
            String image = null;
            if (!images.isEmpty()) {
                image = images.get(0).getSrc();
            }
 
            return new HtmlContent(title, content, image);
        } catch (Exception e) {
        	logger.error("EXCEPTION :: could not fetch page from url " + url.toString(), e);
            return null;
        }
 
    }
}