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

package no.ntnu.okse.core.subscription;

import no.ntnu.okse.core.event.RegistrationChangeEvent;
import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.RegistrationChangeListener;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.topic.Topic;
import org.apache.log4j.Logger;

import java.util.HashSet;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class SubscriptionService {

    private Logger log;

    private HashSet<SubscriptionChangeListener> _subscriptionListeners;
    private HashSet<RegistrationChangeListener> _registrationListeners;
    private HashSet<Subscriber> _subscribers;
    private HashSet<Publisher> _publishers;

    public SubscriptionService() {
        log = Logger.getLogger(SubscriptionService.class.getName());
        _subscribers = new HashSet<>();
        _publishers = new HashSet<>();
        _registrationListeners = new HashSet<>();
        _subscriptionListeners = new HashSet<>();
    }

    /* Begin subscriber public API */
    public synchronized void addSubscriber(Subscriber s) {
        _subscribers.add(s);
        log.info("Added new subscriber: " + s);
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.SUBSCRIBE);
    }

    public synchronized void removeSubscriber(Subscriber s) {
        _subscribers.remove(s);
        log.info("Removed subscriber: " + s);
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.UNSUBSCRIBE);
    }

    public synchronized void pauseSubscriber(Subscriber s) {
        s.setAttribute("paused", "true");
        log.info("Subscriber paused: " + s);
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.PAUSE);
    }

    public synchronized void renewSubscriber(Subscriber s, Long timeout) {
        s.setTimeout(timeout);
        log.info("Subscriber renewed: " + s);
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.RENEW);
    }
    /* End subscriber public API */

    /* Begin publisher public API */
    public synchronized void addPublisher(Publisher p) {
        _publishers.add(p);
        log.info("Publisher registered: " + p);
        fireRegistrationChangeEvent(p, RegistrationChangeEvent.Type.REGISTER);
    }

    public synchronized void removePublisher(Publisher p) {
        _publishers.remove(p);
        log.info("Publisher removed: " + p);
        fireRegistrationChangeEvent(p, RegistrationChangeEvent.Type.REGISTER);
    }
    /* End publisher public API */

    /**
     * Attempt to locate a subscriber by remote address, port and topic object.
     * @param address : The remote IP or hostname of the client
     * @param port    : The remote port of the client
     * @param topic   : The topic object you want the subscriber object for
     * @return The Subscriber, if found, null otherwise.
     */
    public Subscriber getSubscriber(String address, Integer port, Topic topic) {
        for (Subscriber s: _subscribers) {
            if (s.getAddress().equals(address) &&
                s.getPort().equals(port) &&
                s.getTopic().equals(topic)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returning a copy of the _subscribers HashSet
     * @return collector : The HashSet containing all the subscribers
     */
    public HashSet getAllSubscribers() {
        HashSet<Subscriber> collector = (HashSet<Subscriber>) _subscribers.clone();
        return collector;
    }

    /**
     * SubscriptionChange event listener support
     * @param s : An object implementing the SubscriptionChangeListener interface
     */
    public synchronized void addSubscriptionChangeListener(SubscriptionChangeListener s) {
        _subscriptionListeners.add(s);
    }

    /**
     * SubscriptionChange event listener support
     * @param s : An object implementing the SubscriptionChangeListener interface
     */
    public synchronized void removeSubscriptionChangeListener(SubscriptionChangeListener s) {
        if (_subscriptionListeners.contains(s)) _subscriptionListeners.remove(s);
    }

    /**
     * Private helper method fo fire the subscriptionChange method on all listners.
     * @param sub   : The particular subscriber object that has changed.
     * @param type  : What type of action is associated with the subscriber object.
     */
    private void fireSubcriptionChangeEvent(Subscriber sub, SubscriptionChangeEvent.Type type) {
        SubscriptionChangeEvent sce = new SubscriptionChangeEvent(type, sub);
        _subscriptionListeners.stream().forEach(l -> l.subscriptionChanged(sce));
    }

    /**
     * RegistrationChange event listener support
     * @param r : An object implementing the RegistrationChangeListener interface
     */
    public synchronized void addRegistrationChangeListener(RegistrationChangeListener r) {
        _registrationListeners.add(r);
    }

    /**
     * RegistrationChange event listener support
     * @param r : An object implementing the RegistrationChangeListener interface
     */
    public synchronized void removeRegistrationChangeListener(RegistrationChangeListener r) {
        if (_registrationListeners.contains(r)) _registrationListeners.remove(r);
    }

    /**
     * Private helper method fo fire the registrationChange method on all listners.
     * @param reg   : The particular publisher object that has changed.
     * @param type  : What type of action is associated with the publisher object.
     */
    private void fireRegistrationChangeEvent(Publisher reg, RegistrationChangeEvent.Type type) {
        RegistrationChangeEvent rce = new RegistrationChangeEvent(type, reg);
        _registrationListeners.stream().forEach(l -> l.registrationChanged(rce));
    }

}
