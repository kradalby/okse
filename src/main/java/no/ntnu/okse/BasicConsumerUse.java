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
        import org.ntnunotif.wsnu.base.topics.TopicUtils;
        import org.ntnunotif.wsnu.base.topics.TopicValidator;
        import org.ntnunotif.wsnu.base.util.InternalMessage;
        import org.ntnunotif.wsnu.base.util.Log;
        import org.ntnunotif.wsnu.services.eventhandling.ConsumerListener;
        import org.ntnunotif.wsnu.services.eventhandling.NotificationEvent;
        import org.ntnunotif.wsnu.services.general.WsnUtilities;
        import org.ntnunotif.wsnu.services.implementations.notificationconsumer.NotificationConsumerImpl;
        import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
        import org.oasis_open.docs.wsn.b_2.Subscribe;
        import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
        import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
        import org.oasis_open.docs.wsn.bw_2.MultipleTopicsSpecifiedFault;
        import org.oasis_open.docs.wsn.bw_2.TopicExpressionDialectUnknownFault;

        import javax.xml.namespace.NamespaceContext;
        import javax.xml.namespace.QName;
        import java.util.List;

/**
 * A very simple example showing just how easy a consumer may be built.
 */
public class BasicConsumerUse {

    public static void main(String[] args) {
        // Disable logging
        Log.setEnableDebug(false);
        Log.setEnableWarnings(false);
        Log.setEnableErrors(false);

        // This example shows just how simple it is to create a Consumer.

        // First you need an instance of the NotificationConsumerImpl class. This will be the class you interact with to
        // register with producers/ brokers, and where you tell your program where to send the notify messages.
        NotificationConsumerImpl notificationConsumer = new NotificationConsumerImpl();

        // We would also need to know its endpoint reference (the part directly after our root address)
        final String endpoint = "myConsumer";

        // To get it up and able to receive Notify messages, it needs to be registered with a hub that can direct it
        // onto the Internet. The simplest way of doing this would be to run the quickBuild command. The returned hub is
        // there to enable multiple Web Services on one hub.
        SoapForwardingHub hub = notificationConsumer.quickBuild(endpoint);

        // Well, at this point, the consumer can receive notifies. But nothing happens to them. To change this, we need
        // to add a listeners that listens for notifies. Let us make a class that listens to these events.
        notificationConsumer.addConsumerListener(new SimpleConsumerListener());

        // Well this is all well. But we still do not receive any notifies. The reason behind this is of course that no
        // producers knows we exists, and therefore do not know that we wish to receive any messages.


        // To fix this, we need to know where a producer is located, its complete address.
        // Let us just assume it is located here:
        String producerAddress = "http://m.fap.no:58080/myBroker";
        // And now register us as consumers
        InternalMessage reply = WsnUtilities.sendSubscriptionRequest(producerAddress, notificationConsumer.getEndpointReference(), notificationConsumer.getHub());

        // We expect the request to fail, there are no producers on this address:
        if ((reply.statusCode & InternalMessage.STATUS_FAULT) != 0) {
            System.out.println("We failed to generate a subscription, as expected");
        } else {
            System.err.println("The subscription succeeded when it should not");
        }

        // And we should now get the notifies, if the producer existed
        //System.exit(0);
    }

    /**
     * A class implementing a Consumer listener. It writes out when it receives notifies, how many messages was in that
     * notify and if there was a topic connected to that notify.
     */
    public static class SimpleConsumerListener implements ConsumerListener {
        @Override
        public void notify(NotificationEvent event) {
            // Here we can do anything with the event. Or nothing if we choose to. To just show we got a message:
            System.out.println("Received notify!");
            System.out.println("\tNumber of messages in the notify: " + event.getMessage().size());

            // !!! --- THE FOLLOWING CODE IS OPTIONAL, AND ONLY SERVES AS REFERENCE FOR MORE ADVANCED USE --- !!!

            // To do something more interesting, loop through the messages received, and write out which topics
            // they were on (if any).
            for (NotificationMessageHolderType holderType : event.getRaw().getNotificationMessage()) {

                TopicExpressionType topic = holderType.getTopic();

                if (topic == null) {
                    // this message did not have a topic
                    System.out.println("\tNO TOPIC on this message");
                } else {
                    // Print out the topic
                    try {

                        // The event holds the request information, which stores the context it stands in
                        NamespaceContext namespaceContext = event.getRequestInformation().getNamespaceContext(topic);
                        // The context is needed to understand which topic it was
                        List<QName> topicAsList = TopicValidator.evaluateTopicExpressionToQName(topic, namespaceContext);

                        // A topic may be represented as a String
                        String topicAsString = TopicUtils.topicToString(topicAsList);
                        // And printed for our convenience
                        System.out.println("\tTOPIC: " + topicAsString);

                    } catch (InvalidTopicExpressionFault invalidTopicExpressionFault) {
                        // We could not understand the expression
                        System.out.println("\tWARNING a message with not understandable topic was received");
                    } catch (MultipleTopicsSpecifiedFault multipleTopicsSpecifiedFault) {
                        // The topic expression was evaluated to mean more than one topic
                        System.out.println("\tWARNING a message with multiple topics was received");
                    } catch (TopicExpressionDialectUnknownFault topicExpressionDialectUnknownFault) {
                        // We did not understand the dialect of the topic given
                        System.out.println("\tWARNING a message with an unknown dialect was presented");
                    }
                }
            }

            // !!! --- END OF OPTIONAL CODE --- !!!
        }
    }
}