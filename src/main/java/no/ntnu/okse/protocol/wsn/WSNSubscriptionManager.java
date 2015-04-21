package no.ntnu.okse.protocol.wsn;

import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.subscription.Publisher;
import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;
import org.ntnunotif.wsnu.services.implementations.notificationproducer.AbstractNotificationProducer;
import org.ntnunotif.wsnu.services.implementations.subscriptionmanager.AbstractSubscriptionManager;
import org.oasis_open.docs.wsn.b_2.Renew;
import org.oasis_open.docs.wsn.b_2.RenewResponse;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.b_2.UnsubscribeResponse;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebMethod;
import javax.jws.WebService;
import java.util.Collection;
import java.util.HashMap;


/**
 * Created by Trond Walleraunet on 26.03.2015.
 */
@WebService
public class WSNSubscriptionManager extends AbstractSubscriptionManager implements SubscriptionChangeListener {

    public static final String WSN_SUBSCRIBER_TOKEN = "wsn-subscriberkey";
    public static final String WSN_DIALECT_TOKEN = "wsn-dialect";
    public static final String WSN_ENDPOINT_TOKEN = "wsn-endpoint";

    private static Logger log;
    private SubscriptionService _subscriptionService = null;
    private HashMap<String, Subscriber> localSubscriberMap;
    private HashMap<String, AbstractNotificationProducer.SubscriptionHandle> localSubscriberHandle;

    public WSNSubscriptionManager() {
        log = Logger.getLogger(WSNSubscriptionManager.class.getName());
        localSubscriberMap = new HashMap<>();
        localSubscriberHandle = new HashMap<>();
    }

    public void initCoreSubscriptionService(SubscriptionService subService) {
        this._subscriptionService = subService;
    }

    @Override
    public boolean keyExists(String s) {
        return localSubscriberMap.containsKey(s);
    }

    @Override
    public boolean hasSubscription(String s) {
        return localSubscriberMap.containsKey(s);
    }

    /**
     * This is the main OKSE implementation of the subscription manager addSubscriber method that should be used.
     * It will delegate core subscriber registry to the core SubscriptionService, as well as update the local
     * mappings from WS-Nu subscriptionKey to the relevant Subscriber and SubscriptionHandle objects.
     * @param s An instance of OKSE Subscriber with proper fields and attributes set.
     * @param subHandle An instance of WS-Nu SubscriptionHandle with proper fields and attributes set.
     */
    public void addSubscriber(Subscriber s, AbstractNotificationProducer.SubscriptionHandle subHandle) {
        _subscriptionService.addSubscriber(s);
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

    @Override
    public void removeSubscriber(String s) {
        if (hasSubscription(s)) {
            _subscriptionService.removeSubscriber(localSubscriberMap.get(s));
        }
    }

    /**
     * Retrieve a collection of all the WS-Nu subscriptionKeys registered to this manager
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
     *
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
     * This generic update method is intended to purge Subscribers that have a terminationTime set and it has expired
     */
    @Override
    public void update() {

    }

    @Override
    public UnsubscribeResponse unsubscribe(Unsubscribe unsubscribe) throws ResourceUnknownFault, UnableToDestroySubscriptionFault {
        log.debug("UNSUB CALLED");
        return null;
    }

    @Override
    public RenewResponse renew(Renew renew) throws ResourceUnknownFault, UnacceptableTerminationTimeFault {
        log.debug("RENEW CALLED");
        return null;
    }

    // We catch subscriptionchange events to update local maps upon remove, renew etc
    // But not on subscribe, since we are the initiating source we can update the local maps first,
    // and only delegate the subscriber object to the core SubscriptionService.
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
                log.debug("Recieved a SUBSCRIBE event");
                // TODO: Investigate if we really need to do anything here since it will function as a callback
                // TODO: after addSubscriber
            }
        }
    }
}
