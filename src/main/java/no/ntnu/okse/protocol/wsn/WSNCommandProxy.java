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

import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.services.eventhandling.PublisherRegistrationEvent;
import org.ntnunotif.wsnu.services.eventhandling.SubscriptionEvent;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.*;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import java.util.Collection;

/**
 * Created by Aleksander Skraastad (myth) on 4/10/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNCommandProxy extends AbstractNotificationBroker {
    @Override
    public boolean keyExists(String s) {
        return false;
    }

    @Override
    protected Collection<String> getAllRecipients() {
        return null;
    }

    @Override
    protected Notify getRecipientFilteredNotify(String s, Notify notify, NuNamespaceContextResolver nuNamespaceContextResolver) {
        return null;
    }

    @Override
    protected String getEndpointReferenceOfRecipient(String s) {
        return null;
    }

    @Override
    public void notify(Notify notify) {

    }

    @Override
    public RegisterPublisherResponse registerPublisher(RegisterPublisher registerPublisher) throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, ResourceUnknownFault, PublisherRegistrationRejectedFault, UnacceptableInitialTerminationTimeFault, TopicNotSupportedFault {
        return null;
    }

    @Override
    public SubscribeResponse subscribe(Subscribe subscribe) throws NotifyMessageNotSupportedFault, UnrecognizedPolicyRequestFault, TopicExpressionDialectUnknownFault, ResourceUnknownFault, InvalidTopicExpressionFault, UnsupportedPolicyRequestFault, InvalidFilterFault, InvalidProducerPropertiesExpressionFault, UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault, TopicNotSupportedFault, InvalidMessageContentExpressionFault {
        return null;
    }

    @Override
    public GetCurrentMessageResponse getCurrentMessage(GetCurrentMessage getCurrentMessage) throws InvalidTopicExpressionFault, TopicExpressionDialectUnknownFault, MultipleTopicsSpecifiedFault, ResourceUnknownFault, NoCurrentMessageOnTopicFault, TopicNotSupportedFault {
        return null;
    }

    @Override
    public void publisherChanged(PublisherRegistrationEvent publisherRegistrationEvent) {

    }

    @Override
    public void subscriptionChanged(SubscriptionEvent subscriptionEvent) {

    }
}
