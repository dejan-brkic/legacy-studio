/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2013 Crafter Software Corporation.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.craftercms.cstudio.alfresco.util;



import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class provides common methods for handling XML
 * 
 * @author hyanghee
 * 
 */
public class XmlUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(XmlUtils.class);
	/**
	 * load XML from the given input stream
	 * 
	 * @param in
	 * @return Document
	 * @throws DocumentException
	 */
	public static Document loadXml(InputStream in) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(in);
		return document;
	}

	/**
	 * convert document to string
	 * 
	 * @param document
	 * @return XML as String
	 * @throws IOException
	 */
	public static String convertDocumentToString(Document document) throws IOException {
		StringWriter sw = new StringWriter();
		XMLWriter writer = new XMLWriter(sw);
		try {
			writer.write(document);
			writer.flush();
			return sw.toString();
		} finally {
			sw.close();
			writer.close();
		}
	}
	
	/**
	 * convert xml string to document
	 * 
	 * @param xmlString
	 * @return document
	 */
	public static org.w3c.dom.Document convertStringToW3cDocument(String xmlString) {		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		StringReader reader = null;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
	        reader = new StringReader(xmlString);
	        org.w3c.dom.Document document = builder.parse(new InputSource(reader));
	        reader.close();
	        return document;
		} catch (ParserConfigurationException e) {
			LOGGER.error("failed to parse string to document: " + xmlString, e);
		} catch (SAXException e) {
			LOGGER.error("failed to parse string to document: " + xmlString, e);
		} catch (IOException e) {
			LOGGER.error("failed to parse string to document: " + xmlString, e);
		} finally {
			reader.close();
		}
		return null;
	}

}
