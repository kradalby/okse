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

package no.ntnu.okse;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 12/03/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */

//-----------------------------------------------------------------------------
// Copyright (C) 2014 Tormod Haugland and Inge Edward Haulsaunet
//
// This file is part of WS-Nu.
//
// WS-Nu is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// WS-Nu is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with WS-Nu. If not, see <http://www.gnu.org/licenses/>.
//-----------------------------------------------------------------------------


        import org.ntnunotif.wsnu.base.internal.SoapForwardingHub;
        import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
        import org.ntnunotif.wsnu.base.topics.SimpleEvaluator;
        import org.ntnunotif.wsnu.base.util.Log;
        import org.ntnunotif.wsnu.services.implementations.notificationproducer.NotificationProducerImpl;
        import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
        import org.oasis_open.docs.wsn.b_2.Notify;
        import org.oasis_open.docs.wsn.b_2.ObjectFactory;
        import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
        import org.w3c.dom.Document;
        import org.w3c.dom.Element;

        import javax.xml.parsers.DocumentBuilderFactory;
        import javax.xml.parsers.ParserConfigurationException;

/**
 * A very simple example showing just how easy a producer may be built.
 */
public class BasicProducerUse {
    // Prefix and namespace used in topics
    public static final String prefix = "ens";
    public static final String namespace = "http://www.example.com/producerns";

    public static void main(String[] args) {
        // Disable logging
        Log.setEnableDebug(false);
        Log.setEnableWarnings(false);
        Log.setEnableErrors(false);

        // This example shows how simple it is to create a producer that may distribute notifies.

        // First you need an instance of the NotificationProducerImpl class. This will serve as the delegate to
        // distribute notifications. If you wish, you may extend the class, though we do not do that here.
        NotificationProducerImpl producer = new NotificationProducerImpl();
        // This producer is now set up (by default) to support GetCurrentMessage on a specific topic, and it support
        // filtering on topics and message content.

        // We would also need to know its endpoint reference (the part directly after our root address)
        final String endpoint = "exampleProducerEndpoint";

        // To get it up and able to send Notify messages, it needs to be registered with a hub that can direct it
        // onto the Internet. The simplest way of doing this would be to run the quickBuild command. The returned hub is
        // there to enable multiple Web Services on one hub.
        SoapForwardingHub hub = producer.quickBuild(endpoint);

        // At this point consumers are able to register subscriptions to our producer (as long as they know it exists).

        // To send a notify which is filtered on the given filters, we need to build it and its context.
        NotifyWithContext notifyWithContext = buildNotifyWithContext();

        // Finally, to send the notify to all who wishes to see it, we only need to run
        producer.sendNotification(notifyWithContext.notify, notifyWithContext.nuNamespaceContextResolver);
    }

    /**
     * This method is a helper method to build a Notify with its context. It is meant as example on how this may be
     * solved.
     *
     * @return a notify with its context
     */
    public static NotifyWithContext buildNotifyWithContext() {

        // Create a contextResolver, and fill it with the namespace bindings used in the notify
        NuNamespaceContextResolver contextResolver = new NuNamespaceContextResolver();
        contextResolver.openScope();
        contextResolver.putNamespaceBinding(prefix, namespace);

        // Build the notify
        ObjectFactory factory = new ObjectFactory();
        Notify notify = factory.createNotify();

        // Fill it with some messages with topics
        for (int i = 0; i  < 5; i++) {
            // Create message and holder
            NotificationMessageHolderType.Message message = factory.createNotificationMessageHolderTypeMessage();
            NotificationMessageHolderType messageHolderType = factory.createNotificationMessageHolderType();

            // create message content
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
                Element element = document.createElement("message");
                element.setTextContent("Message " + i);
                message.setAny(element);
            } catch (ParserConfigurationException e) {
                System.err.println("failed to build message");
            }

            // Set holders message
            messageHolderType.setMessage(message);

            // Build topic expression
            String expression = prefix + ":root-" + (i % 3);
            // Build topic
            TopicExpressionType topicExpressionType = factory.createTopicExpressionType();
            topicExpressionType.setDialect(SimpleEvaluator.dialectURI);
            topicExpressionType.getContent().add(expression);

            messageHolderType.setTopic(topicExpressionType);

            // remember to bind the necessary objects to the context
            contextResolver.registerObjectWithCurrentNamespaceScope(message);
            contextResolver.registerObjectWithCurrentNamespaceScope(topicExpressionType);

            // Add message to the notify
            notify.getNotificationMessage().add(messageHolderType);
        }

        // ready for return
        contextResolver.closeScope();
        NotifyWithContext notifyWithContext = new NotifyWithContext();
        notifyWithContext.notify = notify;
        notifyWithContext.nuNamespaceContextResolver = contextResolver;

        return notifyWithContext;
    }

    /**
     * A wrapper class to hold a notify with its context.
     */
    public static class NotifyWithContext {
        public Notify notify;
        public NuNamespaceContextResolver nuNamespaceContextResolver;
    }
}