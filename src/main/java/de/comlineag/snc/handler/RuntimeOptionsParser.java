package de.comlineag.snc.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.comlineag.snc.data.RuntimeOption;

public class RuntimeOptionsParser extends DefaultHandler {
    private final StringBuilder valueBuffer = new StringBuilder();
    private final Map<String, RuntimeOption> resultAsMap = new HashMap<String, RuntimeOption>();
    private final List<RuntimeOption> runtimeOptions = new ArrayList<RuntimeOption>();

    //variable to store the values from xml temporarily
    private RuntimeOption temp;

    public List<RuntimeOption> getOptions() {
        return runtimeOptions;
    }

    public Map<String, RuntimeOption> getResultAsMap() {
        return resultAsMap;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes) throws SAXException {
        if("option".equalsIgnoreCase(qName)) {
            temp = new RuntimeOption();
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName)
            throws SAXException {
        // read the value into a string to set them to option object
        final String value = valueBuffer.toString().trim();
        switch (qName) {
        case "name":
            temp.setName(value);
            // set the value into map with name of the option as the key
            resultAsMap.put(value, temp);
            break;
        case "type":
            temp.setType(value);
            break;
        case "value":
            temp.setValue(value);
            break;
        case "constant":
            temp.setConstant(value);
            break;
        case "option":
            // this is the end of option tag add it to the list
            runtimeOptions.add(temp);
            temp = null;
            break;
        default:
            break;
        }
        //reset the buffer after every iteration
        valueBuffer.setLength(0);
    }

    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        //read the value into a buffer
        valueBuffer.append(ch, start, length);
    }
}