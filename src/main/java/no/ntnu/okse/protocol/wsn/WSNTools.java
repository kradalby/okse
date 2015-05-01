/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.protocol.wsn;

import no.ntnu.okse.core.messaging.Message;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.net.XMLParser;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.b_2.ObjectFactory;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.Format;


/**
 * Created by Aleksander Skraastad (myth) on 4/21/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNTools {

    private static Logger log = Logger.getLogger(WSNTools.class.getName());

    // Initialize the WSN XML Object factories
    public static org.oasis_open.docs.wsn.b_2.ObjectFactory b2_factory = new org.oasis_open.docs.wsn.b_2.ObjectFactory();
    public static org.oasis_open.docs.wsn.t_1.ObjectFactory t1_factory = new org.oasis_open.docs.wsn.t_1.ObjectFactory();

    // Namespace references
    public static final String _ConcreteTopicExpression = "http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete";
    public static final String _SimpleTopicExpression = "http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple";

    public static String generateRawSoapEnvelopedNotifyString(String topic, String dialect, String messageContent) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<s:Envelope xmlns:ns2=\"http://www.w3.org/2001/12/soap-envelope\"\n" +
                "            xmlns:ns3=\"http://docs.oasis-open.org/wsrf/bf-2\"\n" +
                "            xmlns:wsa=\"http://www.w3.org/2005/08/addressing\"\n" +
                "            xmlns:wsnt=\"http://docs.oasis-open.org/wsn/b-2\"\n" +
                "            xmlns:ns6=\"http://docs.oasis-open.org/wsn/t-1\"\n" +
                "            xmlns:ns7=\"http://docs.oasis-open.org/wsn/br-2\"\n" +
                "            xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "            xmlns:ns9=\"http://docs.oasis-open.org/wsrf/r-2\">\n" +
                "<s:Header>\n" +
                "<wsa:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationConsumer/Notify</wsa:Action>\n" +
                "</s:Header>\n" +
                "<s:Body>\n" +
                "<wsnt:Notify>\n" +
                "<wsnt:NotificationMessage>\n" +
                "    <wsnt:Topic Dialect=\"" + dialect + "\">" + topic + "</wsnt:Topic>\n" +
                "<wsnt:Message>" + messageContent + "</wsnt:Message>\n" +
                "        </wsnt:NotificationMessage>\n" +
                "        </wsnt:Notify>\n" +
                "        </s:Body>\n" +
                "        </s:Envelope>";
    }

    public static String extractRawXmlContentFromDomNode(Node node) {
        try {
            // Create transformer
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            // Init a stringbuffer
            StringWriter buffer = new StringWriter();
            // We dont want the xml declaration
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            // Transform the node from source and beyond
            transformer.transform(new DOMSource(node), new StreamResult(buffer));
            // Convert to string
            String str = buffer.toString();
            // Return results
            return str;
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InternalMessage parseRawXmlString(String rawXmlString) {
        InputStream inputStream = new ByteArrayInputStream(rawXmlString.getBytes());
        try {
            InternalMessage returnMessage = XMLParser.parse(inputStream);
            return returnMessage;
        } catch (JAXBException e) {
            log.error("Failed to parse raw xml string");
        }
        return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
    }
}
