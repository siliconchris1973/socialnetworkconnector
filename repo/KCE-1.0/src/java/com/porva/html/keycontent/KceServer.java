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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import com.porva.net.tana.DefaultServerHandler;
import com.porva.net.tana.TanaServer;
import com.porva.util.log.ConsoleFormatter;

/**
 * TANA server for key content extractor.<br>
 * <pre>
 * Accepted TANA messages:
 * required        key         value             description
 * -----------------------------------------------------------------------------------------------------------
 *   yes           <b>cmd</b>         <b>filter</b>            command to apply key content extraction for specified content.
 *   yes           <b>content</b>     string                   HTML content to be filtered.
 *   no            <b>charset</b>     string                   character encoding of the content. ISO-8859-1 by default.
 *   no            <b>url</b>         urf-8 string             URL in UTF-8 encoding of the content.
 *   no            <b>html</b>        <b>true|false</b>        Request (do not request) result in HTML foramt.
 *   no            <b>txt</b>         <b>true|false</b>        Request (do not request) result in textual foramt.
 * </pre>
 * 
 * @author Poroshin
 */
public class KceServer
{
	private int port;

	private boolean verbose = true;

	private TanaServer tanaServer;

	public static final String DEF_CHARSET = "ISO-8859-1";

	private static KceSettings settings = new KceSettings();

	private static final Log logger = LogFactory.getLog(KceServer.class);
  
  public static final String MSG_CMD = "cmd";
  public static final String MSG_CMD_FILTER = "filter";
  public static final String MSG_CONTENT = "content";
  public static final String MSG_CHARSET = "charset";
  public static final String MSG_URL = "url";
  public static final String MSG_HTML = "html";
  public static final String MSG_TXT = "txt";

	public KceServer(final int port)
	{
		this.port = port;
		settings.loadSettings(System.getProperty("user.dir") + File.separator
				+ "conf" + File.separator + "keycontent.properties");

		KceServerHandler kceServerHandler = new KceServerHandler(logger);
		tanaServer = new TanaServer(this.port, kceServerHandler, verbose);
	}

	public void start()
	{
		tanaServer.start();
	}

	public void shutdown() throws Exception
	{
		tanaServer.shutdown();
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static class KceServerHandler extends DefaultServerHandler
	{

		public KceServerHandler(Log logger)
		{
			super(logger, "KCE");
		}

		public Map<String, byte[]> handleClientMsg(Map<String, byte[]> recvMap)
		{
			Map<String, byte[]> replyMap = new HashMap<String, byte[]>();
			try {
				String cmd = new String((byte[]) recvMap.get(MSG_CMD));
				if (cmd == null) {
					logger
							.warn("Command task is not defined: use cmd parameter to specify task");
					replyMap.put("error",
							"Command task is not defined: use cmd parameter to specify command task"
									.getBytes());
					return replyMap;
				}
				if (logger.isDebugEnabled())
					logger.debug("received command: " + cmd);

				if (cmd.equalsIgnoreCase(MSG_CMD_FILTER)) {

					// charset
					String charset = DEF_CHARSET;
					Object charsetOb = recvMap.get(MSG_CHARSET);
					if (charsetOb != null)
						charset = new String((byte[]) charsetOb);

					// content
					Object contentOb = recvMap.get(MSG_CONTENT);
					if (contentOb == null) {
						throw new Exception("content to parse is null");
					}
					String content = new String((byte[]) contentOb, charset);

					byte[] uriOb = recvMap.get(MSG_URL);
					URI uri = null;
					if (uriOb != null) {
						uri = new URI(new String(uriOb, "utf-8"));
					}

					// is html
					boolean isHtmlOut = true;
					Object isHtmlOb = recvMap.get(MSG_HTML);
					if (isHtmlOb != null) {
						String isHtmlOutStr = new String((byte[]) isHtmlOb);
						if (isHtmlOutStr != null && isHtmlOutStr.equalsIgnoreCase("false"))
							isHtmlOut = false;
					}

					// is txt
					boolean isTxtOut = false;
					Object isTxtOb = recvMap.get(MSG_TXT);
					if (isTxtOb != null) {
						String isTxtOutStr = new String((byte[]) isTxtOb);
						if (isTxtOutStr != null && isTxtOutStr.equalsIgnoreCase("true"))
							isTxtOut = true;
					}

					Map<String, byte[]> res = cmdFilter(content, isHtmlOut, isTxtOut, uri);
					replyMap.putAll(res);
				} else if (cmd.equalsIgnoreCase("stat")) {
					// todo stat cmd
				} else {
					logger.warn("unknown command requested: " + cmd);
					replyMap.put("error", ("unknown command requested: " + cmd)
							.getBytes());
				}

				replyMap.put("cmd", cmd.getBytes());
			} catch (Exception e) {
				replyMap.put("error", e.toString().getBytes());
			}
			return replyMap;
		}

		private Map<String, byte[]> cmdFilter(String content, boolean isHtmlOut,
				boolean isTxtOut, URI uri) throws Exception
		{
			Kce extractor = new Kce(settings);
			Document parsedDoc = extractor.extractKeyContent(
					new ByteArrayInputStream(content.getBytes("utf-8")), "utf-8", uri);
			Map<String, byte[]> res = new HashMap<String, byte[]>();

			if (isHtmlOut) {
				StringWriter sw = new StringWriter();
				Kce.prettyPrint(parsedDoc, "utf-8", sw);
				res.put("parsed-html", sw.toString().getBytes());
				res.put("encoding", "utf-8".getBytes());
			}
			if (isTxtOut) {
				StringWriter sw = new StringWriter();
				Kce.textPrint(parsedDoc, sw);
				res.put("parsed-txt", sw.toString().getBytes());
			}

			return res;
		}

	}

	public static void main(String[] args)
	{
		if (args.length == 0) {
			System.out.println("Usage: KeyContentServer port_number");
			System.exit(1);
		}
		Logger.getLogger("").setLevel(Level.INFO);
		Logger.getLogger("").getHandlers()[0].setFormatter(new ConsoleFormatter());

		KceServer kceServer = new KceServer(Integer.parseInt(args[0]));
		kceServer.start();
	}
}
