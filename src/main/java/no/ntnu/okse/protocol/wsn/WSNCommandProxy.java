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

import no.ntnu.okse.Application;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.services.eventhandling.PublisherRegistrationEvent;
import org.ntnunotif.wsnu.services.eventhandling.SubscriptionEvent;
import org.ntnunotif.wsnu.services.filterhandling.FilterSupport;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.*;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Aleksander Skraastad (myth) on 4/10/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@WebService(targetNamespace = "http://docs.oasis-open.org/wsn/brw-2", name = "NotificationBroker")
@XmlSeeAlso({org.oasis_open.docs.wsn.t_1.ObjectFactory.class, org.oasis_open.docs.wsn.br_2.ObjectFactory.class, org.oasis_open.docs.wsrf.r_2.ObjectFactory.class, org.oasis_open.docs.wsrf.bf_2.ObjectFactory.class, org.oasis_open.docs.wsn.b_2.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class WSNCommandProxy extends AbstractNotificationBroker {

    private FilterSupport filterSupport;
    private boolean cacheMessages;
    private final Map<String, NotificationMessageHolderType> latestMessages = new HashMap<>();
    private WSNSubscriptionManager subscriptionManager;

    public WSNCommandProxy(Hub hub, WSNSubscriptionManager subscriptionManager) {
        this.setHub(hub);
        this.filterSupport = FilterSupport.createDefaultFilterSupport();
        this.cacheMessages = true;
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    @WebMethod(exclude = true)
    public boolean keyExists(String s) {
        return subscriptionManager.keyExists(s);
    }

    @Override
    @WebMethod(exclude = true)
    protected Collection<String> getAllRecipients() {
        return subscriptionManager.getAllRecipients();
    }

    @Override
    protected String getEndpointReferenceOfRecipient(String subscriptionKey) {
        return this.subscriptionManager.getSubscriptionHandle(subscriptionKey).endpointTerminationTuple.endpoint;
    }

    @Override
    protected Notify getRecipientFilteredNotify(String s, Notify notify, NuNamespaceContextResolver nuNamespaceContextResolver) {
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
