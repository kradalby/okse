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
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;

import javax.xml.namespace.QName;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

/**
 * Created by Aleksander Skraastad (myth) on 4/21/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNTools {
    public static NotificationMessageHolderType generateNotificationMessageHolderType(
        Message message, String topic, String publisherReference, String subscriptionReference
    ) {
        NotificationMessageHolderType holderType = new NotificationMessageHolderType();
        NotificationMessageHolderType.Message innerMessage = new NotificationMessageHolderType.Message();

        innerMessage.setAny(message.getMessage());
        holderType.setMessage(innerMessage);

        W3CEndpointReferenceBuilder endRefBuilder = new W3CEndpointReferenceBuilder();
        if (!message.getPublisher().getAttribute("wsn-endpoint").equals(null)) {
            endRefBuilder.address(message.getPublisher().getHostAndPort() + message.getPublisher().getAttribute(WSNSubscriptionManager.WSN_ENDPOINT_TOKEN));
        } else {
            endRefBuilder.address(message.getPublisher().getHostAndPort());
        }

        QName

        holderType.setProducerReference(endRefBuilder.build());

        return holderType;
    }
}
