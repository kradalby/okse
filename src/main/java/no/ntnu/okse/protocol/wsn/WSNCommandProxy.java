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
import no.ntnu.okse.Application;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
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
    private WSNSubscriptionManager _subscriptionManager;
    private WSNRegistrationManager _registrationManager;

    public WSNCommandProxy(Hub hub) {
        this.log = Logger.getLogger(WSNCommandProxy.class.getName());
        this.setHub(hub);
        this.filterSupport = FilterSupport.createDefaultFilterSupport();
        this._subscriptionManager = null;
        this._registrationManager = null;
    }

    public WSNCommandProxy() {
        this.log = Logger.getLogger(WSNCommandProxy.class.getName());
        this.filterSupport = FilterSupport.createDefaultFilterSupport();
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

    /**
     * Returns the WSNSubscriptionManager associated with this broker proxy
     *
     * @return The WSNSubscriptionManager instance
     */
    public WSNSubscriptionManager getProxySubscriptionManager() {
        return this._subscriptionManager;
    }

    /**
     * Returns the WSNRegistrationManager associated with this broker proxy
     *
     * @return The WSNRegistrationManager instance
     */
    public WSNRegistrationManager getProxyRegistrationManager() {
        return this._registrationManager;
    }

    /**
     * Helper method that throws a SOAP fault if a subscribers consumer reference would cause local
     * loopback on the broker.
     *
     * @param reference The consumer reference of the
     * @throws SubscribeCreationFailedFault If subscriber reference would cause loopback
     */
    private void throwFaultIfConsumerRefWouldCauseLoopback(String reference) throws SubscribeCreationFailedFault {
        boolean isLegal = true;
        // Strip off protocol
        reference = reference.replace("http://", "").replace("https://", "");
        // Fetch WAN host and port from config
        String pubWanHost = WSNotificationServer.getInstance().getPublicWANHost();
        Integer pubWanPort = WSNotificationServer.getInstance().getPublicWANPort();
        // List the illegal hosts
        HashSet<String> illegal = new HashSet<>(Arrays.asList("0.0.0.0", "localhost", "127.0.0.1", pubWanHost));
        // Split at host port intersection
        String[] parts = reference.split(":");
        // If the host is in illegal set
        if (illegal.contains(parts[0])) {
            // If not port was specified
            if (parts.length == 1) {
                // If it was the WAN host, check for default port
                if (parts[0].equals(pubWanHost) && pubWanPort == 80) {
                    isLegal = false;
                } else {
                    // Check for defaultport at regular port
                    if (WSNotificationServer.getInstance().getPort() == 80) {
                        isLegal = false;
                    }
                }
            } else {
                // Attempt to split away potential subpath
                String[] portSplit = parts[1].split("/");
                Integer refPort = Integer.parseInt(portSplit[0]);
                // If we had match with WAN host, check for correlation with WAN port
                if (parts[0].equals(pubWanHost)) {
                    if (refPort.equals(pubWanPort)) {
                        isLegal = false;
                    }
                } else {
                    // If we had match with regular illegals, check regular port correlation
                    if (refPort.equals(WSNotificationServer.getInstance().getPort())) {
                        isLegal = false;
                    }
                }
            }
        }
        // If we found an illefal combination, throw the exception
        if (!isLegal)
            ExceptionUtilities.throwSubscribeCreationFailedFault("en", "Invalid consumer reference. Would cause local loopback on the broker.");
    }

    /**
     * Check if a subscription / registration -key exists
     *
     * @param s The key to check existance for
     * @return True if the key exists, false otherwise
     */
    @Override
    @WebMethod(exclude = true)
    public boolean keyExists(String s) {
        return _subscriptionManager.keyExists(s) || _registrationManager.keyExists(s);
    }

    /**
     * Fetch the collection of recipient subscriptionKeys
     *
     * @return A collection containing the subscriptionKeys as strings
     */
    @Override
    @WebMethod(exclude = true)
    protected Collection<String> getAllRecipients() {
        return _subscriptionManager.getAllRecipients();
    }

    /**
     * Retrieves the endpointReference of a subscriber from its subscription key
     *
     * @param subscriptionKey The subscription key representing the subscriber
     * @return A string containing the endpointReference of the subscriber
     */
    @Override
    protected String getEndpointReferenceOfRecipient(String subscriptionKey) {
        return this._subscriptionManager.getSubscriptionHandle(subscriptionKey).endpointTerminationTuple.endpoint;
    }

    /**
     * Override of the superclass method, this is to ensure that we reference the correct manager endpoint,
     * as WS-Nu only references the SUBSCRIPTION manager, not the publisherRegistrationManager
     *
     * @param prefix The prefix-token to be used as URL param KEY
     * @param key    The SubscriptionKey or PublisherRegistrationKey used as URL param VALUE
     * @return A concatenated full URL of the appropriate endpoint, param key and param value
     */
    @Override
    @WebMethod(exclude = true)
    public String generateHashedURLFromKey(String prefix, String key) {
        String endpointReference = "";
        // Check the prefix, and add the appropriate endpoint
        if (prefix.equals(_subscriptionManager.WSN_SUBSCRIBER_TOKEN)) {
            endpointReference = _subscriptionManager.getEndpointReference();
        } else if (prefix.equals(_registrationManager.WSN_PUBLISHER_TOKEN)) {
            endpointReference = _registrationManager.getEndpointReference();
        }
        // Return the endpointReference with the appended prefix and associated subscription/registration key
        return endpointReference + "/?" + prefix + "=" + key;
    }

    /**
     * Filters the recipients eligible for a notify
     *
     * @param s                          The subscriptionKey of the subscriber
     * @param notify                     The Notify object to be checked
     * @param nuNamespaceContextResolver An instance of NuNameSpaceContextResolver
     * @return The Notify object if it passed validation, false otherwise
     */
    @Override
    @WebMethod(exclude = true)
    protected Notify getRecipientFilteredNotify(String s, Notify notify, NuNamespaceContextResolver nuNamespaceContextResolver) {
        // Check if we have the current recipient registered
        if (!this._subscriptionManager.hasSubscription(s)) {
            return null;
        } else {
            // Check if the subscription is paused
            if (_subscriptionManager.subscriptionIsPaused(s)) return null;
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
        CoreService.getInstance().execute(() -> hub.acceptLocalMessage(outMessage));
    }

    /**
     * Sends a Notification message
     *
     * @param notify                   The Notify object containing the message(s)
     * @param namespaceContextResolver An instance of NuNameSpaceContextResolver
     */
    @Override
    public void sendNotification(Notify notify, NuNamespaceContextResolver namespaceContextResolver) {
        // If this somehow is called without WSNRequestParser set as hub, terminate
        if (hub == null) {
            log.error("Tried to send message with hub null. If a quickBuild is available," +
                    " consider running this before sending messages");
            return;
        }

        // Store the MessageService and CoreService instances
        MessageService messageService = MessageService.getInstance();
        TopicService topicService = TopicService.getInstance();
        // Declare the message object
        Message message;

        for (NotificationMessageHolderType messageHolderType : notify.getNotificationMessage()) {
            TopicExpressionType topic = messageHolderType.getTopic();

            // If it is connected to a topic, remember it
            if (topic != null) {
                try {

                    List<QName> topicQNames = TopicValidator.evaluateTopicExpressionToQName(topic, namespaceContextResolver.resolveNamespaceContext(topic));
                    String topicName = TopicUtils.topicToString(topicQNames);
                    topicName = WSNTools.removeNameSpacePrefixesFromTopicExpression(topicName);

                    log.debug("Message topic extracted: " + topicName);

                    // If the topic exists in the OKSE TopicService
                    if (topicService.topicExists(topicName)) {
                        log.debug("Topic existed, generating OKSE Message for distribution");
                        // Extract the content
                        String content = WSNTools.extractRawXmlContentFromDomNode((ElementNSImpl) messageHolderType.getMessage().getAny());
                        log.debug("Messace object: " + messageHolderType.getMessage().toString());
                        log.debug("Message content: " + content);

                        // Generate the message
                        message = new Message(content, topicName, null, WSNotificationServer.getInstance().getProtocolServerType());
                        log.debug("OKSE Message generated");
                        // Extract the endpoint reference from publisher
                        W3CEndpointReference publisherReference = messageHolderType.getProducerReference();
                        // If we have a publisherReference, add it to the message
                        if (publisherReference != null) {
                            log.debug("We had a publisher-reference, updating OKSE Message");
                            message.setAttribute(WSNSubscriptionManager.WSN_ENDPOINT_TOKEN, ServiceUtilities.getAddress(publisherReference));
                        }

                        // Add the message to the message queue for dispatch
                        messageService.distributeMessage(message);
                    }

                } catch (InvalidTopicExpressionFault invalidTopicExpressionFault) {
                    log.warn("Tried to send a topic with an invalid expression");
                } catch (MultipleTopicsSpecifiedFault multipleTopicsSpecifiedFault) {
                    log.warn("Tried to send a message with multiple topics");
                } catch (TopicExpressionDialectUnknownFault topicExpressionDialectUnknownFault) {
                    log.warn("Tried to send a topic with an invalid expression dialect");
                }
            }
        }

        /* Start Message Parsing */
        log.debug("Start message parsing and namespace binding");
        // bind namespaces to topics
        for (NotificationMessageHolderType holderType : notify.getNotificationMessage()) {

            TopicExpressionType topic = holderType.getTopic();

            if (holderType.getTopic() != null) {
                NuNamespaceContextResolver.NuResolvedNamespaceContext context = namespaceContextResolver.resolveNamespaceContext(topic);

                if (context == null) {
                    continue;
                }
            }
        }
        log.debug("Processing valid recipients...");

        // Update statistics
        WSNotificationServer.getInstance().incrementTotalMessagesReceived();

        // Remember current message with context
        currentMessage = notify;
        currentMessageNamespaceContextResolver = namespaceContextResolver;

        // For all valid recipients
        for (String recipient : this.getAllRecipients()) {

            // If the subscription has expired, continue
            if (_subscriptionManager.getSubscriber(recipient).hasExpired()) continue;

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

                // If the recipient has requested UseRaw, remove Notify payload wrapping
                if (_subscriptionManager
                        .getSubscriber(recipient)
                        .getAttribute(WSNSubscriptionManager.WSN_USERAW_TOKEN) != null) {

                    // For all bundled messages, extract and push
                    for (NotificationMessageHolderType holderType : toSend.getNotificationMessage()) {
                        // Extract the content
                        Object content = WSNTools.extractMessageContentFromNotify(toSend);
                        // Update the InternalMessage with the content of the NotificationMessage
                        outMessage.setMessage(content);
                        // Pass it to the requestparser
                        CoreService.getInstance().execute(() -> hub.acceptLocalMessage(outMessage));
                    }
                } else {
                    // Pass it along to the requestparser
                    CoreService.getInstance().execute(() -> hub.acceptLocalMessage(outMessage));
                }
            }
        }
        log.debug("Finished sending message to valid WS-Notification recipients");
    }

    /**
     * Implementation of the NotificationBroker's notify. This method does nothing but forward the notify by calling
     * {@link #sendNotification(org.oasis_open.docs.wsn.b_2.Notify)}
     *
     * @param notify The Notify object.
     */
    @Override
    @Oneway
    @WebMethod(operationName = "Notify")
    public void notify(@WebParam(partName = "Notify", name = "Notify", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
                       Notify notify) {
        this.sendNotification(notify, connection.getRequestInformation().getNamespaceContextResolver());
    }

    /**
     * The Subscribe request message as defined by the WS-N specification.
     * <p>
     * More information can be found at http://docs.oasis-open.org/wsn/wsn-ws_base_notification-1.3-spec-os.htm#_Toc133735624
     *
     * @param subscribeRequest A {@link org.oasis_open.docs.wsn.b_2.Subscribe} object.
     * @return A {@link org.oasis_open.docs.wsn.b_2.SubscribeResponse} if the subscription was added successfully.
     * @throws NotifyMessageNotSupportedFault           Never.
     * @throws UnrecognizedPolicyRequestFault           Never, policies will not be added until 2.0.
     * @throws TopicExpressionDialectUnknownFault       If the topic expression was not valid.
     * @throws ResourceUnknownFault                     Never, WS-Resources is not added as of 0.3
     * @throws InvalidTopicExpressionFault              If any topic expression added was invalid.
     * @throws UnsupportedPolicyRequestFault            Never, policies will not be added until 2.0
     * @throws InvalidFilterFault                       If the filter was invalid.
     * @throws InvalidProducerPropertiesExpressionFault Never.
     * @throws UnacceptableInitialTerminationTimeFault  If the subscription termination time was invalid.
     * @throws SubscribeCreationFailedFault             If any internal or general fault occured during the processing of a subscription request.
     * @throws TopicNotSupportedFault                   If the topic in some way is unknown or unsupported.
     * @throws InvalidMessageContentExpressionFault     Never.
     */
    @Override
    @WebMethod(operationName = "Subscribe")
    public SubscribeResponse subscribe(@WebParam(partName = "SubscribeRequest", name = "Subscribe",
            targetNamespace = "http://docs.oasis-open.org/wsn/b-2") Subscribe subscribeRequest) throws NotifyMessageNotSupportedFault, UnrecognizedPolicyRequestFault, TopicExpressionDialectUnknownFault, ResourceUnknownFault, InvalidTopicExpressionFault, UnsupportedPolicyRequestFault, InvalidFilterFault, InvalidProducerPropertiesExpressionFault, UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault, TopicNotSupportedFault, InvalidMessageContentExpressionFault {

        W3CEndpointReference consumerEndpoint = subscribeRequest.getConsumerReference();
        boolean useRaw = false;

        if (consumerEndpoint == null) {
            ExceptionUtilities.throwSubscribeCreationFailedFault("en", "Missing endpointreference");
        }

        String endpointReference = ServiceUtilities.getAddress(consumerEndpoint);

        // Loopback check
        throwFaultIfConsumerRefWouldCauseLoopback(endpointReference);

        // EndpointReference is returned as "" from getAddress if something went wrong.
        if (endpointReference.equals("")) {
            ExceptionUtilities.throwSubscribeCreationFailedFault("en", "EndpointReference malformatted or missing.");
        }

        // Check if the subscriber has requested non-Notify wrapped notifications
        if (subscribeRequest.getSubscriptionPolicy() != null) {
            for (Object o : subscribeRequest.getSubscriptionPolicy().getAny()) {
                if (o.getClass().equals(UseRaw.class)) {
                    log.debug("Subscriber requested raw message format");
                    useRaw = true;
                }
            }
        }

        log.debug("Endpointreference is: " + endpointReference);

        String requestAddress = "";
        Integer port = 80;
        String stripped = endpointReference.replace("http://", "").replace("https://", "");
        if (stripped.contains(":")) {
            String[] components = stripped.split(":");
            try {
                port = Integer.parseInt(components[components.length - 1]);
                requestAddress = components[components.length - 2];
            } catch (Exception e) {
                log.error("Failed to parse endpointReference");
            }
        } else {
            requestAddress = stripped;
        }

        FilterType filters = subscribeRequest.getFilter();
        Map<QName, Object> filtersPresent = null;

        // Initialize topicContent and requestDialect and contentFilters
        String rawTopicContent = null;
        String requestDialect = null;
        boolean topicExpressionIsXpath = false;
        ArrayList<String> contentFilters = new ArrayList<>();

        if (filters != null) {
            log.debug("Filters present. Attempting to iterate over filters...");
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

                        // Extract the QName
                        QName fName = filter.getName();

                        log.debug("Subscription request contained filter: " + fName + " Value: " + filter.getValue());
                        // If we have a TopicExpressionType as current
                        if (filter.getValue() instanceof org.oasis_open.docs.wsn.b_2.TopicExpressionType) {
                            // Cast to proper type
                            TopicExpressionType type = (TopicExpressionType) filter.getValue();
                            // Extract the actual value of the element
                            log.debug("Content: " + type.getContent().get(0));
                            // Set it as the raw topic content string
                            rawTopicContent = WSNTools.removeNameSpacePrefixesFromTopicExpression(TopicUtils.extractExpression(type));
                            // List potential attributes
                            log.debug("Attributes: " + type.getOtherAttributes());
                            // List and add the dialect of the expression type
                            log.debug("Dialect: " + type.getDialect());
                            requestDialect = type.getDialect();

                            // Check if dialect was XPATH, then we need to update the flag and add as filter
                            // Since we cannot guarantee a single topic resolvement
                            if (requestDialect.equalsIgnoreCase(WSNTools._XpathTopicExpression)) {
                                topicExpressionIsXpath = true;
                            }

                            // Do we have a MessageContent filter (XPATH)
                        } else if (filter.getValue() instanceof org.oasis_open.docs.wsn.b_2.QueryExpressionType) {
                            // Cast to proper type
                            QueryExpressionType type = (QueryExpressionType) filter.getValue();
                            // For each potential expression, add to the message content filter set
                            type.getContent().stream().forEach(p -> {
                                log.debug("Content: " + p.toString());
                                contentFilters.add(p.toString());
                            });
                            requestDialect = type.getDialect();
                            // What XPATH dialect (or potentially other non-supported) was provided
                            log.debug("Dialect: " + type.getDialect());
                        }

                        // Add the filter to the WS-Nu filtersPresent set
                        filtersPresent.put(fName, filter.getValue());
                    } else {
                        log.warn("Subscription attempt with non-supported filter: " + filter.getName());
                        ExceptionUtilities.throwInvalidFilterFault("en", "Filter not supported for this producer: " +
                                filter.getName(), filter.getName());
                    }

                }
            }
        }

        // Initialize initial termination time
        long terminationTime = 0;

        // If it was provided in the request
        if (subscribeRequest.getInitialTerminationTime() != null && !subscribeRequest.getInitialTerminationTime().isNil()) {
            try {
                terminationTime = WSNTools.interpretTerminationTime(subscribeRequest.getInitialTerminationTime().getValue());

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

        String subscriptionEndpoint = this.generateHashedURLFromKey(_subscriptionManager.WSN_SUBSCRIBER_TOKEN, newSubscriptionKey);

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

        if (rawTopicContent != null) {
            // If the expression is XPATH, we cannot resolve to a single topic, hence add as filter
            // And set topic reference to null
            if (topicExpressionIsXpath) {
                contentFilters.add(rawTopicContent);
                rawTopicContent = null;
            }
            // Check if the topic contains wildcards, dots or double separators
            else if (rawTopicContent.contains("*") || rawTopicContent.contains("//") ||
                    rawTopicContent.contains("//.") || rawTopicContent.contains("/.")) {
                log.debug("Topic expression contained XPATH or FullTopic wildcards or selectors, resetting topic and adding as filter");
                contentFilters.add(rawTopicContent);
                rawTopicContent = null;
            } else {
                log.debug("Sending addTopic request to TopicService");
                TopicService.getInstance().addTopic(rawTopicContent);
            }
        } else {
            log.debug("No topic was specified, setting to null and listening to all topics");
        }

        log.debug("Initializing OKSE subscriber object");
        // Instanciate new OKSE Subscriber object
        Subscriber subscriber = new Subscriber(requestAddress, port, rawTopicContent, WSNotificationServer.getInstance().getProtocolServerType());
        // Set the wsn-subscriber hash key in attributes
        subscriber.setAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN, newSubscriptionKey);
        subscriber.setAttribute(WSNSubscriptionManager.WSN_DIALECT_TOKEN, requestDialect);
        subscriber.setTimeout(terminationTime);
        // Add potential XPATH content filters discovered in the subscribe request
        contentFilters.forEach(filter -> subscriber.addFilter(filter));
        // Add useRaw flag if present
        if (useRaw) {
            subscriber.setAttribute(WSNSubscriptionManager.WSN_USERAW_TOKEN, "true");
            subscriber.addFilter("UseRaw");
        }

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
        log.debug("Publisher endpoint is: " + publisherEndpoint);

        // If we do not have an endpoint, produce a soapfault
        if (publisherEndpoint == null) {
            log.error("Missing endpoint reference in publisher registration request");
            ExceptionUtilities.throwPublisherRegistrationFailedFault("en", "Missing endpointreference");
        }

        // Endpointreference extracted from the W3CEndpointReference
        String endpointReference = ServiceUtilities.getAddress(registerPublisherRequest.getPublisherReference());

        // EndpointReference is returned as "" from getAddress if something went wrong.
        if (endpointReference.equals("")) {
            log.error("Failed to understand the endpoint reference");
            ExceptionUtilities.throwPublisherRegistrationFailedFault("en", "Could not register publisher, failed to " +
                    "understand the endpoint reference");
        }

        String requestAddress = "";
        Integer port = 80;
        String stripped = endpointReference.replace("http://", "").replace("https://", "");
        if (stripped.contains(":")) {
            String[] components = stripped.split(":");
            try {
                port = Integer.parseInt(components[components.length - 1]);
                requestAddress = components[components.length - 2];
            } catch (Exception e) {
                log.error("Failed to parse endpointReference");
            }
        } else {
            requestAddress = stripped;
        }

        List<TopicExpressionType> topics = registerPublisherRequest.getTopic();

        String rawTopicString = "";
        String rawDialect = "";

        // Validate Topic Expressions
        for (TopicExpressionType topic : topics) {
            try {
                if (!TopicValidator.isLegalExpression(topic, namespaceContextResolver.resolveNamespaceContext(topic))) {
                    log.error("Received an invalid topic expression");
                    ExceptionUtilities.throwTopicNotSupportedFault("en", "Expression given is not a legal topicexpression");
                } else {
                    rawTopicString = WSNTools.removeNameSpacePrefixesFromTopicExpression(TopicUtils.extractExpression(topic));
                    rawDialect = topic.getDialect();
                }
            } catch (TopicExpressionDialectUnknownFault topicExpressionDialectUnknownFault) {
                log.error("Received an unknown topic expression dialect");
                ExceptionUtilities.throwInvalidTopicExpressionFault("en", "TopicExpressionDialect unknown");
            }
        }

        // Fetch the termination time
        long terminationTime = registerPublisherRequest.getInitialTerminationTime().toGregorianCalendar().getTimeInMillis();

        // Validate the termination time
        if (terminationTime < System.currentTimeMillis()) {
            log.error("Caught an invalid termination time, must be in the future");
            ExceptionUtilities.throwUnacceptableInitialTerminationTimeFault("en", "Invalid termination time. Can't be before current time");
        }

        // Generate a new subkey
        String newPublisherKey = generateSubscriptionKey();
        // Generate the publisherRegistrationEndpoint
        String registrationEndpoint = generateHashedURLFromKey(WSNRegistrationManager.WSN_PUBLISHER_TOKEN, newPublisherKey);

        // Send subscriptionRequest back if isDemand isRequested
        if (registerPublisherRequest.isDemand()) {
            log.info("Demand registration is TRUE, sending subrequest back");
            WsnUtilities.sendSubscriptionRequest(endpointReference, getEndpointReference(), getHub());
        }

        // Create the necessary WS-Nu components needed for the RegisterPublisherResponse
        HelperClasses.EndpointTerminationTuple endpointTerminationTuple = new HelperClasses.EndpointTerminationTuple(newPublisherKey, terminationTime);
        PublisherHandle pubHandle = new PublisherHandle(endpointTerminationTuple, topics, registerPublisherRequest.isDemand());

        // Set up OKSE publisher object
        Publisher publisher = new Publisher(rawTopicString, requestAddress, port, WSNotificationServer.getInstance().getProtocolServerType());
        publisher.setTimeout(terminationTime);
        publisher.setAttribute(WSNRegistrationManager.WSN_PUBLISHER_TOKEN, newPublisherKey);
        publisher.setAttribute(WSNSubscriptionManager.WSN_DIALECT_TOKEN, rawDialect);

        // Create the topic
        TopicService.getInstance().addTopic(rawTopicString);

        // Register the publisher
        _registrationManager.addPublisher(publisher, pubHandle);

        // Initialize the response payload
        RegisterPublisherResponse response = new RegisterPublisherResponse();

        // Build the endpoint reference
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(registrationEndpoint);
        W3CEndpointReference publisherRegistrationReference = builder.build();
        builder.address(WSNotificationServer.getInstance().getURI());
        W3CEndpointReference consumerReference = builder.build();

        // Update the response with endpointreference
        response.setConsumerReference(consumerReference);
        response.setPublisherRegistrationReference(publisherRegistrationReference);

        return response;
    }

    @Override
    /**
     * Implementation of {@link org.oasis_open.docs.wsn.b_2.GetCurrentMessage}.
     *
     * This message will always fault unless {@link #cacheMessages} is true.
     *
     * @param getCurrentMessageRequest The request object
     * @return A {@link org.oasis_open.docs.wsn.b_2.GetCurrentMessageResponse} object with the latest message on the request topic.
     * @throws InvalidTopicExpressionFault Thrown either if the topic is invalid, or if no topic is given.
     * @throws TopicExpressionDialectUnknownFault Thrown if the topic expression uses a dialect not known
     * @throws MultipleTopicsSpecifiedFault Never thrown due to the nature of the {@link org.oasis_open.docs.wsn.b_2.GetCurrentMessage} object.
     * @throws ResourceUnknownFault Never thrown as of version 0.4, as WS-Resources is not implemented.
     * @throws NoCurrentMessageOnTopicFault If no message is listed on the current topic.
     * @throws TopicNotSupportedFault Never thrown as of version 0.3.
     */
    @WebResult(name = "GetCurrentMessageResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2",
            partName = "GetCurrentMessageResponse")
    @WebMethod(operationName = "GetCurrentMessage")
    public GetCurrentMessageResponse getCurrentMessage(GetCurrentMessage getCurrentMessageRequest) throws InvalidTopicExpressionFault, TopicExpressionDialectUnknownFault, MultipleTopicsSpecifiedFault, ResourceUnknownFault, NoCurrentMessageOnTopicFault, TopicNotSupportedFault {
        log.debug("getCurrentMessage called");

        if (!MessageService.getInstance().isCachingMessages()) {
            log.warn("Someone tried to get current message when caching is disabled");
            ExceptionUtilities.throwNoCurrentMessageOnTopicFault("en", "No messages are stored on Topic " +
                    getCurrentMessageRequest.getTopic().getContent());
        }

        log.debug("Accepted getCurrentMessage");
        // Find out which topic there was asked for (Exceptions automatically thrown)
        TopicExpressionType askedFor = getCurrentMessageRequest.getTopic();

        // Check if there was a specified topic element
        if (askedFor == null) {
            log.warn("Topic missing from getCurrentMessage request");
            ExceptionUtilities.throwInvalidTopicExpressionFault("en", "Topic missing from request.");
        }

        // Fetch the topic QNames
        List<QName> topicQNames = TopicValidator.evaluateTopicExpressionToQName(askedFor, connection.getRequestInformation().getNamespaceContext(askedFor));

        // Fetch the topic as a String
        String topicName = TopicUtils.topicToString(topicQNames);
        topicName = WSNTools.removeNameSpacePrefixesFromTopicExpression(topicName);

        // Fetch the latest message from the MessageService
        Message currentMessage = MessageService.getInstance().getLatestMessage(topicName);

        if (currentMessage == null) {
            log.warn("Was asked for current message on a topic that was not sent");
            ExceptionUtilities.throwNoCurrentMessageOnTopicFault("en", "There was no messages on the topic requested");

            return null;
        } else {
            // Initialize the response object
            GetCurrentMessageResponse response = new GetCurrentMessageResponse();

            WSNTools.NotifyWithContext notifywrapper = WSNTools.buildNotifyWithContext(currentMessage.getMessage(), currentMessage.getTopic(), null, null);
            // If it contained XML, we need to create properly marshalled jaxb node structure
            if (currentMessage.getMessage().contains("<") || currentMessage.getMessage().contains(">")) {
                // Unmarshal from raw XML
                Notify notify = WSNTools.createNotify(currentMessage);
                // If it was malformed, or maybe just a message containing < or >, build it as generic content element
                if (notify == null) {
                    WSNTools.injectMessageContentIntoNotify(WSNTools.buildGenericContentElement(currentMessage.getMessage()), notifywrapper.notify);
                    // Else inject the unmarshalled XML nodes into the Notify message attribute
                } else {
                    WSNTools.injectMessageContentIntoNotify(WSNTools.extractMessageContentFromNotify(notify), notifywrapper.notify);
                }
            }

            // Generate the NotificationMessage
            log.debug("Generated Notify wrapper");

            // Create a unmarshalled and linked Notify and extract the Message content from it
            Object messageObject = WSNTools.extractMessageContentFromNotify(notifywrapper.notify);
            response.getAny().add(messageObject);

            // Return the response
            return response;
        }
    }

    /* Begin obeservation methods */

    @Override
    public void publisherChanged(PublisherRegistrationEvent publisherRegistrationEvent) {
        log.debug("PublisherChanged event triggered");
    }

    @Override
    public void subscriptionChanged(SubscriptionEvent subscriptionEvent) {
        log.debug("SubscriptionChanged event triggered");
    }

    /* End observation methods */
}
