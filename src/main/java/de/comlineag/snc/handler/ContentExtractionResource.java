package de.comlineag.snc.handler;
 
import de.comlineag.snc.helper.BoilerpipeContentExtractionService;
import de.comlineag.snc.data.HtmlContent;

public class ContentExtractionResource {
	private BoilerpipeContentExtractionService boilerpipeContentExtractionService;
	public HtmlContent ExtractContent(String url) {
		return boilerpipeContentExtractionService.getPageContent(url);
	}
}
