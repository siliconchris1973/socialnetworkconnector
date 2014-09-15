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


import com.porva.net.tana.TanaSend;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class KceServerTest extends TestCase
{
  KceServer kceServer;

  public void testKceServer() throws Exception
  {
    int port = 12233;
    String file = "/home/fs/poroshin/src/java/KeyContentExtractor/src/test/1.html";

//    kceServer = new KceServer(port);
//    Thread.sleep(100);

    TanaSend tanaSend = new TanaSend("localhost", port);
    tanaSend.connect();
    Map<String, byte[]> map = new HashMap<String, byte[]>();
    map.put("cmd", "filter".getBytes());
    String contnt = readFile(file);
    map.put("content", contnt.getBytes());
    Map answer = tanaSend.sendAndReceiveMap(map);
    System.out.println(answer);
    tanaSend.disconnect();

//    kceServer.shutdown();
  }

  private String readFile(String file) throws IOException
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