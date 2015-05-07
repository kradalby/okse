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

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import no.ntnu.okse.core.messaging.Message;
import org.ntnunotif.wsnu.base.topics.TopicUtils;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import javax.xml.bind.Element;
import javax.xml.bind.JAXBElement;

import static org.testng.Assert.*;

public class WSNToolsTest {

    String dialect = WSNTools._ConcreteTopicExpression;

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testGenerateRawSoapEnvelopedNotifyString() throws Exception {
        String topic = "test/sub";
        String messageContent = "<data>test</data>";
        String rawSoapEnvelopedNotifyMessage = WSNTools.generateRawSoapEnvelopedNotifyString(topic, dialect, messageContent);
        InternalMessage result = WSNTools.parseRawXmlString(rawSoapEnvelopedNotifyMessage);
        JAXBElement jaxb = (JAXBElement) result.getMessage();
        Envelope env = (Envelope) jaxb.getValue();
        Notify notify = (Notify) env.getBody().getAny().get(0);
        assertEquals(WSNTools._ConcreteTopicExpression, notify.getNotificationMessage().get(0).getTopic().getDialect());
        assertEquals(((Node) WSNTools.extractMessageContentFromNotify(notify)).getTextContent(), "test");
        assertEquals(TopicUtils.extractExpression(notify.getNotificationMessage().get(0).getTopic()), "test/sub");
    }

    @Test
    public void testGenerateRawSoapEnvelopedNotifyString1() throws Exception {
        Message message = new Message("<data>test</data>", "test/sub", null, "Test");
        message.setAttribute(WSNSubscriptionManager.WSN_DIALECT_TOKEN, dialect);
        String rawSoapEnvelopedNotifyMessage = WSNTools.generateRawSoapEnvelopedNotifyString(message);
        InternalMessage result = WSNTools.parseRawXmlString(rawSoapEnvelopedNotifyMessage);
        JAXBElement jaxb = (JAXBElement) result.getMessage();
        Envelope env = (Envelope) jaxb.getValue();
        Notify notify = (Notify) env.getBody().getAny().get(0);
        assertEquals(WSNTools._ConcreteTopicExpression, notify.getNotificationMessage().get(0).getTopic().getDialect());
        assertEquals(((Node) WSNTools.extractMessageContentFromNotify(notify)).getTextContent(), "test");
        assertEquals(TopicUtils.extractExpression(notify.getNotificationMessage().get(0).getTopic()), "test/sub");
    }

    @Test
    public void testExtractRawXmlContentFromDomNode() throws Exception {
        
    }

    @Test
    public void testParseRawXmlString() throws Exception {

    }

    @Test
    public void testCreateNotify() throws Exception {

    }

    @Test
    public void testExtractMessageContentFromNotify() throws Exception {

    }

    @Test
    public void testInjectMessageContentIntoNotify() throws Exception {

    }

    @Test
    public void testRemoveNameSpacePrefixesFromTopicExpression() throws Exception {

    }

    @Test
    public void testBuildNotifyWithContext() throws Exception {

    }

    @Test
    public void testBuildGenericContentElement() throws Exception {

    }

    @Test
    public void testExtractSubscriptionReferenceFromRawXmlResponse() throws Exception {

    }
}