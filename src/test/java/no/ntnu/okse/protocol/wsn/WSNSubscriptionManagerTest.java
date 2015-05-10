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

import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.subscription.Publisher;
import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.ntnunotif.wsnu.base.internal.ServiceConnection;
import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.ntnunotif.wsnu.services.filterhandling.FilterSupport;
import org.ntnunotif.wsnu.services.general.HelperClasses;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;
import org.ntnunotif.wsnu.services.implementations.notificationproducer.AbstractNotificationProducer;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.testng.Assert.*;

public class WSNSubscriptionManagerTest {

    WSNSubscriptionManager sm;
    TestSubscriptionService tss;
    Subscriber s;
    AbstractNotificationProducer.SubscriptionHandle sh;
    HelperClasses.EndpointTerminationTuple ett;
    FilterSupport.SubscriptionInfo si;

    @BeforeMethod
    public void setUp() throws Exception {
        // Set up sub manager
        sm = new WSNSubscriptionManager();
        // Set up dummy subscription service
        tss = new TestSubscriptionService();
        // Initialize sub manager with sub service
        sm.initCoreSubscriptionService(tss);
        // Add listener support
        tss.addSubscriptionChangeListener(sm);
        // Create a subscriber
        s = new Subscriber("0.0.0.0", 8080, "test", "WSNotification");
        // Set its subscription reference
        s.setAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN, "1234567890abcdef");
        // Add WS-Nu endpoint termination tuple
        ett = new HelperClasses.EndpointTerminationTuple("test", 10L);
        // Add WS-Nu SubscriptionInfo
        si = new FilterSupport.SubscriptionInfo(null, new NuNamespaceContextResolver());
        // Add WS-Nu SubscriptionHandle
        sh = new AbstractNotificationBroker.SubscriptionHandle(ett, si);
        // Create dummy request URL params
        Map<String, String[]> params = new HashMap<>();
        // Insert ref of our subscriber
        params.put(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN, new String[]{"1234567890abcdef"});
        // Create and set a dummy service connection
        sm.setConnection(new TestServiceConnection());
        // Set our dummy request params
        sm.getConnection().getRequestInformation().setParameters(params);
        // Add the subscriber to the subscription service
        sm.addSubscriber(s, sh);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        s = null;
    }

    @Test
    public void testKeyExists() throws Exception {
        assertTrue(sm.keyExists("1234567890abcdef"));
        assertFalse(sm.keyExists("0987654321"));
    }

    @Test
    public void testHasSubscription() throws Exception {
        assertTrue(sm.keyExists("1234567890abcdef"));
        assertFalse(sm.keyExists("0987654321"));
    }

    @Test
    public void testSubscriptionIsPaused() throws Exception {
        assertFalse(sm.subscriptionIsPaused("1234567890abcdef"));
        s.setAttribute("paused", "true");
        assertTrue(sm.subscriptionIsPaused("1234567890abcdef"));
        s.setAttribute("paused", "false");
    }

    @Test
    public void testAddSubscriber() throws Exception {
        Subscriber s2 = new Subscriber("0.0.0.0", 8080, "test", "test");
        s2.setAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN, "addtest");
        sm.addSubscriber(s2, sh);
        assertTrue(sm.keyExists("addtest"));
        assertTrue(tss.getAllSubscribers().contains(s2));
        tss.removeSubscriber(s2);
    }

    @Test
    public void testAddSubscriber1() throws Exception {
        int count = tss.getAllSubscribers().size();
        sm.addSubscriber("1234567890abcdef", 10L);
        int count2 = tss.getAllSubscribers().size();
        assertEquals(count, count2);
    }

    @Test
    public void testRemoveSubscriber() throws Exception {
        assertTrue(sm.hasSubscription(s.getAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN)));
        tss.removeSubscriber(s);
        assertFalse(sm.keyExists(s.getAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN)));
        assertFalse(tss.getAllSubscribers().contains(s));
    }

    @Test
    public void testGetAllRecipients() throws Exception {
        assertTrue(sm.getAllRecipients().contains("1234567890abcdef"));
        assertTrue(sm.getAllRecipients().size() == 1);
        Subscriber s2 = new Subscriber("0.0.0.0", 8001, "test2", "WSNotification");
        s2.setAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN, "asdf");
        sm.addSubscriber(s2, sh);
        assertTrue(sm.getAllRecipients().contains("asdf"));
    }

    @Test
    public void testGetSubscriptionHandle() throws Exception {
        AbstractNotificationProducer.SubscriptionHandle handle = sm.getSubscriptionHandle(s.getAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN));
        assertNotNull(handle);
        assertSame(handle, sh);
    }

    @Test
    public void testGetSubscriptionHandle1() throws Exception {
        AbstractNotificationProducer.SubscriptionHandle handle = sm.getSubscriptionHandle(s);
        assertNotNull(handle);
        assertSame(handle, sh);
    }

    @Test
    public void testGetSubscriber() throws Exception {
        Subscriber s2 = sm.getSubscriber(s.getAttribute(WSNSubscriptionManager.WSN_SUBSCRIBER_TOKEN));
        assertNotNull(s2);
        assertSame(s, s2);
    }

    @Test
    public void testUpdate() throws Exception {
        assertTrue(true);
    }

    @Test
    public void testUnsubscribe() throws Exception {
        assertTrue(sm.hasSubscription("1234567890abcdef"));
        Unsubscribe unsub = new Unsubscribe();
        UnsubscribeResponse response = sm.unsubscribe(unsub);
        assertNotNull(response);
        assertFalse(sm.hasSubscription("1234567890abcdef"));
    }

    @Test
    public void testRenew() throws Exception {
        Renew renew = new Renew();
        renew.setTerminationTime("2016-01-01T00:00:00");
        RenewResponse response = sm.renew(renew);
        assertEquals(response.getTerminationTime().getYear(), 2016);
        assertEquals(response.getTerminationTime().getMonth(), 1);
        assertEquals(response.getTerminationTime().getDay(), 1);
        assertEquals(response.getTerminationTime().getHour(), 0);
        assertEquals(response.getTerminationTime().getMinute(), 0);
        assertEquals(response.getTerminationTime().getSecond(), 0);
        LocalDateTime now = LocalDateTime.now();
        renew.setTerminationTime("P2Y");
        response = sm.renew(renew);
        assertEquals(response.getTerminationTime().getYear(), now.getYear() + 2);
        renew.setTerminationTime("P1D");
        response = sm.renew(renew);
        assertFalse(response.getTerminationTime().getDay() == now.getDayOfMonth());
        renew.setTerminationTime("gibberish");
        try {
            sm.renew(renew);
            fail();
        } catch (UnacceptableTerminationTimeFault e) {
            // This should be caught
        }
        try {
            renew.setTerminationTime("P48H2M1S");
            sm.renew(renew);
            fail();
        } catch (UnacceptableTerminationTimeFault e) {
            // This should be caught
        }
    }

    /* HELPER CLASSES */

    public class TestSubscriptionService extends SubscriptionService {
        private HashSet<Subscriber> subscribers = new HashSet<>();
        @Override
        public void addSubscriber(Subscriber s) { subscribers.add(s); }
        @Override
        public void removeSubscriber(Subscriber s) {
            subscribers.remove(s);
            sm.subscriptionChanged(new SubscriptionChangeEvent(SubscriptionChangeEvent.Type.UNSUBSCRIBE, s));
        }
        @Override
        public HashSet<Subscriber> getAllSubscribers() { return subscribers; }
    }

    public class TestServiceConnection implements ServiceConnection {
        private RequestInformation requestInformation = new RequestInformation();
        public InternalMessage acceptMessage(InternalMessage internalMessage) { return null; }
        public InternalMessage acceptRequest(InternalMessage internalMessage) { return null; }
        public Class getServiceType() { return null; }
        public String getServiceEndpoint() { return null; }
        public RequestInformation getRequestInformation() { return requestInformation; }
        public void endpointUpdated(String s) {}
        public Object getWebService() { return null; }
    }
}