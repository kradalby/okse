package no.ntnu.okse.protocol.wsn;

import no.ntnu.okse.Application;
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
import org.oasis_open.docs.wsn.bw_2.SubscriptionManager;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


/**
 * Created by Trond Walleraunet on 26.03.2015.
 */
@WebService
public class WSNSubscriptionManager extends AbstractSubscriptionManager implements SubscriptionChangeListener {

    public static final String WSN_SUBSCRIBER_TOKEN = "wsn-subscriberkey";
    public static final String WSN_PUBLISHER_TOKEN = "wsn-publisherkey";

    private static Logger log;
    private SubscriptionService _subscriptionService;
    private HashMap<String, Subscriber> localSubscriberMap;
    private HashMap<String, Publisher> localPublisherMap;
    private HashMap<String, AbstractNotificationProducer.SubscriptionHandle> localSubscriberHandle;
    private HashMap<String, AbstractNotificationBroker.PublisherHandle> localPublisherHandle;

    public WSNSubscriptionManager() {
        log = Logger.getLogger(WSNSubscriptionManager.class.getName());
        _subscriptionService = Application.cs.getSubscriptionService();
        localSubscriberMap = new HashMap<>();
        localPublisherMap = new HashMap<>();
        localSubscriberHandle = new HashMap<>();
        localPublisherHandle = new HashMap<>();
    }

    @Override
    public boolean keyExists(String s) {
        return localPublisherMap.containsKey(s) || localSubscriberMap.containsKey(s);
    }

    @Override
    public boolean hasSubscription(String s) {
        return localSubscriberMap.containsKey(s);
    }

    @Override
    public void addSubscriber(String s, long l) {

    }

    @Override
    public void removeSubscriber(String s) {
        if (hasSubscription(s)) {
            _subscriptionService.removeSubscriber(localSubscriberMap.get(s));
        }
    }

    public Collection<String> getAllRecipients() {
        return localSubscriberMap.keySet();
    }

    public AbstractNotificationProducer.SubscriptionHandle getSubscriptionHandle(String s) {
        return localSubscriberHandle.get(s);
    }

    public AbstractNotificationBroker.PublisherHandle getPublisherHandle(String s) {
        return localPublisherHandle.get(s);
    }

    @Override
    public void update() {

    }

    @Override
    public UnsubscribeResponse unsubscribe(Unsubscribe unsubscribe) throws ResourceUnknownFault, UnableToDestroySubscriptionFault {
        return null;
    }

    @Override
    public RenewResponse renew(Renew renew) throws ResourceUnknownFault, UnacceptableTerminationTimeFault {
        return null;
    }

    @Override
    public void subscriptionChanged(SubscriptionChangeEvent e) {
        if (e.getType().equals(SubscriptionChangeEvent.Type.UNSUBSCRIBE)) {
            localSubscriberMap.remove(e.getData().getAttribute(WSN_SUBSCRIBER_TOKEN));
            localSubscriberHandle.remove(e.getData().getAttribute(WSN_SUBSCRIBER_TOKEN));
        } else if (e.getType().equals(SubscriptionChangeEvent.Type.SUBSCRIBE)) {
            // TODO: Call generateHash method and create a wsn local subscription mapping with proper
            // handles and wrapper classes
        }
    }
}
