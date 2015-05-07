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
import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.base.topics.TopicUtils;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.soap.envelope.Envelope;

import javax.xml.bind.JAXBElement;

import static org.testng.Assert.*;

public class WSNToolsTest {

    String dialect = WSNTools._ConcreteTopicExpression;
    Message message = new Message("<data>test</data>", "test/sub", null, "Test");

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
        assertTrue(WSNTools.buildGenericContentElement("data") instanceof org.w3c.dom.Element);
        org.w3c.dom.Element genericContentElement = WSNTools.buildGenericContentElement("data");
        assertEquals(genericContentElement.getTextContent(), "data");
        assertEquals(genericContentElement.getTagName(), "Content");
    }

    @Test
    public void testParseRawXmlString() throws Exception {
        String rawXml = WSNTools.generateRawSoapEnvelopedNotifyString(message);
        InternalMessage result = WSNTools.parseRawXmlString(rawXml);
        assertNotNull(result.getMessage());
        result = WSNTools.parseRawXmlString("<herp derp<");
        assertEquals(result.getMessage(), null);
    }

    @Test
    public void testCreateNotify() throws Exception {
        Object result = WSNTools.createNotify(message);
        assertTrue(result instanceof Notify);
        Object content = WSNTools.extractMessageContentFromNotify((Notify) result);
        assertEquals(((Node) content).getTextContent(), "test");
    }

    @Test
    public void testExtractMessageContentFromNotify() throws Exception {
        Notify msg = WSNTools.createNotify(message);
        Object content = WSNTools.extractMessageContentFromNotify(msg);
        assertEquals(((Node) content).getTextContent(), "test");
    }

    @Test
    public void testInjectMessageContentIntoNotify() throws Exception {
        Notify msg = WSNTools.createNotify(message);
        Element generatedContent = WSNTools.buildGenericContentElement("updated");
        WSNTools.injectMessageContentIntoNotify(generatedContent, msg);
        if (((Node) msg.getNotificationMessage().get(0).getMessage().getAny()).getTextContent().equals("test")) {
            fail("Message content should be 'updated', not 'test'");
        }
        assertEquals(((Node) msg.getNotificationMessage().get(0).getMessage().getAny()).getTextContent(), "updated");
    }

    @Test
    public void testRemoveNameSpacePrefixesFromTopicExpression() throws Exception {
        String topicExpression = "ns:topic/ns2:topicsub";
        String topicExpression2 = "topic/ns3:topic2/ns4:topic5";
        String topicExpression3 = "ns3:topic/skeet/ns5:topic1";
        String topicExpression4 = "ns:topic";
        String expectedTopic = "topic/topicsub";
        String expectedTopic2 = "topic/topic2/topic5";
        String expectedTopic3 = "topic/skeet/topic1";
        String expectedTopic4 = "topic";

        String result = WSNTools.removeNameSpacePrefixesFromTopicExpression(topicExpression);
        String result2 = WSNTools.removeNameSpacePrefixesFromTopicExpression(topicExpression2);
        String result3 = WSNTools.removeNameSpacePrefixesFromTopicExpression(topicExpression3);
        String result4 = WSNTools.removeNameSpacePrefixesFromTopicExpression(topicExpression4);

        assertEquals(result, expectedTopic);
        assertEquals(result2, expectedTopic2);
        assertEquals(result3, expectedTopic3);
        assertEquals(result4, expectedTopic4);
    }

    @Test
    public void testBuildNotifyWithContext() throws Exception {
        WSNTools.NotifyWithContext notify = WSNTools.buildNotifyWithContext("test", "test/sub", "ox", "http://okse.default.message");
        Notify n = notify.notify;
        NuNamespaceContextResolver ncr = notify.nuNamespaceContextResolver;
        assertTrue(ncr.resolveNamespaceContext(n.getNotificationMessage().get(0).getMessage()).getAllPrefixes().contains("ox"));
        assertEquals(TopicUtils.extractExpression(n.getNotificationMessage().get(0).getTopic()), "ox:test/sub");
        assertEquals(((Node) WSNTools.extractMessageContentFromNotify(n)).getTextContent(), "test");
    }

    @Test
    public void testBuildGenericContentElement() throws Exception {
        Element result = WSNTools.buildGenericContentElement("elementtest");
        assertTrue(result instanceof Element);
        assertEquals(result.getTextContent(), "elementtest");
        assertEquals(result.getTagName(), "Content");
    }

    @Test
    public void testExtractSubscriptionReferenceFromRawXmlResponse() throws Exception {
        String xmlSubscribeResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<ns8:Envelope xmlns:ns2=\"http://www.w3.org/2001/12/soap-envelope\" " +
                "xmlns:ns3=\"http://docs.oasis-open.org/wsrf/bf-2\" " +
                "xmlns:ns4=\"http://www.w3.org/2005/08/addressing\" " +
                "xmlns:ns5=\"http://docs.oasis-open.org/wsn/b-2\" " +
                "xmlns:ns6=\"http://docs.oasis-open.org/wsn/t-1\" " +
                "xmlns:ns7=\"http://docs.oasis-open.org/wsn/br-2\" " +
                "xmlns:ns8=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ns9=\"http://docs.oasis-open.org/wsrf/r-2\">" +
                "<ns8:Body><ns5:SubscribeResponse>" +
                "<ns5:SubscriptionReference>" +
                "<ns4:Address>http://128.128.128.128:61000/subscriptionManager/?wsn-subscriberkey=6ff9843f23f38547a3f97db4304099abdef0bb11</ns4:Address>" +
                "</ns5:SubscriptionReference><ns5:TerminationTime>2016-01-01T00:00:00.000+01:00</ns5:TerminationTime>" +
                "</ns5:SubscribeResponse>" +
                "</ns8:Body>" +
                "</ns8:Envelope>";

        String subscriptionReference = WSNTools.
    }
}