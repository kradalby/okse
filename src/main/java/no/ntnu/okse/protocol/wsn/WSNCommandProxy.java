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
import no.ntnu.okse.core.subscription.Publisher;
import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.topic.TopicService;

import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.base.topics.TopicUtils;
import org.ntnunotif.wsnu.base.topics.TopicValidator;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.services.eventhandling.PublisherRegistrationEvent;
import org.ntnunotif.wsnu.services.eventhandling.SubscriptionEvent;
import org.ntnunotif.wsnu.services.filterhandling.FilterSupport;
import org.ntnunotif.wsnu.services.general.ExceptionUtilities;
import org.ntnunotif.wsnu.services.general.HelperClasses;
import org.ntnunotif.wsnu.services.general.ServiceUtilities;
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;

import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.*;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.*;
import javax.jws.soap.SOAPBinding;
import javax.xml.XMLConstants;
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
    private WSNSubscriptionManager _subscriptionManager;
    private WSNRegistrationManager _registrationManager;

    public WSNCommandProxy(Hub hub) {
        this.log = Logger.getLogger(WSNCommandProxy.class.getName());
        this.setHub(hub);
        this.filterSupport = FilterSupport.createDefaultFilterSupport();
        this.cacheMessages = true;
        this._subscriptionManager = null;
        this._registrationManager = null;
    }

    public WSNCommandProxy() {
        this.log = Logger.getLogger(WSNCommandProxy.class.getName());
        this.filterSupport = FilterSupport.createDefaultFilterSupport();
        this.cacheMessages = true;
        this._subscriptionManager = null;
        this._registrationManager = null;
    }

    // For now, set both WS-Nu submanager and OKSE submanager fields.
    public void setSubscriptionManager(WSNSubscriptionManager subManager) {
        this._subscriptionManager = subManager;
        this.manager = this._subscriptionManager;
        this.usesManager = true;
    }

    // Sets the manager that this broker uses
    public void setRegistrationManager(WSNRegistrationManager pubManager) {
        this._registrationManager = pubManager;
        this.registrationManager = pubManager;
    }

    @Override
    @WebMethod(exclude = true)
    public boolean keyExists(String s) {
        return _subscriptionManager.keyExists(s);
    }

    @Override
    @WebMethod(exclude = true)
    protected Collection<String> getAllRecipients() {
        return _subscriptionManager.getAllRecipients();
    }

    @Override
    protected String getEndpointReferenceOfRecipient(String subscriptionKey) {
        return this._subscriptionManager.getSubscriptionHandle(subscriptionKey).endpointTerminationTuple.endpoint;
    }

    @Override
    @WebMethod(exclude = true)
    protected Notify getRecipientFilteredNotify(String s, Notify notify, NuNamespaceContextResolver nuNamespaceContextResolver) {
        // Check if we have the current recipient registered
        if (!this._subscriptionManager.hasSubscription(s)) {
            return null;
        }

        // If we dont have filter support, nothing more to do.
        if (this.filterSupport == null) {
            return notify;
        }

        // Find the current recipient to notify
        SubscriptionHandle subscriptionHandle = this._subscriptionManager.getSubscriptionHandle(s);

        return filterSupport.evaluateNotifyToSubscription(notify, subscriptionHandle.subscriptionInfo, nuNamespaceContextResolver);
    }

    /**
     * Will try to send the {@link org.oasis_open.docs.wsn.b_2.Notify} to the
     * {@link javax.xml.ws.wsaddressing.W3CEndpointReference} indicated.
     *
     * @param notify               the {@link org.oasis_open.docs.wsn.b_2.Notify} to send
     * @param w3CEndpointReference the reference of the receiving endpoint
     * @throws IllegalAccessException
     */
    @WebMethod(exclude = true)
    public void sendSingleNotify(Notify notify, W3CEndpointReference w3CEndpointReference) {
        // Not really needed, since we are not using the WS-Nu quickbuild, but just in case
        // we need to terminate the request if we don't have anywhere to forward
        if (hub == null) {
            log.error("Tried to send message with hub null. If a quickBuild is available," +
                    " consider running this before sending messages");
            return;
        }

        log.debug("Was told to send single notify to a target");
        // Initialize a new WS-Nu internalmessage
        InternalMessage outMessage = new InternalMessage(InternalMessage.STATUS_OK |
                InternalMessage.STATUS_HAS_MESSAGE |
                InternalMessage.STATUS_ENDPOINTREF_IS_SET,
                notify);
        // Update the requestinformation
        outMessage.getRequestInformation().setEndpointReference(ServiceUtilities.getAddress(w3CEndpointReference));
        log.debug("Forwarding Notify");
        // Pass it along to the requestparser
        hub.acceptLocalMessage(outMessage);
    }

    @Override
    public void sendNotification(Notify notify, NuNamespaceContextResolver namespaceContextResolver) {
        // If this somehow is called without WSNRequestParser set as hub, terminate
        if (hub == null) {
            log.error("Tried to send message with hub null. If a quickBuild is available," +
                    " consider running this before sending messages");
            return;
        }

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

        /* Start Message Parsing */

        // bind namespaces to topics
        for (NotificationMessageHolderType holderType : notify.getNotificationMessage()) {

            TopicExpressionType topic = holderType.getTopic();

            if (holderType.getTopic() != null) {
                NuNamespaceContextResolver.NuResolvedNamespaceContext context = namespaceContextResolver.resolveNamespaceContext(topic);

                if (context == null) {
                    continue;
                }

                context.getAllPrefixes().forEach(prefix -> {
                    // check if this is the default xmlns attribute
                    if (!prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                        // add namespace context to the expression node
                        topic.getOtherAttributes().put(new QName("xmlns:" + prefix), context.getNamespaceURI(prefix));
                    }
                });
            }
        }

        // Remember current message with context
        currentMessage = notify;
        currentMessageNamespaceContextResolver = namespaceContextResolver;

        // Derp derp
        log.info(notify.getNotificationMessage().stream().map(n -> n.getTopic().getContent()).reduce((a, b) -> b).toString());

        // For all valid recipients
        for (String recipient : this.getAllRecipients()) {

            // Filter do filter handling, if any
            Notify toSend = getRecipientFilteredNotify(recipient, notify, namespaceContextResolver);

            // If any message was left to send, send it
            if (toSend != null) {
                InternalMessage outMessage = new InternalMessage(
                        InternalMessage.STATUS_OK |
                        InternalMessage.STATUS_HAS_MESSAGE |
                        InternalMessage.STATUS_ENDPOINTREF_IS_SET,
                        toSend
                );
                // Update the requestinformation
                outMessage.getRequestInformation().setEndpointReference(getEndpointReferenceOfRecipient(recipient));
                // Pass it along to the requestparser
                hub.acceptLocalMessage(outMessage);
            }
        }
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

        log.debug("Endpointreference is: " + endpointReference);

        String requestAddress = "";
        Integer port = 80;
        if (endpointReference.contains(":")) {
            String[] components = endpointReference.split(":");
            try {
                port = Integer.parseInt(components[components.length - 1]);
                requestAddress = components[components.length - 2];
                requestAddress = requestAddress.replace("//", "");
            } catch (Exception e) {
                log.error("Failed to parse endpointReference");
            }
        }

        FilterType filters = subscribeRequest.getFilter();
        Map<QName, Object> filtersPresent = null;

        if (filters != null) {
            log.debug("Filters present. Attempting to iterate over filters...");
            filtersPresent = new HashMap<>();

            for (Object o : filters.getAny()) {

                if (o instanceof JAXBElement) {
                    JAXBElement filter = (JAXBElement) o;

                    log.info("Fetching namespacecontext of filter value");
                    // Get the namespace context for this filter
                    NamespaceContext namespaceContext = connection.getRequestInformation().getNamespaceContext(filter.getValue());

                    // Filter legality checks
                    if (filterSupport != null &&
                            filterSupport.supportsFilter(filter.getName(), filter.getValue(), namespaceContext)) {

                        QName fName = filter.getName();

                        log.debug("Subscription request contained filter: " + fName + " Value: " + filter.getValue());
                        TopicExpressionType type = (TopicExpressionType) filter.getValue();
                        type.getContent().stream().forEach(p -> log.info("Content: " + p.toString()));
                        log.debug("Attributes: " + type.getOtherAttributes());
                        log.debug("Dialect: " + type.getDialect());

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
            log.debug("Subscribe request had no termination time set, using default");
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
            log.error("Could not convert date time, is it formatted properly?");
            ExceptionUtilities.throwUnacceptableInitialTerminationTimeFault("en", "Internal error: The date was not " +
                    "convertable to a gregorian calendar-instance. If the problem persists," +
                    "please post an issue at http://github.com/tOgg1/WS-Nu");
        }

        log.debug("Generating WS-Nu subscription hash");
        /* Generate WS-Nu subscription hash */
        String newSubscriptionKey = generateSubscriptionKey();
        log.debug("Generating WS-Nu endpoint reference url to subscriptionManager using key: " + newSubscriptionKey + " and prefix: " + WsnUtilities.subscriptionString);

        String subscriptionEndpoint = this.generateHashedURLFromKey(WsnUtilities.subscriptionString, newSubscriptionKey);

        log.debug("Setting up W3C endpoint reference builder");
        /* Build endpoint reference */
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(subscriptionEndpoint);

        log.debug("Building endpoint reference to response");
        // Set the subscription reference on the SubscribeResponse object
        response.setSubscriptionReference(builder.build());

        log.debug("Preparing WS-Nu components needed for subscription");
        /* Prepare WS-Nu components needed for a subscription */
        FilterSupport.SubscriptionInfo subscriptionInfo = new FilterSupport.SubscriptionInfo(filtersPresent, connection.getRequestInformation().getNamespaceContextResolver());
        HelperClasses.EndpointTerminationTuple endpointTerminationTuple;
        endpointTerminationTuple = new HelperClasses.EndpointTerminationTuple(endpointReference, terminationTime);
        SubscriptionHandle subscriptionHandle = new SubscriptionHandle(endpointTerminationTuple, subscriptionInfo);

        log.debug("Preparing OKSE subscriber objects");
        /* Prepare needed information for OKSE Subscriber object */

        String rawTopicContent = "";
        String requestDialect = "";

        log.debug("Extracting topic information");
        // Extract topic information
        for (QName q : subscriptionInfo.getFilterSet()) {
            for (Object o : ((TopicExpressionType) filtersPresent.get(q)).getContent()) {
                rawTopicContent = o.toString();
            }
            requestDialect = ((TopicExpressionType) filtersPresent.get(q)).getDialect();
        }

        log.debug("Sending addTopic request to TopicService");
        TopicService.getInstance().addTopic(rawTopicContent);

        log.debug("Initializing OKSE subscriber object");
        // Instanciate new OKSE Subscriber object
        Subscriber subscriber = new Subscriber(requestAddress, port, rawTopicContent, WSNotificationServer.getInstance().getProtocolServerType());
        // Set the wsn-subscriber hash key in attributes
        subscriber.setAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN, newSubscriptionKey);
        subscriber.setAttribute(WSNSubscriptionManager.WSN_DIALECT_TOKEN, requestDialect);
        subscriber.setTimeout(terminationTime);

        // Register the OKSE subscriber to the SubscriptionService, via the WSNSubscriptionManager
        log.debug("Passing the subscriber to the SubscriptionService...");
        _subscriptionManager.addSubscriber(subscriber, subscriptionHandle);

        return response;
    }

    @Override
    @WebResult(name = "RegisterPublisherResponse", targetNamespace = "http://docs.oasis-open.org/wsn/br-2", partName = "RegisterPublisherResponse")
    @WebMethod(operationName = "RegisterPublisher")
    public RegisterPublisherResponse registerPublisher(RegisterPublisher registerPublisherRequest) throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, ResourceUnknownFault, PublisherRegistrationRejectedFault, UnacceptableInitialTerminationTimeFault, TopicNotSupportedFault {
        log.debug("registerPublisher called");

        // Fetch the namespace context resolver
        NuNamespaceContextResolver namespaceContextResolver = connection.getRequestInformation().getNamespaceContextResolver();

        // Extract the publisher endpoint
        W3CEndpointReference publisherEndpoint = registerPublisherRequest.getPublisherReference();
        log.debug("Publisher endpint is: " + publisherEndpoint);

        // If we do not have an endpoint, produce a soapfault
        if (publisherEndpoint == null) {
            log.error("Missing endpoint reference in publisher registration request");
            ExceptionUtilities.throwPublisherRegistrationFailedFault("en", "Missing endpointreference");
        }

        // Endpointreference extracted from the W3CEndpointReference
        String endpointReference = ServiceUtilities.getAddress(registerPublisherRequest.getPublisherReference());

        // EndpointReference is returned as "" from getAddress if something went wrong.
        if(endpointReference.equals("")){
            log.error("Failed to understand the endpoint reference");
            ExceptionUtilities.throwPublisherRegistrationFailedFault("en", "Could not register publisher, failed to " +
                    "understand the endpoint reference");
        }

        String requestAddress = "";
        Integer port = 80;
        if (endpointReference.contains(":")) {
            String[] components = endpointReference.split(":");
            try {
                port = Integer.parseInt(components[components.length - 1]);
                requestAddress = components[components.length - 2];
                requestAddress = requestAddress.replace("//", "");
            } catch (Exception e) {
                log.error("Failed to parse endpointReference");
            }
        }

        List<TopicExpressionType> topics = registerPublisherRequest.getTopic();

        String rawTopicString = "";

        for (TopicExpressionType topic : topics) {
            try {
                if (!TopicValidator.isLegalExpression(topic, namespaceContextResolver.resolveNamespaceContext(topic))) {
                    log.error("Recieved an invalid topic expression");
                    ExceptionUtilities.throwTopicNotSupportedFault("en", "Expression given is not a legal topicexpression");
                } else {
                    for (Object t : topic.getContent()) {
                        rawTopicString = (String) t;
                    }
                }
            } catch (TopicExpressionDialectUnknownFault topicExpressionDialectUnknownFault) {
                log.error("Recieved an unknown topic expression dialect");
                ExceptionUtilities.throwInvalidTopicExpressionFault("en", "TopicExpressionDialect unknown");
            }
        }

        // Fetch the termination time
        long terminationTime = registerPublisherRequest.getInitialTerminationTime().toGregorianCalendar().getTimeInMillis();

        if (terminationTime < System.currentTimeMillis()) {
            log.error("Caught an invalid termination time, must be in the future");
            ExceptionUtilities.throwUnacceptableInitialTerminationTimeFault("en", "Invalid termination time. Can't be before current time");
        }

        // Generate a new subkey
        String newSubscriptionKey = generateSubscriptionKey();
        // Generate the publisherRegistrationEndpoint
        String subscriptionEndpoint = generateHashedURLFromKey(WsnUtilities.publisherRegistrationString, newSubscriptionKey);

        // Send subscriptionRequest back if isDemand isRequested
        if (registerPublisherRequest.isDemand()) {
            log.info("Demand registration is TRUE, sending subrequest back");
            WsnUtilities.sendSubscriptionRequest(endpointReference, getEndpointReference(), getHub());
        }

        HelperClasses.EndpointTerminationTuple endpointTerminationTuple = new HelperClasses.EndpointTerminationTuple(newSubscriptionKey, terminationTime);
        PublisherHandle pubHandle = new PublisherHandle(endpointTerminationTuple, topics, registerPublisherRequest.isDemand());

        // Set up OKSE publisher object
        Publisher publisher = new Publisher(rawTopicString, requestAddress, port, WSNotificationServer.getInstance().getProtocolServerType());
        _registrationManager.addPublisher(publisher, pubHandle);

        // Initialize the response payload
        RegisterPublisherResponse response = new RegisterPublisherResponse();

        // Build the endpoint reference
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(subscriptionEndpoint);

        // Update the response with endpointreference
        response.setConsumerReference(builder.build());
        response.setPublisherRegistrationReference(publisherEndpoint);

        return response;
    }

    @Override
    public GetCurrentMessageResponse getCurrentMessage(GetCurrentMessage getCurrentMessage) throws InvalidTopicExpressionFault, TopicExpressionDialectUnknownFault, MultipleTopicsSpecifiedFault, ResourceUnknownFault, NoCurrentMessageOnTopicFault, TopicNotSupportedFault {
        log.debug("getCurrentMessage called");
        return null;
    }

    @Override
    public void publisherChanged(PublisherRegistrationEvent publisherRegistrationEvent) {
        log.debug("PublisherChanged event triggered");
    }

    @Override
    public void subscriptionChanged(SubscriptionEvent subscriptionEvent) {
        log.debug("SubscriptionChanged event triggered");
    }
}
