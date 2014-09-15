/*
 * Copyright (C) 2005 Poroshin Vladimir. All Rights Reserved. This program is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.porva.html.keycontent;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XHTMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.porva.html.FragmentExtractor;
import com.porva.util.log.ConsoleFormatter;

/**
 * Main class to extract key content from a HTML source. 
 * @author Poroshin
 */
public class Kce
{
  private Document mTree = null; // the DOM tree for HTML

  private String charset = "ISO-8859-1"; // default charset

  private boolean mCheckChildren = true; // Boolean that determines if the the children of the node
                                          // should be filtered

  private final KceSettings settings;

  private int lengthForTableRemover = 0;

  private List<NodeFoundListener> nodeFoundListeners = new Vector<NodeFoundListener>();

  private static final Log logger = LogFactory.getLog(Kce.class);

  /**
   * Allocate a new {@link Kce} with specified settings.
   * 
   * @param settings an object of settings.
   */
  public Kce(KceSettings settings)
  {
    this.settings = settings;
    if (this.settings == null)
      throw new NullPointerException();
    
    AdsServerList.init(this.settings.adsServerListFile);
  }

  /**
   * Extract key content from W3C document.
   * 
   * @param document W3C document to filter.
   * @param uri original localtion of the content of this document.
   * @return cleaned document.
   */
  public Document extractKeyContent(final Document document, URI uri)
  {
    mTree = document;
    extractKeyContent(mTree);
    return mTree;
  }

  /**
   * Extract key content from the data from input stream. 
   * 
   * @param in input stream of the of data to filter.
   * @param charset character encoding of the data received form the stream. 
   * @param uri original location of the data.
   * @return cleaned document.
   */
  public Document extractKeyContent(final InputStream in, String charset, URI uri)
  {
    if (charset != null)
      this.charset = charset;

    try {
      InputSource inputSource = null;

      if (uri != null) {
        String fragment = uri.getFragment();
        FragmentExtractor fragmentExtractor = new FragmentExtractor();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuffer sb = new StringBuffer();
        String str;
        while ((str = br.readLine()) != null) {
          sb.append(str);
        }
        String contentStr = sb.toString();
        String fragmentStr = fragmentExtractor.extractFragment(contentStr, fragment);
        if (fragmentStr != null) {
          String html = fragmentExtractor.fragmentToHtml(contentStr, fragmentStr);
          inputSource = new InputSource(new StringReader(html));
        } else {
          inputSource = new InputSource(new StringReader(contentStr));
        }
      }

      if (inputSource == null) {
        InputStreamReader reader = new InputStreamReader(in, this.charset);
        inputSource = new InputSource(reader);
      }

      // HTMLParser parser = new HTMLParser();
      org.cyberneko.html.parsers.DOMParser parser = new org.cyberneko.html.parsers.DOMParser();
      parser.parse(inputSource);
      mTree = parser.getDocument();
      extractKeyContent(mTree);
    } catch (Exception e) {
      logger.warn(e.getMessage());
      e.printStackTrace();
    }
    return mTree;
  }

  /**
   * Register a new W3C node listener. 
   * 
   * @param listener a listener to register.
   * @throws NullArgumentException if <code>listener</code> is <code>null</code>. 
   */
  public void registerNodeFoundListener(NodeFoundListener listener)
  {
    if (listener == null)
      throw new NullArgumentException("listener");
    nodeFoundListeners.add(listener);
  }

  private void extractKeyContent(final Node iNode)
  {
    NodeList children = iNode.getChildNodes();
    if (children != null) {
      int len = children.getLength();
      for (int i = 0; i < len; i++) {
        if (children.getLength() != len) { // filterNode() has removed some top element (probable
                                            // comments)
          len = children.getLength();
          filterNode(children.item(--i));
        } else
          filterNode(children.item(i));
      }
    }

    // Appends the links to the bottom of the page
    // if (settings.addLinksToBottom) // todo
    // addEnqueuedLinks();
  }

  private void filterNode(final Node iNode)
  {
    if (iNode == null)
      logger.warn("filterNode: node is null");

    mCheckChildren = true;

    // Put the node through the sequence of filters
    passThroughFilters(iNode);

    if (!mCheckChildren)
      return;

    if (iNode.hasChildNodes()) {
      Node next = iNode.getFirstChild();
      while (next != null) {
        Node current = next;
        next = current.getNextSibling();
        filterNode(current); // filter children recursivly
      }
    }
  }

  /**
   * Passes a node through a set of filters
   * 
   * @param iNode the node to filter
   */
  private void passThroughFilters(final Node iNode)
  {
    if (iNode == null) {
      logger.warn("Node is null!");
      return;
    }
    Node parent = iNode.getParentNode();
    if (parent == null) {
      logger.warn("Parent node is null!");
      return;
    }

    int type = iNode.getNodeType();
    String name = iNode.getNodeName();

    // remove comments
    if (settings.isRemoveComments && (type == Node.COMMENT_NODE)) {
      parent.removeChild(iNode);
      mCheckChildren = false;
      return;
    }

    // Element node
    if (type == Node.ELEMENT_NODE) {

      // keep tags
      if (settings.isKeepTags) {
        for (int i = 0; i < settings.keepTags.length; i++) {
          if (settings.keepTags[i].equalsIgnoreCase(name))
            return;
        }
      }

      // remove tags
      if (settings.isRemoveTags) {
        for (int i = 0; i < settings.removeTags.length; i++) {
          if (settings.removeTags[i].equalsIgnoreCase(name)) {
            parent.removeChild(iNode);
            mCheckChildren = false;
            return;
          }
        }
      }

      // process <META ...> tags
      if (settings.isProcessMeta && name.equals("META")) {
        if (settings.isRemoveAllMeta) {
          iNode.getParentNode().removeChild(iNode);
          mCheckChildren = false;
        }
        return;
      }

      // remove ad links
      if (settings.isRemoveAdLinks && isAdLink(iNode)) {
        parent.removeChild(iNode);
        mCheckChildren = false;
        return;
      }

      // remove text empty elements
      if (settings.isRemoveTextEmptyElements && isNoTextInside(iNode)) { // todo: isNoTextInside
                                                                          // can remove iNode
        lengthForTableRemover = 0;
        try {
          parent.removeChild(iNode);
        } catch (Exception e) {
        } // todo
        mCheckChildren = false;
        return;
      }

      // remove all attributes from some nodes
      if (settings.isRemoveAttr) {
        for (int i = 0; i < settings.removeAttrNodes.length; i++) {
          if (settings.removeAttrNodes[i].equalsIgnoreCase(name)) {
            NamedNodeMap attributes = iNode.getAttributes();
            while (attributes.getLength() != 0)
              removeAttribute(iNode, attributes.item(0).getNodeName());
          }
        }
      }

    }

    Iterator it = nodeFoundListeners.iterator();
    while (it.hasNext()) {
      NodeFoundListener listener = (NodeFoundListener) it.next();
      if (listener.getNodeName().equalsIgnoreCase(name))
        listener.foundNode(iNode);
    }
  }

  private boolean isNoTextInside(final Node iNode)
  {
    boolean noText = true;

    int type = iNode.getNodeType();
    String name = iNode.getNodeName();

    if (type == Node.ELEMENT_NODE) {
      if (settings.isRemoveLinkCells && (name.equals("TR") || name.equals("DIV"))) {
        return testRemoveCell(iNode);
      }
    } else if (type == Node.TEXT_NODE) {
      lengthForTableRemover += getTextLength(iNode.getNodeValue());
      if (lengthForTableRemover >= settings.substanceMinTextLength)
        return false;
    }

    // Process the children
    if (iNode.hasChildNodes()) {
      Node next = iNode.getFirstChild();

      while (next != null) {
        Node current = next;
        next = current.getNextSibling();
        if (!isNoTextInside(current))
          return false;
      } // while
    } // if

    return noText;
  }

  private int getTextLength(String str)
  {
    int len = 0;
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      // see http://www.cs.utk.edu/~pham/ascii_table.jpg
      if ((ch >= 48 && ch <= 57) || (ch >= 65 && ch <= 90) || (ch >= 97 && ch <= 122) || ch > 127) // ||
                                                                                                    // ch >
                                                                                                    // 127
                                                                                                    // //
                                                                                                    // todo:
                                                                                                    // !!!
        len++;
    }
    return len;
  }

  /**
   * Removes a table cell if the link ratio is appropriate
   * 
   * @param iNode the table cell node
   */
  private boolean testRemoveCell(final Node iNode)
  {
    // Ignore if the cell has no children
    if (!iNode.hasChildNodes()) {
      iNode.getParentNode().removeChild(iNode);
      return true;
    }

    // Count up links and words
    double links = getNumLinks(iNode);
    double words = getNumWords(iNode);

    // Compute the ratio and check for divide by 0
    double ratio = 0;
    if (words == 0)
      ratio = settings.linkTextRatio + 1;
    else
      ratio = links / words;

    if (words >= settings.minNumOfWords)
      return false;

    if (ratio > settings.linkTextRatio) {

      iNode.getParentNode().removeChild(iNode);
      return true;
      // Node next = iNode.getFirstChild();
      // while (next != null) {
      // Node current = next;
      // next = current.getNextSibling();
      //
      // Node next2 = iNode.getFirstChild();
      // while (next2 != null) {
      // Node current2 = next;
      // next2 = current2.getNextSibling();
      // //removeAll(current2);
      // }
      //
      // //Don't check the children because they are all removed
      // mCheckChildren = false;
      // }
    }
    return false;
  }

  /**
   * Counts the number of links from one node downward
   * 
   * @param iNode the node to start counting from
   * @return the number of links
   */
  private double getNumLinks(final Node iNode)
  {
    double links = 0;

    if (iNode.hasChildNodes()) {
      Node next = iNode.getFirstChild();

      while (next != null) {
        Node current = next;
        next = current.getNextSibling();
        links += getNumLinks(current);
      }
    }

    if (isLink(iNode))
      links++;

    return links;
  }

  /**
   * Counts the number of words from one node downward
   * 
   * @param iNode the node to start counting from
   * @return the number of words
   */
  private double getNumWords(final Node iNode)
  {
    double words = 0;

    if (iNode.hasChildNodes()) {
      Node next = iNode.getFirstChild();

      while (next != null) {
        Node current = next;
        next = current.getNextSibling();

        // If it is a link, don't go any deeper into it
        if (!isLink(current))
          words += getNumWords(current);
      }
    }

    // Check to see if the node is a Text node or an element node
    int type = iNode.getNodeType();

    // Text node
    if (type == Node.TEXT_NODE) {
      String content = iNode.getNodeValue();
      words += ((double) getTextLength(content)) / settings.letternsPerWord;
    } // if

    return words;
  } // getNumWords

  /**
   * Checks to see if a node is a link
   * 
   * @param iNode the node to check
   * @return true if the node is a link, false if it is not
   */
  private boolean isLink(final Node iNode)
  {
    // Check to see if the node is a Text node or an element node
    int type = iNode.getNodeType();

    // Element node
    if (type == Node.ELEMENT_NODE) {
      String name = iNode.getNodeName();

      // Check to see if it is a link
      if (name.equals("A"))
        return ((Element) iNode).hasAttribute("href");

    } // if

    return false;
  }

  /**
   * Determines if a node has a link to an ad
   * 
   * @param iNode the node to check for ads
   * @return true if the node is a link to an ad, or false if it isn't
   */
  private boolean isAdLink(final Node iNode)
  {
    String attr = null;

    if (hasAttribute(iNode, "href"))
      attr = "href";
    else if (hasAttribute(iNode, "src"))
      attr = "src";
    else
      // Doesn't had the required attributes
      return false;

    // Get the address of the potential ad
    Node attrNode = iNode.getAttributes().getNamedItem(attr);
    String address = attrNode.getNodeValue();

    try {
      URL addressURL = new URL(address);
      String host = addressURL.getHost();

      return AdsServerList.isAdServer(host);

    } catch (Exception e) {
      // Don't do anything because if the URL is malformed, it
      // probably doesn't point towards an advertisement domain
    } // catch

    return false;

  }

  /**
   * Checks to see if an attribute exists in an Element node
   * 
   * @param iNode the node
   * @param iAttr the name of the attribute to check for
   * @return true if the attribute exists, false if it doesn't
   */
  private boolean hasAttribute(final Node iNode, final String iAttr)
  {
    Node attr = iNode.getAttributes().getNamedItem(iAttr);
    if (attr == null)
      return false;
    else
      return true;
  } // hasAttribute

  /**
   * Removes an attribute if the attrbiute exists from an Element node
   * 
   * @param iNode the node
   * @param iAttr the name of the attribute
   */
  private void removeAttribute(final Node iNode, final String iAttr)
  {
    iNode.getAttributes().removeNamedItem(iAttr);
  }

  /**
   * Pretty prints the cleaned HTML to an writer <code>iOut</code>.
   * 
   * @param node the Document to start printing from
   * @param out the writer to print to.
   */
  public static void prettyPrint(final Document node, final String charset, final Writer out)
  {
    OutputFormat format = new OutputFormat(node, charset, true);
    XHTMLSerializer printer = new XHTMLSerializer(out, format);

    try {
      printer.serialize(node);
      out.flush();
    } catch (Exception e) {
      logger.warn("Failed to print document", e);
    }
  }

  /**
   * Pretty print the cleaned document as text.
   * 
   * @param node starting node of W3C document.
   * @param out the writer to print to.
   * @throws IOException if some IO exception occurs.
   */
  public static void textPrint(final Node node, final Writer out) throws IOException
  {
    // Print child nodes first
    if (node.hasChildNodes()) {
      Node next = node.getFirstChild();

      while (next != null) {
        Node current = next;
        next = current.getNextSibling();

        String name = current.getNodeName();
        boolean valid = true;

        if (name.equalsIgnoreCase("STYLE"))
          valid = false;
        else if (name.equalsIgnoreCase("SCRIPT"))
          valid = false;
        if (valid)
          textPrint(current, out);
      }
    }

    int type = node.getNodeType();

    if (type == Node.ELEMENT_NODE) {
      if (node.getNodeName().equalsIgnoreCase("BR")) {
        out.write("\n");
      }
    }
    else if (type == Node.TEXT_NODE) {
      if (!(node.getNodeValue().trim().equals(""))) {
        out.write(node.getNodeValue());
      }
    }
  }

  // ///////////////////
  // remove the same content funcitons
  // ///////////////////

  private static void removeTheSameLinks(List linkNodes, String uriStr)
  {
    List downloadCandidates = filterLinks(linkNodes, uriStr);
    Iterator it = downloadCandidates.iterator();
    while (it.hasNext()) { // todo: find best linkNodes to download
      URL url = (URL) it.next();
      try {
        String content = getContent(url);

        KceSettings settings = new KceSettings();
        Kce extractor = new Kce(settings);
        LinkFoundListener linkFoundListener = new LinkFoundListener();
        extractor.registerNodeFoundListener(linkFoundListener);
        extractor.extractKeyContent(new ByteArrayInputStream(content.getBytes()), "iso-8859-1",
                                    null); // todo
        List foundLinks = linkFoundListener.getFoundNodes();

        Iterator it2 = foundLinks.iterator();
        while (it2.hasNext()) {
          Node node = (Node) it2.next();
          String link = ((Element) node).getAttribute("href");

          Iterator it3 = linkNodes.iterator();
          while (it3.hasNext()) { // todo: optimize -- put linkNodes into map
            Node node3 = (Node) it3.next();
            String link3 = ((Element) node3).getAttribute("href");
            if (node3.getParentNode() != null && link.equalsIgnoreCase(link3)) {
              node3.getParentNode().removeChild(node3);
              logger.info("Removed the same link: " + link3);
            }
          }
        }

        break;
      } catch (IOException e) {

      }
    }
  }

  private static String getContent(URL url) throws IOException
  {
    StringBuffer buf = new StringBuffer();
    // Read all the text returned by the server
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    String str;
    while ((str = in.readLine()) != null) {
      buf.append(str);
    }
    in.close();
    return buf.toString();
  }

  private static List filterLinks(List linkNodes, String uriStr)
  {
    List<URL> filteredLinks = new Vector<URL>();
    if (linkNodes == null)
      return filteredLinks;
    URL pageUrl;
    try {
      pageUrl = new URL(uriStr);
    } catch (MalformedURLException e) {
      logger.warn("Failed to create URL from " + uriStr + ": " + e.toString());
      return filteredLinks;
    }

    Iterator it = linkNodes.iterator();
    while (it.hasNext()) {
      Node node = (Node) it.next();
      String link = ((Element) node).getAttribute("href");
      try {
        URL url = new URL(pageUrl, link); // resolve link to absolute if it is relative

        if (url.getHost().equals(pageUrl.getHost()) && url.getPath().equals(pageUrl.getPath())) // skip
                                                                                                // the
                                                                                                // same
                                                                                                // url
          continue;

        if (url.getHost().equals(pageUrl.getHost())
            && getPathExtension(url).equalsIgnoreCase(getPathExtension(pageUrl))) {
          filteredLinks.add(url);
        }
      } catch (MalformedURLException e) {
        // e.printStackTrace(); //To change body of catch statement use File | Settings | File
        // Templates.
      }
    }
    return filteredLinks;
  }

  private static String getPathExtension(URL url)
  {
    Pattern filePattern = Pattern.compile(".*/[^/]+\\.([^/]+)$");
    String path = url.getPath();
    String fileType = null;
    if (path == null || path.equals("")) // todo: is it so that null path is equivalent to empty
                                          // ???
      path = "/";

    Matcher matcher = filePattern.matcher(path);
    if (matcher.matches()) {
      fileType = matcher.group(1);
      fileType = fileType.toLowerCase();
    } else
      fileType = "html"; // for links like "http://server.com/" or "http://server.com" or
                          // http://server.com/path

    return fileType;
  }
  
  ///////////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws IOException, URISyntaxException
  {
    Logger.getLogger("").setLevel(Level.INFO);
    Logger.getLogger("").getHandlers()[0].setFormatter(new ConsoleFormatter());

    if (args.length < 2) {
      printHelp();
      System.exit(1);
    }
    if (args[0].equals("-server")) {
      serverMode(args);
    } else if (args[0].equals("-clean")) {
      cleanMode(args);
    }
  }
  
  private static void serverMode(String[] args)
  {
    int port;
    if (args[1].equals("-p"))
      port = Integer.parseInt(args[2]);
    else 
      throw new IllegalArgumentException("Port number is invalid.");
    KceServer kceServer = new KceServer(port);
    kceServer.start();
  }
  
  private static void cleanMode(String[] args) throws IOException
  {
    String file = args[1];
    String charset = "ISO-8859-1";
    if (args.length > 2)
      charset = args[2];

    KceSettings settings = new KceSettings();
    settings.loadSettings(System.getProperty("user.dir") + File.separator + "conf" + File.separator
        + "keycontent.properties");
    Kce extractor = new Kce(settings);

//    // listeners
//    LinkFoundListener linkFoundListener = new LinkFoundListener();
//    extractor.registerNodeFoundListener(linkFoundListener);
    Document parsedDoc;
    if (file.startsWith("http://")) {
      URL url = new URL(file);
      InputStream in = url.openStream();
      parsedDoc = extractor.extractKeyContent(in, charset, null);
      in.close();
    }
    else 
      parsedDoc = extractor.extractKeyContent(new FileInputStream(new File(file)), charset, null);
    Kce.prettyPrint(parsedDoc, "utf-8", new PrintWriter(System.out));
  }
  
  private static void printHelp()
  {
    System.err.println("HTML Key Conten Extractor ver. 1.0.0.");
    System.err.println("Usage: java -jar kce.jar -clean html_file [charset]");
    System.err.println("       java -jar kce.jar -clean url_to_clean [charset]");
    System.err.println("       java -jar kce.jar -server -p port_number");
    System.err.println("All cleaned results are in UTF-8 encoding.");
    System.err.println("Examples: java -jar kce.jar -clean file_to_clean.html ISO-8859-1");
    System.err.println("          java -jar kce.jar -clean http://news.com.com");
    System.err.println("          java -jar kce.jar -server -p 12345");
  }
}
