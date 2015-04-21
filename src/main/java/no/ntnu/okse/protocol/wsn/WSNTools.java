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
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.t_1.*;

import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

/**
 * Created by Aleksander Skraastad (myth) on 4/21/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNTools {

    // Initialize the WSN XML Object factories
    public static org.oasis_open.docs.wsn.b_2.ObjectFactory b2_factory = new org.oasis_open.docs.wsn.b_2.ObjectFactory();
    public static org.oasis_open.docs.wsn.t_1.ObjectFactory t1_factory = new org.oasis_open.docs.wsn.t_1.ObjectFactory();

    // Namespace references
    public static final String _ConcreteTopicExpression = "http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete";
    public static final String _SimpleTopicExpression = "http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple";

    /**
     * Generates a WS-Notification Notification Message from the provided arguments
     *
     * @param message An OKSE message object containing all the required fields
     * @param topic The raw topic string this message is destined for
     * @param subscriptionReference The subscription reference endpoint
     * @param publisherReference The publisher reference endpoint
     * @param dialect The dialect used for this message
     * @return A completely built NotificationMessageHolderType used in WS-Nu for storing notifications
     */
    public static NotificationMessageHolderType generateNotificationMessageHolderType(
        Message message,
        String topic,
        String subscriptionReference,
        String publisherReference,
        String dialect
    ) {
        // Create the HolderType
        NotificationMessageHolderType holderType = b2_factory.createNotificationMessageHolderType();
        // Create the Message object
        NotificationMessageHolderType.Message innerMessage = b2_factory.createNotificationMessageHolderTypeMessage();
        // Create the TopicExpressionType
        TopicExpressionType topicType = b2_factory.createTopicExpressionType();
        // Set the Dialect on the TopicExpressionType
        topicType.setDialect(dialect);
        // Add the actual topic to the TopicExpressionType
        topicType.getContent().add(topic);
        // Set the Topic on the HolderType
        holderType.setTopic(topicType);

        // Set the actual contents of the message to the Message element
        innerMessage.setAny("Test");

        W3CEndpointReferenceBuilder endRefBuilder = new W3CEndpointReferenceBuilder();
        if (!message.getPublisher().getAttribute("wsn-endpoint").equals(null)) {
            endRefBuilder.address(message.getPublisher().getHostAndPort() + message.getPublisher().getAttribute(WSNSubscriptionManager.WSN_ENDPOINT_TOKEN));
        } else {
            endRefBuilder.address(message.getPublisher().getHostAndPort());
        }

        holderType.setProducerReference(endRefBuilder.build());

        return holderType;
    }
}
