package no.ntnu.okse.protocol.wsn;

import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.ntnunotif.wsnu.services.general.ExceptionUtilities;
import org.ntnunotif.wsnu.services.implementations.notificationproducer.AbstractNotificationProducer;
import org.ntnunotif.wsnu.services.implementations.subscriptionmanager.AbstractSubscriptionManager;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.bw_2.PauseFailedFault;
import org.oasis_open.docs.wsn.bw_2.ResumeFailedFault;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by Aleksander Skraastad (myth) on 26.03.2015.
 */
@WebService(targetNamespace = "http://docs.oasis-open.org/wsn/bw-2", name = "PausableSubscriptionManager")
@XmlSeeAlso({org.oasis_open.docs.wsn.t_1.ObjectFactory.class, org.oasis_open.docs.wsn.br_2.ObjectFactory.class, org.oasis_open.docs.wsrf.r_2.ObjectFactory.class, org.oasis_open.docs.wsrf.bf_2.ObjectFactory.class, org.oasis_open.docs.wsn.b_2.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class WSNSubscriptionManager extends AbstractSubscriptionManager implements SubscriptionChangeListener {

    public static final String WSN_SUBSCRIBER_TOKEN = "wsn-subscriberkey";
    public static final String WSN_DIALECT_TOKEN = "wsn-dialect";
    public static final String WSN_ENDPOINT_TOKEN = "wsn-endpoint";
    public static final String WSN_USERAW_TOKEN = "wsn-useraw";

    private static Logger log;
    private SubscriptionService _subscriptionService = null;
    private ConcurrentHashMap<String, Subscriber> localSubscriberMap;
    private ConcurrentHashMap<String, AbstractNotificationProducer.SubscriptionHandle> localSubscriberHandle;

    /**
     * Empty constructor that initializes the log and local maps
     */
    public WSNSubscriptionManager() {
        log = Logger.getLogger(WSNSubscriptionManager.class.getName());
        localSubscriberMap = new ConcurrentHashMap<>();
        localSubscriberHandle = new ConcurrentHashMap<>();
    }

    /* Helper methods */

    /**
     * This method is called from the CommandProxy to set a reference to our SubscriptionService
     *
     * @param subService
     */
    public void initCoreSubscriptionService(SubscriptionService subService) {
        this._subscriptionService = subService;
    }

    /**
     * Check to see if a subscriptionKey exists
     *
     * @param s The key to check if exists
     * @return True if the key exists, false otherwise
     */
    @Override
    @WebMethod(exclude = true)
    public boolean keyExists(String s) {
        return localSubscriberMap.containsKey(s);
    }

    /**
     * Check to see if a subscriber exists
     *
     * @param s The subscriptionKey to check if exists
     * @return True if the subscriber exists, false otherwise
     */
    @Override
    @WebMethod(exclude = true)
    public boolean hasSubscription(String s) {
        return localSubscriberMap.containsKey(s);
    }

    /**
     * Check to see if a subscription is paused or not
     *
     * @param subscriptionReference The subscriptionKey to be checked
     * @return True if the subscriber is paused, false otherwise
     */
    @WebMethod(exclude = true)
    public boolean subscriptionIsPaused(String subscriptionReference) {
        return localSubscriberMap.containsKey(subscriptionReference) &&
                localSubscriberMap.get(subscriptionReference).getAttribute("paused") != null &&
                localSubscriberMap.get(subscriptionReference).getAttribute("paused").equals("true");
    }

    /**
     * This is the main OKSE implementation of the subscription manager addSubscriber method that should be used.
     * It will delegate core subscriber registry to the core SubscriptionService, as well as update the local
     * mappings from WS-Nu subscriptionKey to the relevant Subscriber and SubscriptionHandle objects.
     *
     * @param s         An instance of OKSE Subscriber with proper fields and attributes set.
     * @param subHandle An instance of WS-Nu SubscriptionHandle with proper fields and attributes set.
     */
    public void addSubscriber(Subscriber s, AbstractNotificationProducer.SubscriptionHandle subHandle) {
        _subscriptionService.addSubscriber(s);
        log.debug("Adding Subscriber to local mappings: " + s.getAttribute(WSN_SUBSCRIBER_TOKEN));
        localSubscriberMap.put(s.getAttribute(WSN_SUBSCRIBER_TOKEN), s);
        localSubscriberHandle.put(s.getAttribute(WSN_SUBSCRIBER_TOKEN), subHandle);
    }

    // This should not be called in any of the OKSE Custom/Proxy web service implementations.
    // Use addSubscriber(Subscriber s, SubscriptionHandle subHandle) instead.
    @Override
    public void addSubscriber(String s, long l) {
        log.warn("WS-Nu default addSubscriber with hashKey and terminationTime called. " +
                "Locate offending method and change to addSubscriber(Subscriber s).");
    }

    /**
     * Removes a Subscriber from the SubscriptionService, and the listener callback will remove it from
     * local mappings.
     *
     * @param s The WS-Nu subscriptionkey
     */
    @Override
    public void removeSubscriber(String s) {
        if (hasSubscription(s)) {
            _subscriptionService.removeSubscriber(localSubscriberMap.get(s));
        }
    }

    /**
     * Retrieve a collection of all the WS-Nu subscriptionKeys registered to this manager
     *
     * @return A Collection of WS-Nu subscriptionKeys
     */
    public Collection<String> getAllRecipients() {
        return localSubscriberMap.keySet();
    }

    /**
     * Retrieve the SubscriptionHandle of the subscriber identified by the argument subscriptionKey.
     *
     * @param s The subscriptionKey of the Subscriber to fetch the SubscriptionHandle of
     * @return A SubscriptionHandle connected to the Subscriber if it exists, null otherwise
     */
    public AbstractNotificationProducer.SubscriptionHandle getSubscriptionHandle(String s) {
        if (localSubscriberHandle.containsKey(s)) return localSubscriberHandle.get(s);
        return null;
    }

    /**
     * Retrieve the SubscriptionHandle of the subscriber provided as the argument
     * <p/>
     * This method attempts to extract the WS-Nu subscriptionKey from the Subscriber object's
     * attribute set, and delegates the rest to the String based method with the same name.
     *
     * @param subscriber The Subscriber object to fetch the SubscriptionHandle of
     * @return A SubscriptionHandle connected to the Subscriber if it exists, null otherwise
     */
    public AbstractNotificationProducer.SubscriptionHandle getSubscriptionHandle(Subscriber subscriber) {
        return getSubscriptionHandle(subscriber.getAttribute(WSN_SUBSCRIBER_TOKEN));
    }

    /**
     * Retrieve a OKSE Subscriber object based on the WS-Nu subscriptionReference
     *
     * @param subscriptionReference The subscriptionReference to fetch related OKSE Subscriber object from
     * @return An OKSE Subscriber object if found, <code>null</code> otherwise
     */
    public Subscriber getSubscriber(String subscriptionReference) {
        if (localSubscriberMap.containsKey(subscriptionReference)) return localSubscriberMap.get(subscriptionReference);
        return null;
    }

    /**
     * This generic update method is intended to purge Subscribers that have a terminationTime set and it has expired
     */
    @Override
    public void update() {

    }

    /* BEGIN PUBLIC WEB METHODS */

    /**
     * SimpleSubscriptionManagers implementation of unsubscribe.
     *
     * @param unsubscribeRequest The incoming unsubscribeRequest parsed from XML
     * @return A proper UbsubscribeResponse XML Object
     * @throws ResourceUnknownFault             If the subscription reference did not exist
     * @throws UnableToDestroySubscriptionFault If there was an other error of some sort
     */
    @Override
    @WebResult(name = "UnsubscribeResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "UnsubscribeResponse")
    @WebMethod(operationName = "Unsubscribe")
    public UnsubscribeResponse unsubscribe
    (
            @WebParam(partName = "UnsubscribeRequest", name = "Unsubscribe", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
            Unsubscribe unsubscribeRequest
    ) throws ResourceUnknownFault, UnableToDestroySubscriptionFault {

        log.debug("Received unsubscribe request");
        RequestInformation requestInformation = connection.getRequestInformation();

        for (Map.Entry<String, String[]> entry : requestInformation.getParameters().entrySet()) {
            // If the param is not the subscription reference, continue
            if (!entry.getKey().equals(WSN_SUBSCRIBER_TOKEN)) {
                continue;
            }

            /* If there is not one value, something is wrong, but try the first one */
            if (entry.getValue().length > 1) {
                log.debug("Found more than one param value, iterating...");
                String subRef = entry.getValue()[0];
                // Check if we have the subscriptionReference in our local map
                if (localSubscriberMap.containsKey(subRef)) {
                    // If we do, remove the Subscriber object in the local map from the subscription service
                    // The final removal of the subscriptionKey from the local map will happen in the
                    // SubscriptionChangeListener callback.
                    log.debug("Found subscriptionReference in local map, passing Subscriber object to SubscriptionService");
                    _subscriptionService.removeSubscriber(localSubscriberMap.get(subRef));
                    return new UnsubscribeResponse();
                } else {
                    ExceptionUtilities.throwResourceUnknownFault("en", "Ill-formated subscription-parameter");
                }
            } else if (entry.getValue().length == 0) {
                ExceptionUtilities.throwUnableToDestroySubscriptionFault("en", "Subscription-parameter in URL is missing value");
            }

            // Extract the subscriptionReference and store it
            String subRef = entry.getValue()[0];

            /* The subscriptions is not recognized */
            if (!localSubscriberMap.containsKey(subRef)) {
                log.debug("Subscription not found");
                log.debug("Expected: " + subRef);
                ExceptionUtilities.throwResourceUnknownFault("en", "Subscription not found.");
            }

            log.debug("Found subscription reference in local map, passing Subscriber object to SubscriptionService");
            // Remove the subscriber from the subscription service, and await callback from listener support
            // to remove the localmap keys
            _subscriptionService.removeSubscriber(localSubscriberMap.get(subRef));
            // Return the response
            return new UnsubscribeResponse();
        }

        ExceptionUtilities.throwUnableToDestroySubscriptionFault("en", "The subscription was not found as any parameter" +
                " in the request-uri. Please send a request on the form: " +
                "\"http://urlofthis.domain/webservice/?" + WSN_SUBSCRIBER_TOKEN + "=subscriptionreference");
        return null;
    }

    /**
     * SimpleSubscriptionManager's implementation of renew.
     *
     * @param renewRequest The incoming renewRequest parsed from XML
     * @return A RenewResponse XML object
     * @throws ResourceUnknownFault             If the subscriptionReference was not found, or missing
     * @throws UnacceptableTerminationTimeFault If the terminationTime was either unparseable or in the past
     */
    @Override
    @WebResult(name = "RenewResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "RenewResponse")
    @WebMethod(operationName = "Renew")
    public RenewResponse renew(
            @WebParam(partName = "RenewRequest", name = "Renew", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
            Renew renewRequest)
            throws ResourceUnknownFault, UnacceptableTerminationTimeFault {

        RequestInformation requestInformation = connection.getRequestInformation();

        log.debug("Received renew request");
        /* Find the subscription tag */
        for (Map.Entry<String, String[]> entry : requestInformation.getParameters().entrySet()) {
            log.debug("Current key processing: " + entry.getKey());
            if (!entry.getKey().equals(WSN_SUBSCRIBER_TOKEN)) {
                continue;
            }
            log.debug("Found subscription parameter");
            String subRef;

            /* There is not one value, something is wrong, but try the first one */
            if (entry.getValue().length >= 1) {
                subRef = entry.getValue()[0];

                if (!localSubscriberMap.containsKey(subRef)) {
                    log.debug("Attempt to renew a subscription that did not exist");
                    ExceptionUtilities.throwResourceUnknownFault("en", "Given resource was unknown: " + subRef);
                }
            /* We just continue down here as the time-fetching operations are rather large */
            } else if (entry.getValue().length == 0) {
                log.debug("Attempt to renew a blank subscription reference");
                ExceptionUtilities.throwResourceUnknownFault("en", "A blank resource is always unknown.");
            }

            // Extract and store the subscriptionReference
            subRef = entry.getValue()[0];

            log.debug("SubscriptionReference is: " + subRef);
            log.debug("Matched Subscriber object is: " + localSubscriberMap.get(subRef));

            // Parse the new termination time
            long time = WSNTools.interpretTerminationTime(renewRequest.getTerminationTime());

            // Verify new termination time
            if (time < System.currentTimeMillis()) {
                log.debug("Received a terminationTime in renew request that had already passed");
                ExceptionUtilities.throwUnacceptableTerminationTimeFault("en", "Tried to renew a subscription so it " +
                        "should last until a time that has already passed.");
            }

            // Send a renewSubscriber task to the SubscriptionService
            _subscriptionService.renewSubscriber(localSubscriberMap.get(subRef), time);

            // Create the response
            RenewResponse response = new RenewResponse();
            GregorianCalendar gc = new GregorianCalendar();
            GregorianCalendar cgc = new GregorianCalendar();
            Date terminationDate = new Date(time);
            Date currentDate = new Date();
            gc.setTime(terminationDate);
            cgc.setTime(currentDate);
            XMLGregorianCalendar terminationTime = null;
            XMLGregorianCalendar currentTime = null;

            try {
                terminationTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
                currentTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(cgc);
            } catch (DatatypeConfigurationException e) {
                log.error("Failed to generate the XMLGregorianCalendar instance");
            }
            // Add the XMLGregorianCalendar instances if they successfully were generated
            if (terminationTime != null) response.setTerminationTime(terminationTime);
            if (currentTime != null) response.setCurrentTime(currentTime);

            // Return the response
            return response;
        }

        log.debug("Subscription not found, probably ill-formatted request");
        ExceptionUtilities.throwResourceUnknownFault("en", "The subscription was not found as any parameter" +
                " in the request-uri. Please send a request on the form: " +
                "\"http://urlofthis.domain/webservice/?" + WSN_SUBSCRIBER_TOKEN + "=subscriptionreference");

        return null;
    }

    @WebResult(name = "ResumeSubscriptionResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "ResumeSubscriptionResponse")
    @WebMethod(operationName = "ResumeSubscription")
    public ResumeSubscriptionResponse resumeSubscription
            (
                    @WebParam(partName = "ResumeSubscriptionRequest", name = "ResumeSubscription", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
                    ResumeSubscription resumeSubscriptionRequest
            ) throws ResourceUnknownFault, ResumeFailedFault {

        log.debug("Received Resume request");

        // Fetch the request information
        RequestInformation requestInformation = connection.getRequestInformation();

        // Iterate through the parameters
        for (Map.Entry<String, String[]> entry : requestInformation.getParameters().entrySet()) {
            // If it is not the subscriptionReference, continue
            if (!entry.getKey().equals(WSN_SUBSCRIBER_TOKEN)) {
                continue;
            }

            /* If there is not one value, something is wrong, but try the first one*/
            if (entry.getValue().length > 1) {
                // Fetch the first value
                String subRef = entry.getValue()[0];

                if (!localSubscriberMap.containsKey(subRef)) {
                    if (subscriptionIsPaused(subRef)) {
                        log.debug("SubscriptionReference in resume request found, passing to SubscriptionService");
                        // Tell the SubscriptionService to resume the Subscriber
                        _subscriptionService.resumeSubscriber(localSubscriberMap.get(subRef));
                        // Return the response
                        return new ResumeSubscriptionResponse();
                    } else {
                        log.warn("Attempt to resume a subscriber that was not paused");
                        ExceptionUtilities.throwResumeFailedFault("en", "Subscription is not paused");
                    }
                }
                log.warn("Received a malformed subscription parameter during resume request");
                ExceptionUtilities.throwResourceUnknownFault("en", "Ill-formated subscription-parameter");
            } else if (entry.getValue().length == 0) {
                log.warn("Subscription-parameter in URL is missing value during resume request");
                ExceptionUtilities.throwResumeFailedFault("en", "Subscription-parameter in URL is missing value");
            }

            // Extract and store the subscriptionReference
            String subRef = entry.getValue()[0];

            /* The subscriptions is not recognized */
            if (!localSubscriberMap.containsKey(subRef)) {
                log.debug("ResumeRequest: Subscription not found");
                log.debug("ResumeRequest expected: " + subRef);
                ExceptionUtilities.throwResourceUnknownFault("en", "Subscription not found.");
            }

            // Tell the SubscriptionService to resume the subscriber
            _subscriptionService.resumeSubscriber(localSubscriberMap.get(subRef));

            // Return the response
            return new ResumeSubscriptionResponse();
        }
        ExceptionUtilities.throwResumeFailedFault("en", "The subscription was not found as any parameter" +
                " in the request-uri. Please send a request on the form: " +
                "\"http://urlofthis.domain/webservice/?" + WSN_SUBSCRIBER_TOKEN + "=subscriptionreference");
        return null;
    }

    @WebResult(name = "PauseSubscriptionResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "PauseSubscriptionResponse")
    @WebMethod(operationName = "PauseSubscription")
    public PauseSubscriptionResponse pauseSubscription
            (
                    @WebParam(partName = "PauseSubscriptionRequest", name = "PauseSubscription", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
                    PauseSubscription pauseSubscriptionRequest
            ) throws ResourceUnknownFault, PauseFailedFault {

        log.debug("Received Pause request");

        // Fetch the request information
        RequestInformation requestInformation = connection.getRequestInformation();

        for (Map.Entry<String, String[]> entry : requestInformation.getParameters().entrySet()) {
            // If the parameter is not the subscription token (we are not checking the value, just the key)
            if (!entry.getKey().equals(WSN_SUBSCRIBER_TOKEN)) {
                continue;
            }

            /* If there is not one value, something is wrong, but try the first one*/
            if (entry.getValue().length > 1) {
                String subRef = entry.getValue()[0];
                if (!localSubscriberMap.containsKey(subRef)) {
                    if (!subscriptionIsPaused(subRef)) {
                        // Tell the subscriptionService to pause the subscriber
                        _subscriptionService.pauseSubscriber(localSubscriberMap.get(subRef));
                        // Return the response
                        return new PauseSubscriptionResponse();
                    } else {
                        log.debug("Attempt to pause an already paused subscription");
                        ExceptionUtilities.throwPauseFailedFault("en", "Subscription is already paused");
                    }
                }
                log.debug("Received ill-formated subscription parameter in Pause request");
                ExceptionUtilities.throwResourceUnknownFault("en", "Ill-formated subscription-parameter");
            } else if (entry.getValue().length == 0) {
                log.debug("Subscription parameter missing in Pause request");
                ExceptionUtilities.throwPauseFailedFault("en", "Subscription-parameter in URL is missing value");
            }

            // Extract and store the subscriptionReference
            String subRef = entry.getValue()[0];

            /* The subscriptions is not recognized */
            if (!localSubscriberMap.containsKey(subRef)) {
                log.debug("Subscription not found");
                log.debug("Expected: " + subRef);
                ExceptionUtilities.throwResourceUnknownFault("en", "Subscription not found.");
            }

            // Tell the SubscriptionService to pause the subscriber
            _subscriptionService.pauseSubscriber(localSubscriberMap.get(subRef));

            // Return the response
            return new PauseSubscriptionResponse();
        }
        ExceptionUtilities.throwPauseFailedFault("en", "The subscription was not found as any parameter" +
                " in the request-uri. Please send a request on the form: " +
                "\"http://urlofthis.domain/webservice/?" + WSN_SUBSCRIBER_TOKEN + "=subscriptionreference");

        return null;
    }

    /* Listener support methods */

    // We catch subscriptionchange events to update local maps upon remove, renew etc
    // But not on subscribe, since we are the initiating source we can update the local maps first,
    // and only delegate the subscriber object to the core SubscriptionService.

    /**
     * Listener method that takes in a SubscriptionChangeEvent
     *
     * @param e The Event object with associated Subscriber object
     */
    @Override
    @WebMethod(exclude = true)
    public void subscriptionChanged(SubscriptionChangeEvent e) {
        // If it is WSNotification subscriber
        if (e.getData().getOriginProtocol().equals(WSNotificationServer.getInstance().getProtocolServerType())) {
            // If we are dealing with an Unsubscribe
            if (e.getType().equals(SubscriptionChangeEvent.Type.UNSUBSCRIBE)) {
                log.debug("Ubsubscribing " + localSubscriberHandle.get(e.getData().getAttribute(WSN_SUBSCRIBER_TOKEN)));
                // Remove the local mappings from WS-Nu subscriptionKey to OKSE Subscriber object and WS-Nu subscriptionHandle
                localSubscriberMap.remove(e.getData().getAttribute(WSN_SUBSCRIBER_TOKEN));
                localSubscriberHandle.remove(e.getData().getAttribute(WSN_SUBSCRIBER_TOKEN));

            } else if (e.getType().equals(SubscriptionChangeEvent.Type.SUBSCRIBE)) {
                log.debug("Received a SUBSCRIBE event");
            }
        }
    }
}
