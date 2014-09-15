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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Static class to check ad servers.
 *
 * @author Poroshin V.
 * @date Feb 9, 2006
 */
public class AdsServerList
{
  private static final Log logger = LogFactory.getLog(AdsServerList.class);

  private static HashSet<String> adServers = null;

  /**
   * Init {@link AdsServerList} with list of ads server from file <code>adsServerFile</code>.
   * 
   * @param adsServerFilename file of ads servers. One ad server name per line.
   * @throws NullArgumentException if <code>adsServerFile</code> is <code>null</code>.
   */
  public static void init(final String adsServerFilename)
  {
    if (adsServerFilename == null)
      throw new NullArgumentException("adsServerFile");

    if (adServers == null)
      loadAdsServerList(adsServerFilename);
  }

  /**
   * Returns <code>true</code> if given <code>servername</code> is an ad server;
   * <code>false</code> othrewise.
   * 
   * @param servername name of the server to check for adness.
   * @return <code>true</code> if given <code>servername</code> is an ad server;
   * <code>false</code> othrewise.
   * @throws IllegalStateException if {@link AdsServerList} is not initialized.
   */
  public static boolean isAdServer(final String servername)
  {
    if (adServers == null)
      throw new IllegalStateException("AdsServerList is not initialized.");

    return adServers.contains(servername);
  }

  /**
   * Loads the ad file into a hashtable
   */
  private static void loadAdsServerList(final String adsServerFile)
  {
    assert adsServerFile != null;

    adServers = new HashSet<String>();

    try {
      InputStream is = null;
      File adServerList = new File(adsServerFile);
      if (!adServerList.exists() || !adServerList.canRead())
        is = getResourceAsStream(adsServerFile);
      else
        is = new FileInputStream(adServerList);

      BufferedInputStream bis = new BufferedInputStream(is);
      InputStreamReader isr = new InputStreamReader(bis);
      BufferedReader in = new BufferedReader(isr);
      String line = in.readLine();

      while (line != null) {
        adServers.remove(line);
        adServers.add(line);
        line = in.readLine();
      }

      logger.info("AdServerList Loaded: " + adServers.size() + " servers in list.");

    } catch (FileNotFoundException e) {
      // if the ad file is not there, don't do anything, just print
      // that the file isn't there
      logger.warn("Server list for ad remover not found.");
    } catch (NullPointerException e) {
      logger.warn("Server list for ad remover not found.");
    } catch (IOException e) {
      if ("Stream closed".equalsIgnoreCase(e.getMessage())) {
        logger.warn("Server list for ad remover not found.");
      } else {
        e.printStackTrace();
      }
    }
  }

  private static InputStream getResourceAsStream(final String name)
  {
    // enable loading from jars
    return new Object().getClass().getResourceAsStream("/" + name);
  }
}
