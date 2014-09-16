package de.comlineag.snc.data;

public class HtmlContent {

    private final String title;
    private final String text;
    private final String image;

    public HtmlContent(String title, String text, String image) {
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "HtmlContent [title=" + title + ", text=" + text + ", image=" + image + "]";
    }
    
}