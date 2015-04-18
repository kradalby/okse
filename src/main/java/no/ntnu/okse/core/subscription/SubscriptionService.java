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

import no.ntnu.okse.core.AbstractCoreService;
import no.ntnu.okse.core.event.PublisherChangeEvent;
import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.listeners.PublisherChangeListener;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.topic.Topic;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class SubscriptionService extends AbstractCoreService {

    private static SubscriptionService _singleton;
    private static Thread _serverThread;
    private static boolean _invoked = false;

    private LinkedBlockingQueue<SubscriptionTask> queue;
    private HashSet<SubscriptionChangeListener> _subscriptionListeners;
    private HashSet<PublisherChangeListener> _registrationListeners;
    private HashSet<Subscriber> _subscribers;
    private HashSet<Publisher> _publishers;

    /**
     * Private constructor that passes classname to superclass log field and calls initialization method
     */
    private SubscriptionService() {
        super(SubscriptionService.class.getName());
        init();
    }

    /**
     * The main instanciation method of SubscriptionService adhering to the singleton pattern
     * @return
     */
    public static SubscriptionService getInstance() {
        if (!_invoked) _singleton = new SubscriptionService();
        return _singleton;
    }

    /**
     * Initializing method
     */
    @Override
    protected void init() {
        log.info("Initializing SubscriptionSerice...");
        queue = new LinkedBlockingQueue<>();
        _subscribers = new HashSet<>();
        _publishers = new HashSet<>();
        _registrationListeners = new HashSet<>();
        _subscriptionListeners = new HashSet<>();
    }

    /**
     * Startup method that sets up the service
     */
    @Override
    public void boot() {
        if (!_running) {
            log.info("Booting SubscriptionService...");
            _serverThread = new Thread(() -> {
                _running = true;
                _singleton.run();
            });
            _serverThread.setName("SubscriptionService");
            _serverThread.start();
        }
    }

    /**
     * Main run method that will be called when the subclass' serverThread is started
     */
    @Override
    public void run() {
        log.info("SubscriptionService booted successfully");
        while (_running) {
            try {
                SubscriptionTask task = queue.take();
                log.info(task.getType() + " job recieved, executing task...");
            } catch (InterruptedException e) {
                log.warn("Interrupt caught, consider sending a No-Op Task to the queue to awaken the thread.");
            }
        }
    }

    /**
     * Graceful shutdown method
     */
    @Override
    public void stop() {
        _running = false;
        Runnable job = () -> log.info("Stopping SubscriptionService...");
        try {
            queue.put(new SubscriptionTask(SubscriptionTask.Type.SHUTDOWN, job));
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to inject shutdown event to queue");
        }
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
        firePublisherChangeEvent(p, PublisherChangeEvent.Type.REGISTER);
    }

    public synchronized void removePublisher(Publisher p) {
        _publishers.remove(p);
        log.info("Publisher removed: " + p);
        firePublisherChangeEvent(p, PublisherChangeEvent.Type.REGISTER);
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
     * PublisherChange event listener support
     * @param r : An object implementing the PublisherChangeListener interface
     */
    public synchronized void addPublisherChangeListener(PublisherChangeListener r) {
        _registrationListeners.add(r);
    }

    /**
     * PublisherChange event listener support
     * @param r : An object implementing the PublisherChangeListener interface
     */
    public synchronized void removePublisherChangeListener(PublisherChangeListener r) {
        if (_registrationListeners.contains(r)) _registrationListeners.remove(r);
    }

    /**
     * Private helper method fo fire the publisherChange method on all listners.
     * @param reg   : The particular publisher object that has changed.
     * @param type  : What type of action is associated with the publisher object.
     */
    private void firePublisherChangeEvent(Publisher reg, PublisherChangeEvent.Type type) {
        PublisherChangeEvent rce = new PublisherChangeEvent(type, reg);
        _registrationListeners.stream().forEach(l -> l.publisherChanged(rce));
    }
}
