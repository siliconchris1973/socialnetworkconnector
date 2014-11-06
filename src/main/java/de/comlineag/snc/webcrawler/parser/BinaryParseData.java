/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.comlineag.snc.webcrawler.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import de.comlineag.snc.webcrawler.url.WebURL;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryParseData implements ParseData {
	
	private static final Logger logger = LoggerFactory.getLogger(BinaryParseData.class);
	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String DEFAULT_OUTPUT_FORMAT = "html";
	
	// FIXME the original code read this, but gave an error
	//private static final WebCrawlerParser AUTO_DETECT_PARSER = new AutoDetectParser();
	private static final AutoDetectParser AUTO_DETECT_PARSER = new AutoDetectParser();
	private static final SAXTransformerFactory SAX_TRANSFORMER_FACTORY = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
	
	private final ParseContext context = new ParseContext();
	private Set<WebURL> outgoingUrls = new HashSet<>();
	private String html = null;
	
	public BinaryParseData() {
		// FIXME this was marked as a comment, because of type mismatch!!!
		//context.set(WebCrawlerParser.class, AUTO_DETECT_PARSER);
	}
	
	public void setBinaryContent(byte[] data) {
		InputStream inputStream = new ByteArrayInputStream(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		try {
			TransformerHandler handler = getTransformerHandler(outputStream, DEFAULT_OUTPUT_FORMAT, DEFAULT_ENCODING);
			AUTO_DETECT_PARSER.parse(inputStream, handler, new Metadata(), context);
			
			// Hacking the following line to remove Tika's inserted DocType
			String htmlContent = new String(outputStream.toByteArray(), DEFAULT_ENCODING).replace("http://www.w3.org/1999/xhtml", "");
			setHtml(htmlContent);
		} catch (TransformerConfigurationException e) {
			logger.error("Error configuring handler", e);
		} catch (UnsupportedEncodingException e) {
			logger.error("Encoding for content not supported", e);
		} catch (Exception e) {
			logger.error("Error parsing file", e);
		}
	}
	
  /**
   * Returns a transformer handler that serializes incoming SAX events to
   * XHTML or HTML (depending the given method) using the given output encoding.
   *
   * @param encoding output encoding, or <code>null</code> for the platform default
   */
  private static TransformerHandler getTransformerHandler(OutputStream out, String method, String encoding)
        throws TransformerConfigurationException {

    TransformerHandler transformerHandler = SAX_TRANSFORMER_FACTORY.newTransformerHandler();
    Transformer transformer = transformerHandler.getTransformer();
    transformer.setOutputProperty(OutputKeys.METHOD, method);
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

    if (encoding != null) {
      transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
    }

    transformerHandler.setResult(new StreamResult(new PrintStream(out)));
    return transformerHandler;
  }

  /** @return Parsed binary content or null */
  public String getHtml() {
    return html;
  }

    public void setHtml(String html) {
      this.html = html;
    }

  @Override
  public Set<WebURL> getOutgoingUrls() {
    return outgoingUrls;
  }

  @Override
  public void setOutgoingUrls(Set<WebURL> outgoingUrls) {
    this.outgoingUrls = outgoingUrls;
  }

  @Override
  public String toString() {
    if (html == null || html.isEmpty()) {
      return "No data parsed yet";
    } else {
      return getHtml();
    }
  }
}