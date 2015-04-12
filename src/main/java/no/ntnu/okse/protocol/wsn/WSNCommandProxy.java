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
import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.base.topics.TopicUtils;
import org.ntnunotif.wsnu.base.topics.TopicValidator;
import org.ntnunotif.wsnu.base.util.Utilities;
import org.ntnunotif.wsnu.services.eventhandling.PublisherRegistrationEvent;
import org.ntnunotif.wsnu.services.eventhandling.SubscriptionEvent;
import org.ntnunotif.wsnu.services.filterhandling.FilterSupport;
import org.ntnunotif.wsnu.services.general.ExceptionUtilities;
import org.ntnunotif.wsnu.services.general.HelperClasses;
import org.ntnunotif.wsnu.services.general.ServiceUtilities;
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;
import org.ntnunotif.wsnu.services.implementations.subscriptionmanager.AbstractSubscriptionManager;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.*;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import java.util.*;

/**
 * Created by Aleksander Skraastad (myth) on 4/10/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@WebService(targetNamespace = "http://docs.oasis-open.org/wsn/brw-2", name = "NotificationBroker")
@XmlSeeAlso({org.oasis_open.docs.wsn.t_1.ObjectFactory.class, org.oasis_open.docs.wsn.br_2.ObjectFactory.class, org.oasis_open.docs.wsrf.r_2.ObjectFactory.class, org.oasis_open.docs.wsrf.bf_2.ObjectFactory.class, org.oasis_open.docs.wsn.b_2.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class WSNCommandProxy extends AbstractNotificationBroker {

    private Logger log;
    private FilterSupport filterSupport;
    private boolean cacheMessages;
    private final Map<String, NotificationMessageHolderType> latestMessages = new HashMap<>();
    private WSNSubscriptionManager subscriptionManager;

    public WSNCommandProxy(Hub hub) {
        this.log = Logger.getLogger(WSNCommandProxy.class.getName());
        this.setHub(hub);
        this.filterSupport = FilterSupport.createDefaultFilterSupport();
        this.cacheMessages = true;
        this.subscriptionManager = null;
    }

    public WSNCommandProxy() {
        this.log = Logger.getLogger(WSNCommandProxy.class.getName());
        this.filterSupport = FilterSupport.createDefaultFilterSupport();
        this.cacheMessages = true;
        this.subscriptionManager = null;
    }

    // For now, set both WS-Nu submanager and OKSE submanager fields.
    public void setSubscriptionManager(WSNSubscriptionManager subManager) {
        this.subscriptionManager = subManager;
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
    @WebMethod(exclude = true)
    protected Notify getRecipientFilteredNotify(String s, Notify notify, NuNamespaceContextResolver nuNamespaceContextResolver) {
        // Check if we have the current recipient registered
        if (!this.subscriptionManager.hasSubscription(s)) {
            return null;
        }

        // If we dont have filter support, nothing more to do.
        if (this.filterSupport == null) {
            return notify;
        }

        // Find the current recipient to notify
        SubscriptionHandle subscriptionHandle = this.subscriptionManager.getSubscriptionHandle(s);

        return filterSupport.evaluateNotifyToSubscription(notify, subscriptionHandle.subscriptionInfo, nuNamespaceContextResolver);
    }

    @Override
    public void sendNotification(Notify notify, NuNamespaceContextResolver namespaceContextResolver) {
        // Check if we should cache message
        if (cacheMessages) {
            // Take out the latest messages
            for (NotificationMessageHolderType messageHolderType : notify.getNotificationMessage()) {
                TopicExpressionType topic = messageHolderType.getTopic();

                // If it is connected to a topic, remember it
                if (topic != null) {

                    try {

                        List<QName> topicQNames = TopicValidator.evaluateTopicExpressionToQName(topic, namespaceContextResolver.resolveNamespaceContext(topic));
                        String topicName = TopicUtils.topicToString(topicQNames);
                        latestMessages.put(topicName, messageHolderType);

                    } catch (InvalidTopicExpressionFault invalidTopicExpressionFault) {
                        log.warn("Tried to send a topic with an invalid expression");
                        invalidTopicExpressionFault.printStackTrace();
                    } catch (MultipleTopicsSpecifiedFault multipleTopicsSpecifiedFault) {
                        log.warn("Tried to send a message with multiple topics");
                        multipleTopicsSpecifiedFault.printStackTrace();
                    } catch (TopicExpressionDialectUnknownFault topicExpressionDialectUnknownFault) {
                        log.warn("Tried to send a topic with an invalid expression dialect");
                        topicExpressionDialectUnknownFault.printStackTrace();
                    }
                }
            }
        }
        // Super type can do the rest
        super.sendNotification(notify, namespaceContextResolver);
    }

    /**
     * Implementation of the NotificationBroker's notify. This method does nothing but forward the notify by calling
     * {@link #sendNotification(org.oasis_open.docs.wsn.b_2.Notify)}
     * @param notify The Notify object.
     */
    @Override
    @Oneway
    @WebMethod(operationName = "Notify")
    public void notify(@WebParam(partName = "Notify", name = "Notify", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
                       Notify notify) {
        this.sendNotification(notify);
    }

    /**
     * The Subscribe request message as defined by the WS-N specification.
     *
     * More information can be found at http://docs.oasis-open.org/wsn/wsn-ws_base_notification-1.3-spec-os.htm#_Toc133735624
     * @param subscribeRequest A {@link org.oasis_open.docs.wsn.b_2.Subscribe} object.
     * @return A {@link org.oasis_open.docs.wsn.b_2.SubscribeResponse} if the subscription was added successfully.
     * @throws NotifyMessageNotSupportedFault Never.
     * @throws UnrecognizedPolicyRequestFault Never, policies will not be added until 2.0.
     * @throws TopicExpressionDialectUnknownFault  If the topic expression was not valid.
     * @throws ResourceUnknownFault Never, WS-Resources is not added as of 0.3
     * @throws InvalidTopicExpressionFault If any topic expression added was invalid.
     * @throws UnsupportedPolicyRequestFault Never, policies will not be added until 2.0
     * @throws InvalidFilterFault If the filter was invalid.
     * @throws InvalidProducerPropertiesExpressionFault Never.
     * @throws UnacceptableInitialTerminationTimeFault If the subscription termination time was invalid.
     * @throws SubscribeCreationFailedFault If any internal or general fault occured during the processing of a subscription request.
     * @throws TopicNotSupportedFault If the topic in some way is unknown or unsupported.
     * @throws InvalidMessageContentExpressionFault Never.
     */
    @Override
    @WebMethod(operationName = "Subscribe")
    public SubscribeResponse subscribe(@WebParam(partName = "SubscribeRequest", name = "Subscribe",
            targetNamespace = "http://docs.oasis-open.org/wsn/b-2") Subscribe subscribeRequest) throws NotifyMessageNotSupportedFault, UnrecognizedPolicyRequestFault, TopicExpressionDialectUnknownFault, ResourceUnknownFault, InvalidTopicExpressionFault, UnsupportedPolicyRequestFault, InvalidFilterFault, InvalidProducerPropertiesExpressionFault, UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault, TopicNotSupportedFault, InvalidMessageContentExpressionFault {

        W3CEndpointReference consumerEndpoint = subscribeRequest.getConsumerReference();

        if (consumerEndpoint == null) {
            ExceptionUtilities.throwSubscribeCreationFailedFault("en", "Missing endpointreference");
        }

        String endpointReference = ServiceUtilities.getAddress(consumerEndpoint);

        // EndpointReference is returned as "" from getAddress if something went wrong.
        if(endpointReference.equals("")){
            ExceptionUtilities.throwSubscribeCreationFailedFault("en", "EndpointReference malformatted or missing.");
        }

        FilterType filters = subscribeRequest.getFilter();
        Map<QName, Object> filtersPresent = null;

        if (filters != null) {
            log.info("Filters present. Attempting to iterate over filters...");
            filtersPresent = new HashMap<>();

            for (Object o : filters.getAny()) {

                if (o instanceof JAXBElement) {
                    JAXBElement filter = (JAXBElement) o;

                    log.debug("Fetching namespacecontext of filter value");
                    // Get the namespace context for this filter
                    NamespaceContext namespaceContext = connection.getRequestInformation().getNamespaceContext(filter.getValue());

                    // Filter legality checks
                    if (filterSupport != null &&
                            filterSupport.supportsFilter(filter.getName(), filter.getValue(), namespaceContext)) {

                        QName fName = filter.getName();

                        log.debug("Subscription request contained filter: " + fName + " Value: " + filter.getValue());
                        TopicExpressionType type = (TopicExpressionType) filter.getValue();
                        log.debug("Attributes: " + type.getOtherAttributes());
                        type.getContent().stream().forEach(p -> log.info("Content: " + p.toString()));
                        log.info("Dialect: " + type.getDialect());

                        filtersPresent.put(fName, filter.getValue());
                    } else {
                        log.warn("Subscription attempt with non-supported filter: " + filter.getName());
                        ExceptionUtilities.throwInvalidFilterFault("en", "Filter not supported for this producer: " +
                                filter.getName(), filter.getName());
                    }

                }
            }
        }

        long terminationTime = 0;

        if (subscribeRequest.getInitialTerminationTime() != null) {
            try {
                terminationTime = ServiceUtilities.interpretTerminationTime(subscribeRequest.getInitialTerminationTime().getValue());

                if (terminationTime < System.currentTimeMillis()) {
                    ExceptionUtilities.throwUnacceptableInitialTerminationTimeFault("en", "Termination time can not be before 'now'");
                }

            } catch (UnacceptableTerminationTimeFault unacceptableTerminationTimeFault) {
                ExceptionUtilities.throwUnacceptableInitialTerminationTimeFault("en", "Malformated termination time");
            }
        } else {
            /* Set it to terminate in half a year */
            terminationTime = System.currentTimeMillis() + Application.DEFAULT_SUBSCRIPTION_TERMINATION_TIME;
        }

        SubscribeResponse response = new SubscribeResponse();

        // Create a gregCalendar instance so we can create a xml object from it
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(terminationTime);

        try {
            XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
            response.setTerminationTime(calendar);
        } catch (DatatypeConfigurationException e) {
            log.debug("Could not convert date time, is it formatted properly?");
            ExceptionUtilities.throwUnacceptableInitialTerminationTimeFault("en", "Internal error: The date was not " +
                    "convertable to a gregorian calendar-instance. If the problem persists," +
                    "please post an issue at http://github.com/tOgg1/WS-Nu");
        }

        /* Generate WS-Nu subscription hash */
        String newSubscriptionKey = generateSubscriptionKey();
        String subscriptionEndpoint = generateHashedURLFromKey(WsnUtilities.subscriptionString, newSubscriptionKey);

        /* Build endpoint reference */
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(subscriptionEndpoint);

        // Set the subscription reference on the SubscribeResponse object
        response.setSubscriptionReference(builder.build());

        /* Prepare WS-Nu components needed for a subscription */
        FilterSupport.SubscriptionInfo subscriptionInfo = new FilterSupport.SubscriptionInfo(filtersPresent, connection.getRequestInformation().getNamespaceContextResolver());
        HelperClasses.EndpointTerminationTuple endpointTerminationTuple;
        endpointTerminationTuple = new HelperClasses.EndpointTerminationTuple(endpointReference, terminationTime);
        SubscriptionHandle subscriptionHandle = new SubscriptionHandle(endpointTerminationTuple, subscriptionInfo);

        /* Prepare needed information for OKSE Subscriber object */
        String requestAddress = connection.getRequestInformation().getEndpointReference();
        Integer port = 80;
        if (requestAddress.contains(":")) {
            String[] components = requestAddress.split(":");
            if (components.length == 2) {
                requestAddress = components[0];
                port = Integer.parseInt(components[1]);
            }
        }

        String rawTopicContent = "";
        String requestDialect = "";

        // Extract topic information
        for (QName q : subscriptionInfo.getFilterSet()) {
            for (Object o : ((TopicExpressionType) filtersPresent.get(q)).getContent()) {
                rawTopicContent = o.toString();
            }
            requestDialect = ((TopicExpressionType) filtersPresent.get(q)).getDialect();
        }

        log.debug(rawTopicContent);
        log.debug(requestDialect);

        // Instanciate new OKSE Subscriber object
        Subscriber subscriber = new Subscriber(requestAddress, port, null, WSNotificationServer.getInstance().getProtocolServerType());
        // Set the wsn-subscriber hash key in attributes
        subscriber.setAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN, newSubscriptionKey);
        subscriber.setAttribute(WSNSubscriptionManager.WSN_DIALECT_TOKEN, requestDialect);

        // Register the OKSE subscriber to the SubscriptionService, via the WSNSubscriptionManager
        subscriptionManager.addSubscriber(subscriber, subscriptionHandle);


        return response;
    }

    @Override
    public RegisterPublisherResponse registerPublisher(RegisterPublisher registerPublisher) throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, ResourceUnknownFault, PublisherRegistrationRejectedFault, UnacceptableInitialTerminationTimeFault, TopicNotSupportedFault {
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
