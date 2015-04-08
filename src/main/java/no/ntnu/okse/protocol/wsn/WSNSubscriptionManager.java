package no.ntnu.okse.protocol.wsn;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.services.implementations.subscriptionmanager.AbstractSubscriptionManager;
import org.oasis_open.docs.wsn.b_2.Renew;
import org.oasis_open.docs.wsn.b_2.RenewResponse;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.b_2.UnsubscribeResponse;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebService;
import java.util.HashMap;


/**
 * Created by Trond Walleraunet on 26.03.2015.
 */
@WebService
public class WSNSubscriptionManager extends AbstractSubscriptionManager {

    private static Logger log;
    private SubscriptionService _subscriptionService;

    public WSNSubscriptionManager() {
        _subscriptionService = Application.cs.getSubscriptionService();
    }

    @Override
    public boolean keyExists(String s) {
        return false;
    }

    @Override
    public boolean hasSubscription(String s) {
        return false;
    }

    @Override
    public void addSubscriber(String s, long l) {

    }

    @Override
    public void removeSubscriber(String s) {

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
}
