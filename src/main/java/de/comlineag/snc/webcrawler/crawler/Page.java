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

package de.comlineag.snc.webcrawler.crawler;

import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import de.comlineag.snc.webcrawler.parser.ParseData;
import de.comlineag.snc.webcrawler.url.WebURL;

/**
 * This class contains the data for a fetched and parsed page.
 *
 * @author Yasser Ganjisaffar <lastname at gmail dot com>
 */
public class Page {

  /**
   * The URL of this page.
   */
  protected WebURL url;

  /**
  * Redirection flag
  */
  protected boolean redirect;

  /**
   * The URL to which this page will be redirected to
   */
  protected String redirectedToUrl;

  /**
  * Status of the page
  */
  protected int statusCode;

  /**
   * The content of this page in binary format.
   */
  protected byte[] contentData;

  /**
   * The ContentType of this page.
   * For example: "text/html; charset=UTF-8"
   */
  protected String contentType;

  /**
   * The encoding of the content.
   * For example: "gzip"
   */
  protected String contentEncoding;

  /**
   * The charset of the content.
   * For example: "UTF-8"
   */
  protected String contentCharset;

  /**
  * Language of the Content.
  */
  private String language;

  /**
   * Headers which were present in the response of the
   * fetch request
   */
  protected Header[] fetchResponseHeaders;

  /**
   * The parsed data populated by parsers
   */
  protected ParseData parseData;


  public Page(WebURL url) {
    this.url = url;
  }

  /**
   * Loads the content of this page from a fetched
   * HttpEntity.
   */
  public void load(HttpEntity entity) throws Exception {

    contentType = null;
    Header type = entity.getContentType();
    if (type != null) {
      contentType = type.getValue();
    }

    contentEncoding = null;
    Header encoding = entity.getContentEncoding();
    if (encoding != null) {
      contentEncoding = encoding.getValue();
    }

    Charset charset = ContentType.getOrDefault(entity).getCharset();
    if (charset != null) {
      contentCharset = charset.displayName();
    }


    contentData = EntityUtils.toByteArray(entity);
  }

  public WebURL getWebURL() {
    return url;
  }

  public void setWebURL(WebURL url) {
    this.url = url;
  }

  public boolean isRedirect() {
    return redirect;
  }

  public void setRedirect(boolean redirect) {
    this.redirect = redirect;
  }

  public String getRedirectedToUrl() {
    return redirectedToUrl;
  }

  public void setRedirectedToUrl(String redirectedToUrl) {
    this.redirectedToUrl = redirectedToUrl;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * Returns headers which were present in the response of the
   * fetch request
   */
  public Header[] getFetchResponseHeaders() {
    return fetchResponseHeaders;
  }

  public void setFetchResponseHeaders(Header[] headers) {
    fetchResponseHeaders = headers;
  }

  /**
   * Returns the parsed data generated for this page by parsers
   */
  public ParseData getParseData() {
    return parseData;
  }

  public void setParseData(ParseData parseData) {
    this.parseData = parseData;
  }

  /**
   * Returns the content of this page in binary format.
   */
  public byte[] getContentData() {
    return contentData;
  }

  public void setContentData(byte[] contentData) {
    this.contentData = contentData;
  }

  /**
   * Returns the ContentType of this page.
   * For example: "text/html; charset=UTF-8"
   */
  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Returns the encoding of the content.
   * For example: "gzip"
   */
  public String getContentEncoding() {
    return contentEncoding;
  }

  public void setContentEncoding(String contentEncoding) {
    this.contentEncoding = contentEncoding;
  }

  /**
   * Returns the charset of the content.
   * For example: "UTF-8"
   */
  public String getContentCharset() {
    return contentCharset;
  }

  public void setContentCharset(String contentCharset) {
    this.contentCharset = contentCharset;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }
}