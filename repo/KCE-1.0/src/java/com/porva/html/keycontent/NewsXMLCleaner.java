/*
 *   Copyright (C) 2005 Poroshin Vladimir. All Rights Reserved.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.porva.html.keycontent;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.porva.util.FioUtils;

public class NewsXMLCleaner
{
	private static final KceSettings settings = new KceSettings();

	private Kce extractor;

	private DocumentBuilderFactory factory;

	private static final Log logger = LogFactory.getLog(NewsXMLCleaner.class);

	private int processedNum = 0;

	public NewsXMLCleaner()
	{
		extractor = new Kce(settings);
		factory = DocumentBuilderFactory.newInstance();
	}

	public String removeIllegalChars(String newsXml)
	{
		if (newsXml == null)
			throw new NullArgumentException("newsXml");

		newsXml = newsXml.replace("&#18;", "'");
    //newsXml = newsXml.replaceAll("&#\\d+;", "");
		newsXml = newsXml.replace("&#5;", "");
		newsXml = newsXml.replace("&#11;", "");
		newsXml = newsXml.replace("&#19;", "");
		newsXml = newsXml.replace("&#20;", "");
		newsXml = newsXml.replace("&#14;", "");
		newsXml = newsXml.replace("&#22;", "");
		newsXml = newsXml.replace("&#23;", "");
		newsXml = newsXml.replace("&#2;", "");
		return newsXml;
	}

	// test
	public String postClean(String newsXml)
	{
		if (newsXml == null)
			throw new NullArgumentException("newsXml");
    
    Pattern p = Pattern.compile("<BR/>(\\s*<BR/>)+", Pattern.MULTILINE);
    return p.matcher(newsXml).replaceAll("<BR/><BR/>\n");
	}

	public String clean(String newsXml) throws ParserConfigurationException,
			SAXException, IOException
	{
		if (newsXml == null)
			throw new NullArgumentException("newsXml");

		newsXml = removeIllegalChars(newsXml);

		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource inputSouce = new InputSource(new StringReader(newsXml));
		Document doc = builder.parse(inputSouce);

		Document cleanedDoc = extractor.extractKeyContent(doc, null);
		StringWriter stringWriter = new StringWriter();

		OutputFormat format = new OutputFormat(cleanedDoc, "utf-8", true);
		XMLSerializer printer = new XMLSerializer(stringWriter, format);
		printer.serialize(cleanedDoc);

		String cleaned = stringWriter.toString();
		cleaned = postClean(cleaned);

		return cleaned;
	}

	public String clean(File newsXmlFile) throws ParserConfigurationException,
			SAXException, IOException
	{
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(newsXmlFile);

		Document cleanedDoc = extractor.extractKeyContent(doc, null);
		StringWriter stringWriter = new StringWriter();

		OutputFormat format = new OutputFormat(cleanedDoc, "utf-8", true);
		XMLSerializer printer = new XMLSerializer(stringWriter, format);
		printer.serialize(cleanedDoc);

		return stringWriter.toString();
	}

	public String processFile(final File file)
	{
		String res = null;
		try {
			res = clean(file);
		} catch (Exception e) {
			logger.warn("Failed to clean file: " + file, e);
		}
		return res;
	}

	public void processDir(File dir)
	{
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				processDir(file);
				continue;
			}
			if (file.getAbsolutePath().endsWith(".xml")) {
				try {
					String res = clean(file);
					storeResult(res, file.getAbsolutePath());
					processedNum++;
					if (processedNum % 500 == 0)
						System.out.println(processedNum + " xml files processed");
				} catch (Exception e) {
					logger.warn("Failed to clean file: " + file, e);
				}
			}
		}
	}

	public void storeResult(String res, String file) throws IOException
	{
		FioUtils.writeToFile(file, res, "utf-8");
	}

	// ///////////////////////////////////////////////////////////////////////////

	public static void main(String[] args)
	{
		NewsXMLCleaner cleaner = new NewsXMLCleaner();
		File file = new File(args[0]);
		String result = cleaner.processFile(file);
		System.out.print(result);
	}

	public static String getUsageInfo()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Usage: java -jar kce.jar file_to_clean");

		return sb.toString();
	}

}
