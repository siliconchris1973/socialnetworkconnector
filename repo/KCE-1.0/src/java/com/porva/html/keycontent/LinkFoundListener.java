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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Listen for a HTML links and store them.  
 * 
 * @author Poroshin
 */
public class LinkFoundListener implements NodeFoundListener
{
  private static final String LINK_NODE_NAME = "A";

  List<Node> linkNodes = new Vector<Node>();

  /* (non-Javadoc)
   * @see com.porva.html.keycontent.NodeFoundListener#getFoundNodes()
   */
  public List getFoundNodes()
  {
    return linkNodes;
  }

  /* (non-Javadoc)
   * @see com.porva.html.keycontent.NodeFoundListener#foundNode(org.w3c.dom.Node)
   */
  public void foundNode(Node node)
  {
    if (LinkFoundListener.isLink(node)) {
      //String link = ((Element)node).getAttribute("href");
      linkNodes.add(node);
      //System.err.println(link);
    }
  }

  /* (non-Javadoc)
   * @see com.porva.html.keycontent.NodeFoundListener#getNodeName()
   */
  public String getNodeName()
  {
    return LINK_NODE_NAME;
  }

  /**
   * Checks if a node is a link.
   * 
   * @param iNode a node to check.
   * @return true if the node is a link; false otherwise. 
   */
  public static boolean isLink(final Node iNode)
  {
    //Check to see if the node is a Text node or an element node
    int type = iNode.getNodeType();

    //Element node
    if (type == Node.ELEMENT_NODE) {
      String name = iNode.getNodeName();

      //Check to see if it is a link
      if ( name.equals("A") )
        return ((Element)iNode).hasAttribute("href");

    }

    return false;
  }

  /**
   * Returns list of found links.
   * 
   * @return list of found links.
   */
  public List<String> getLinks()
  {
    List<String> links = new ArrayList<String>();
    for (Node node : linkNodes) {
      String link = ((Element)node).getAttribute("href");
      links.add(link);
    }
    return links;
  }

}
