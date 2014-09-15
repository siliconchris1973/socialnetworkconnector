package com.porva.html.keycontent;

import org.apache.commons.lang.NullArgumentException;

import junit.framework.TestCase;

public class AdsServerListTest extends TestCase
{
  
  String adServersFilename = "test/serverlist.txt";
  
  public void testInit()
  {
    try {
      AdsServerList.init(null);
      fail();
    } catch (NullArgumentException e) {
    }
    try {
      AdsServerList.isAdServer("some.server");
      fail();
    } catch (IllegalStateException e) {
    }
    
    AdsServerList.init(adServersFilename);
  }
  
  public void testIsAdServer()
  {
    assertFalse(AdsServerList.isAdServer("some.server"));
    assertFalse(AdsServerList.isAdServer(null));
    
    assertTrue(AdsServerList.isAdServer("znext.com"));
  }

}
