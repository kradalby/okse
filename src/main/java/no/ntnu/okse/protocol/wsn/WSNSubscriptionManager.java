package no.ntnu.okse.protocol.wsn;

import org.ntnunotif.wsnu.base.internal.ServiceConnection;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.Log;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.apache.log4j.Logger;

import javax.jws.WebMethod;
import java.util.HashMap;


/**
 * Created by Trond Walleraunet on 26.03.2015.
 */
public class WSNSubscriptionManager implements ServiceConnection{

    private static Logger log;
    private final HashMap<String, Long> _subscriptions;

    public WSNSubscriptionManager() {
        _subscriptions = new HashMap<>();
    }


    @Override
    public InternalMessage acceptMessage(InternalMessage message) {
        //InternalMessage rm = new InternalMessage(InternalMessage.STATUS_OK, message);
        addSubscriber("/subscribe", 1000); //Må sjekke litt i SimpleSubscriptionManager
        return message;
        }

    @WebMethod(exclude = true)
    public void addSubscriber(String subscriptionReference, long subscriptionEnd) {
        Log.d("SimpleSubscriptionmanager", "Adding subscription: " + subscriptionReference);
        _subscriptions.put(subscriptionReference, subscriptionEnd);
    }

    @Override
    public InternalMessage acceptRequest(InternalMessage internalMessage) {
        return null;
    }

    @Override
    public Class getServiceType() {
        return null;
    }

    @Override
    public String getServiceEndpoint() {
        return null;
    }

    @Override
    public RequestInformation getRequestInformation() {
        return null;
    }

    @Override
    public void endpointUpdated(String s) {

    }

    @Override
    public Object getWebService() {
        return null;
    }
}
