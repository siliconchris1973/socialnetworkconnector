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

import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

public class TitleFoundListener implements NodeFoundListener
{
	List<Node> foundNodes = new Vector<Node>();

	public static final String TITLE_NODE_NAME = "TITLE";

	private static final Log logger = LogFactory.getLog(TitleFoundListener.class);

	public void foundNode(Node node)
	{
		foundNodes.add(node);
	}

	public String getNodeName()
	{
		return TITLE_NODE_NAME;
	}

	public List getFoundNodes()
	{
		return foundNodes;
	}

	public String getTitle()
	{
		if (foundNodes.size() == 0)
			return null;

		Node node = (Node) foundNodes.get(0);
		if (node == null)
			return null;

		Node childNode = node.getFirstChild();
		if (childNode.getNodeType() == Node.TEXT_NODE) {
			return childNode.getNodeValue();
		}
		logger.warn("Failed to find title!");
		return null;
	}
}
