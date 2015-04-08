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

import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.topic.Topic;

import java.util.HashSet;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class SubscriptionService {

    private HashSet<SubscriptionChangeListener> _listeners;
    private HashSet<Subscriber> _subscribers;

    public SubscriptionService() {
        _subscribers = new HashSet<>();
        _listeners = new HashSet<>();
    }

    public synchronized void addSubscriber(Subscriber s) {
        _subscribers.add(s);
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.SUBSCRIBE);
    }

    public synchronized void removeSubscriber(Subscriber s) {
        _subscribers.remove(s);
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.UNSUBSCRIBE);
    }

    public synchronized void pauseSubscriber(Subscriber s) {
        s.setAttribute("paused", "true");
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.PAUSE);
    }

    public synchronized void renewSubscriber(Subscriber s, Long timeout) {
        s.setTimeout(timeout);
        fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.RENEW);
    }

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
     * SubscriptionChange event listener support
     * @param s : An object implementing the SubscriptionChangeListener interface
     */
    public synchronized void addSubscriptionChangeListener(SubscriptionChangeListener s) {
        _listeners.add(s);
    }

    /**
     * SubscriptionChange event listener support
     * @param s : An object implementing the SubscriptionChangeListener interface
     */
    public synchronized void removeSubscriptionChangeListener(SubscriptionChangeListener s) {
        if (_listeners.contains(s)) _listeners.remove(s);
    }

    /**
     * Private helper method fo fire the subscriptionChange method on all listners.
     * @param sub   : The particular subscriber object that has changed.
     * @param type  : What type of action is associated with the subscriber object.
     */
    private void fireSubcriptionChangeEvent(Subscriber sub, SubscriptionChangeEvent.Type type) {
        SubscriptionChangeEvent sce = new SubscriptionChangeEvent(type, sub);
        _listeners.stream().forEach(l -> l.subscriptionChanged(sce));
    }

}
