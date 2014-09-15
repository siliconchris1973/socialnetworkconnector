package com.porva.html.keycontent;

import junit.framework.TestCase;

public class NewsXMLCleanerTest extends TestCase
{

  /*
   * Test method for 'com.porva.html.keycontent.NewsXMLCleaner.removeIllegalChars(String)'
   */
  public void testRemoveIllegalChars()
  {
    NewsXMLCleaner cleaner = new NewsXMLCleaner();
    String cleaned = cleaner.removeIllegalChars("a absde\n &#18; fg\n &#22;");
    assertEquals("a absde\n ' fg\n ", cleaned);
  }

  public void testPostClean()
  {
    NewsXMLCleaner cleaner = new NewsXMLCleaner();
    String cleaned = cleaner
        .postClean("bla\n-bla\n<BR/>\n       <BR/>\n           " +
            "  <BR/>\n             <BR/>\n            <BR/>\nxxx\nyyy");
    System.out.println(cleaned);
    // assertEquals("a absde\n ' fg\n ", cleaned);
  }

}
