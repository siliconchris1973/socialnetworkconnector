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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class KceSettings
{
	private static Log logger = LogFactory
			.getLog(KceSettings.class);

	boolean isRemoveComments = true;

	boolean isKeepTags = true;

	String[] keepTags = { "BR" };

	boolean isProcessMeta = true;

	boolean isRemoveAllMeta = false;

	// final boolean isAddContentTypeMeta = true;
	// final String contentTypeMetaCharset = "utf-8";

	boolean isRemoveTags = true;

	String[] removeTags = { "SCRIPT", "NOSCRIPT", "INPUT", "BUTTON", "STYLE",
			"SELECT", "EMBED", "OBJECT", "IMG", "IFRAME" };

	// // todo: isIncludeCSS
	// // todo: isSelectTagToText

	boolean isRemoveAdLinks = true;

	boolean isRemoveTextEmptyElements = true;

	boolean isRemoveLinkCells = true;

	int substanceMinTextLength = 5; // todo ???

	int letternsPerWord = 5;

	double linkTextRatio = 0.20;

	int minNumOfWords = 15;

	boolean isRemoveAttr = true;

	String[] removeAttrNodes = { "TD", "TR", "TABLE", "BODY", "DIV", "LI", "UL" };

	boolean isRemoveCommonLinks = true;

	String adsServerListFile = "serverlist.txt";

	public void loadSettings(final String filename)
	{
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(filename));
			isRemoveComments = getBoolean(properties.getProperty("isRemoveComments"),
					isRemoveComments);
			isKeepTags = getBoolean(properties.getProperty("isKeepTags"), isKeepTags);
			keepTags = getStringArray(properties.getProperty("keepTags"), keepTags);
			isProcessMeta = getBoolean(properties.getProperty("isProcessMeta"),
					isProcessMeta);
			isRemoveAllMeta = getBoolean(properties.getProperty("isRemoveAllMeta"),
					isRemoveAllMeta);
			isRemoveTags = getBoolean(properties.getProperty("isRemoveTags"),
					isRemoveTags);
			removeTags = getStringArray(properties.getProperty("removeTags"),
					removeTags);
			isRemoveAdLinks = getBoolean(properties.getProperty("isRemoveAdLinks"),
					isRemoveAdLinks);
			isRemoveTextEmptyElements = getBoolean(properties
					.getProperty("isRemoveTextEmptyElements"), isRemoveTextEmptyElements);
			isRemoveLinkCells = getBoolean(properties
					.getProperty("isRemoveLinkCells"), isRemoveLinkCells);
			substanceMinTextLength = getInt(properties
					.getProperty("substanceMinTextLength"), substanceMinTextLength);
			letternsPerWord = getInt(properties.getProperty("letternsPerWord"),
					letternsPerWord);
			linkTextRatio = getDouble(properties.getProperty("linkTextRatio"),
					linkTextRatio);
			minNumOfWords = getInt(properties.getProperty("minNumOfWords"),
					minNumOfWords);
			isRemoveAttr = getBoolean(properties.getProperty("isRemoveAttr"),
					isRemoveAttr);
			removeAttrNodes = getStringArray(properties
					.getProperty("removeAttrNodes"), removeAttrNodes);
			isRemoveCommonLinks = getBoolean(properties
					.getProperty("isRemoveCommonLinks"), isRemoveCommonLinks);
			adsServerListFile = properties.getProperty("adsServerListFile",
					adsServerListFile);
		} catch (IOException e) {
			logger.warn("Failed to load confing file " + filename, e);
		}
	}

	public boolean getBoolean(String b, boolean def)
	{
		Boolean bool = new Boolean(def);
		try {
			bool = Boolean.valueOf(b);
		} catch (Exception e) {
		}
		return bool.booleanValue();
	}

	public String[] getStringArray(String str, String[] def)
	{
		String[] ar = def;
		try {
			ar = str.split(" ");
		} catch (Exception e) {
		}
		return ar;
	}

	public int getInt(String str, int def)
	{
		Integer i = new Integer(def);
		try {
			i = new Integer(str);
		} catch (Exception e) {
		}
		return i.intValue();
	}

	public double getDouble(String str, double def)
	{
		Double i = new Double(def);
		try {
			i = new Double(str);
		} catch (Exception e) {
		}
		return i.doubleValue();
	}

}
