package de.comlineag.snc.data;

import java.net.URL;
import java.util.ArrayList;

/**
*
* @author 		Christian Guenther
* @category 	data type
* @version		0.1				- 23.10.2014
* @status		in development
*
* @description	a data type defining the content of a website. Elements provided by this class are:
* 				the source - source url of the page
* 				the title
* 				the raw html content
* 				the text (without html markup)
* 				a list of links within the page
* 				a list of images (provided by class Image)
*
* @changelog	0.1 (Chris)		class created
* 
*/
public class HtmlContent implements Comparable<HtmlContent> {
	private final String src;
	private final String title;
	private final String text;
	private final String rawText;
	private final ArrayList<Image> images = new ArrayList<Image>();
	private final ArrayList<URL> links = new ArrayList<URL>();
	
	public HtmlContent(String src, String title, String text, String rawText, ArrayList<Image> imagesIn, ArrayList<URL> linksIn) {
		this.src = src;
		this.title = title;
		this.text = text;
		this.rawText = rawText;
		
		for (int i=0;i<imagesIn.size();i++)
			this.images.add(imagesIn.get(i));
		for (int i=0;i<linksIn.size();i++)
			this.links.add(linksIn.get(i));
    }
	
	public HtmlContent(String src, String title, String text, String rawText) {
		this.src = src;
		this.title = title;
		this.text = text;
		this.rawText = rawText;
	}
	
	
	// public getter
	public String getSrc() {return src; }
	public String getText() {return text; }
	public String getTitle() {return title;}
	public String getRawText() {return rawText;}
	public ArrayList<Image> getImages() {return images;}
	public ArrayList<URL> getLinks() {return links;}
	
	
	/**
	 * compares one html content with another based on the src and title elements
	 */
	@Override
	public int compareTo(HtmlContent o) {
		if(o == this) {
			return 0;
		}
		if(src.equals(o.src)) {
			return 0;
		} else if(title.equals(o.title)) {
			return src.compareTo(o.src);
		} else {
			return 1;
		}
    }
	
	@Override
	public String toString() {
		return "HtmlContent [title=" + title + ", text=" + text + ", image=" + "]";
	}
}