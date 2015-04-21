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

import no.ntnu.okse.core.event.PublisherChangeEvent;
import no.ntnu.okse.core.event.listeners.PublisherChangeListener;
import no.ntnu.okse.core.subscription.Publisher;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;
import org.ntnunotif.wsnu.services.implementations.notificationproducer.AbstractNotificationProducer;
import org.ntnunotif.wsnu.services.implementations.publisherregistrationmanager.AbstractPublisherRegistrationManager;
import org.oasis_open.docs.wsn.br_2.DestroyRegistration;
import org.oasis_open.docs.wsn.br_2.DestroyRegistrationResponse;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationManager;
import org.oasis_open.docs.wsn.brw_2.ResourceNotDestroyedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebMethod;
import javax.jws.WebService;
import java.util.HashMap;

/**
 * Created by Aleksander Skraastad (myth) on 4/20/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@WebService
public class WSNRegistrationManager extends AbstractPublisherRegistrationManager implements PublisherChangeListener {

    public static final String WSN_PUBLISHER_TOKEN = "wsn-publisherkey";

    private static Logger log;
    private SubscriptionService _subscriptionService = null;
    private HashMap<String, Publisher> localPublisherMap;
    private HashMap<String, AbstractNotificationBroker.PublisherHandle> localPublisherHandle;

    public WSNRegistrationManager() {
        log = Logger.getLogger(WSNRegistrationManager.class.getName());
        _subscriptionService = null;
        localPublisherMap = new HashMap<>();
        localPublisherHandle = new HashMap<>();
    }

    public void initCoreSubscriptionService(SubscriptionService service) {
        _subscriptionService = service;
    }

    public void addPublisher(Publisher p, AbstractNotificationBroker.PublisherHandle pubHandle) {
        _subscriptionService.addPublisher(p);
        localPublisherMap.put(p.getAttribute(WSN_PUBLISHER_TOKEN), p);
        localPublisherHandle.put(p.getAttribute(WSN_PUBLISHER_TOKEN), pubHandle);
    }

    @Override
    public void addPublisher(String s, long l) {
        log.warn("WS-Nu default addPublisher with hashKey and terminationTime called. " +
                "Locate offending method and change to addSubscriber(Publisher p, PublisherHandle pubHandle)");
    }

    /**
     * This method uses the OKSE core subscription service to remove the subscriber,
     * and the awaits the callback from the publisherchangeevent to verify, and then remove from local maps
     * @param p
     */
    @Override
    public void removePublisher(String p) {
        if (hasPublisher(p)) {
            _subscriptionService.removePublisher(localPublisherMap.get(p));
        }
    }

    public boolean hasPublisher(String p) {
        return localPublisherMap.containsKey(p);
    }

    @Override
    public void update() {
        // This should maybe proxy the periodic update event removing expired subs
    }

    @Override
    public void publisherChanged(PublisherChangeEvent e) {
        // Is this event on WSN publisher?
        if (e.getData().getOriginProtocol().equals(WSNotificationServer.getInstance().getProtocolServerType())) {
            // Do we have an unregister?
            if (e.getType().equals(PublisherChangeEvent.Type.UNREGISTER)) {
                // Remove the publisher from maps using its WSN_PUBLISHER_TOKEN hash
                localPublisherMap.remove(e.getData().getAttribute(WSN_PUBLISHER_TOKEN));
                localPublisherHandle.remove(e.getData().getAttribute(WSN_PUBLISHER_TOKEN));
            }
        }
    }

    @Override
    public DestroyRegistrationResponse destroyRegistration(DestroyRegistration destroyRegistration) throws ResourceNotDestroyedFault, ResourceUnknownFault {
        return null;
    }
}
