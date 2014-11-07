package de.comlineag.snc.data;

import java.net.URL;

/**
*
* @author 		Christian Guenther
* @category 	data type
* @version		0.1				- 23.10.2014
* @status		in development
*
* @description	a data type defining one image, e.g. in a website. Elements provided by this class are:
* 				the title
* 				the raw html content
* 				the text (without html markup)
* 				a list of links within the page
* 				a list of images (provided by class ImageData)
*
* @changelog	0.1 (Chris)		class created
* 
*/
public class ImageData implements Comparable<ImageData> {
	private final String src;
	private final String width;
	private final String height;
	private final String alt;
	private final int area;
	private final String imageName;
	private final URL link;
	
	public ImageData(final String src, final String width, final String height, final String alt, final String imageName, URL link) {
		this.src = src;
		if(src == null) {
			throw new NullPointerException("src attribute must not be null");
		}
		
		this.width = nullTrim(width);
		this.height = nullTrim(height);
		this.alt = nullTrim(alt);
		this.imageName = nullTrim(imageName);
		this.link = link;
		
		if(width != null && height != null) {
			int a;
			try {
				a = Integer.parseInt(width) * Integer.parseInt(height);
			} catch(NumberFormatException e) {
				a = -1;
			}
			this.area = a;
		} else {
			this.area = -1;
		}
	}

    public String getSrc() { return src; }
    public String getWidth() { return width; }
    public String getHeight() { return height; }
    public String getAlt() { return alt; }
    public URL getLink() { return link; }
    
    
    /**
     * Returns the image's area (specified by width * height), or -1 if width/height weren't both specified or could not be parsed.
     * 
     * @return
     */
    public int getArea() { return area; }
    
    
    public String toString() { return src+"\twidth="+width+"\theight="+height+"\talt="+alt+"\tarea="+area+"\name="+imageName; }
    
    private static String nullTrim(String s) {
    	if(s == null) {
    		return null;
    	}
    	s = s.trim();
    	if(s.length() == 0) {
    		return null;
    	}
    	return s;
    }
    
    
    @Override
    public int compareTo(ImageData o) {
            if(o == this) {
                    return 0;
            }
            if(area > o.area) {
                    return -1;
            } else if(area == o.area) {
                    return src.compareTo(o.src);
            } else {
                    return 1;
            }
    }
}