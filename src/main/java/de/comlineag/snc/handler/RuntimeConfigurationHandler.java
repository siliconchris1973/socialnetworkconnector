package de.comlineag.snc.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import de.comlineag.snc.data.RuntimeOption;

public class RuntimeConfigurationHandler {
	 public static void printOptions(String pathtoconfig) {
         final RuntimeOptionsParser handler = new RuntimeOptionsParser();
         try {
             SAXParserFactory.newInstance().newSAXParser()
                     .parse(pathtoconfig, handler);
         } catch (SAXException | IOException | ParserConfigurationException e) {
             System.err.println("Somethig went wrong while parsing the input file the exception is -- " + e.getMessage() + " -- ");
         }
         Map<String, RuntimeOption> result = handler.getResultAsMap();
         Collection<RuntimeOption> values = result.values();
         for (RuntimeOption option : values) {
             System.out.println(option.getName());
         }

     }
}
