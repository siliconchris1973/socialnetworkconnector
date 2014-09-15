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
package com.porva.html;

import org.w3c.tidy.Tidy;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentExtractor
{
	private static Tidy tidy = new Tidy();
	static {
		File tidyConfigFile = new File(System.getProperty("user.dir")
				+ File.separator + "conf" + File.separator + "jtidy.conf");
		tidy.setConfigurationFromFile(tidyConfigFile.getAbsolutePath());
	}

	public String extractFragment(String content, String fragmentName)
	{
		Pattern pattern = Pattern.compile(".*<a\\s+name=[\"\']?" + fragmentName
				+ "[\"\']?(.*?)</a>(.+?)<a\\s+name=.*", Pattern.CASE_INSENSITIVE
				| Pattern.DOTALL);
		Matcher matcher = pattern.matcher(content);

		String found = null;
		if (matcher.matches()) {
			found = matcher.group(2);
		}
		return found;
	}

	public String fragmentToHtml(String originalContent, String fragment)
	{
		String header = getHtmlHeader(originalContent);
		if (header != null)
			fragment = header + fragment;

		ByteArrayInputStream in = new ByteArrayInputStream(fragment.getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		tidy.parse(in, out);
		String res = new String(out.toByteArray()); // todo: utf-8 ???
		return res;
	}

	private String getHtmlHeader(String content)
	{
		Pattern pattern = Pattern.compile(".*(<html(.+?)<body[^>]*>).*",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher matcher = pattern.matcher(content);

		String found = null;
		if (matcher.matches()) {
			found = matcher.group(1);
		}
		return found;
	}

	// public List extractFragment(Node startNode, String fragmentName)
	// {
	// FragmentFinder fragmentFinder = new FragmentFinder(fragmentName);
	// NodeTraveller.nodeTravel(startNode, fragmentFinder);
	// return fragmentFinder.getFoundNodes();
	// }
	//
	// public String fragmentToHtml(List fragmentNodes)
	// {
	// Iterator it = fragmentNodes.iterator();
	// while (it.hasNext()) {
	// Node node = (Node)it.next();
	// System.out.println(node.toString());
	// }
	// return null;
	// }
	//
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// private static class FragmentFinder implements NodeTravellerHandler
	// {
	// private String fragmentName;
	// private List foundNodes = new Vector();
	// private boolean isStarted = false;
	//
	// public FragmentFinder(String fragmentName)
	// {
	// this.fragmentName = fragmentName;
	// }
	//
	// public List getFoundNodes()
	// {
	// return foundNodes;
	// }
	//
	// public boolean handleFoundNode(Node node)
	// {
	// if (node.getNodeName().equalsIgnoreCase("A")) {
	// Node name = node.getAttributes().getNamedItem("name");
	// if (name != null && name.getNodeValue().equals(fragmentName)) { // begin of
	// fragment
	// isStarted = true;
	// return false;
	// }
	// if (name != null && isStarted) { // end of fragment
	// isStarted = false;
	// // NodeTraveller.stop();
	// }
	// }
	//
	// if (isStarted) {
	// foundNodes.add(node);
	// }
	//
	//
	// return true;
	// }
	// }
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws IOException
	{
		String file = "/home/fs/poroshin/tmp/2003_12_01_resourceshelf_archive.html";
		String content = readFile(file);

		FragmentExtractor fragmentExtractor = new FragmentExtractor();
		String fragment = fragmentExtractor.extractFragment(content,
				"107286433778192820");
		String cleaned = fragmentExtractor.fragmentToHtml(content, fragment);
		System.out.println(cleaned);
	}

	private static String readFile(String file) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(new File(file)));
		String str;
		StringBuffer buf = new StringBuffer();
		while ((str = br.readLine()) != null) {
			buf.append(str).append("\n");
		}
		return buf.toString();
	}

}
