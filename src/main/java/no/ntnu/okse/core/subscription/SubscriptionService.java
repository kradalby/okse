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

import no.ntnu.okse.Application;
import no.ntnu.okse.core.AbstractCoreService;
import no.ntnu.okse.core.event.PublisherChangeEvent;
import no.ntnu.okse.core.event.SubscriptionChangeEvent;
import no.ntnu.okse.core.event.TopicChangeEvent;
import no.ntnu.okse.core.event.listeners.PublisherChangeListener;
import no.ntnu.okse.core.event.listeners.SubscriptionChangeListener;
import no.ntnu.okse.core.event.listeners.TopicChangeListener;
import no.ntnu.okse.core.topic.TopicService;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class SubscriptionService extends AbstractCoreService implements TopicChangeListener {

    private static SubscriptionService _singleton;
    private static Thread _serviceThread;
    private static boolean _invoked = false;

    // Is the scheduled auto removal of expired subs and pubs active?
    private boolean autoPurgeRunning = false;

    private LinkedBlockingQueue<SubscriptionTask> queue;
    private ScheduledExecutorService scheduler;
    private Properties config;
    private ConcurrentHashSet<SubscriptionChangeListener> _subscriptionListeners;
    private ConcurrentHashSet<PublisherChangeListener> _registrationListeners;
    private ConcurrentHashSet<Subscriber> _subscribers;
    private ConcurrentHashSet<Publisher> _publishers;

    /**
     * Private constructor that passes classname to superclass log field and calls initialization method
     */
    protected SubscriptionService() {
        super(SubscriptionService.class.getName());
        init();
    }

    /**
     * The main instanciation method of SubscriptionService adhering to the singleton pattern
     *
     * @return singleton instance of SubscriptionService
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
        _invoked = true;
        config = Application.readConfigurationFiles();
        log.info("Initializing SubscriptionService...");
        queue = new LinkedBlockingQueue<>();
        scheduler = Executors.newScheduledThreadPool(1);
        _subscribers = new ConcurrentHashSet<>();
        _publishers = new ConcurrentHashSet<>();
        _registrationListeners = new ConcurrentHashSet<>();
        _subscriptionListeners = new ConcurrentHashSet<>();
    }

    /**
     * Startup method that sets up the service
     */
    @Override
    public void boot() {
        if (!_running) {
            log.info("Booting SubscriptionService...");
            _serviceThread = new Thread(() -> {
                _running = true;
                _singleton.run();
            });
            _serviceThread.setName("SubscriptionService");
            _serviceThread.start();
        }
    }

    /**
     * This method must contain the operations needed for this class to register itself as a listener
     * to the different instances it wants to listen to. This method will be called after all Core Services have
     * been booted.
     */
    @Override
    public void registerListenerSupport() {
        // Register self as listener to TopicService events
        TopicService.getInstance().addTopicChangeListener(this);
    }

    /**
     * Main run method that will be called when the subclass' serverThread is started
     */
    @Override
    public void run() {
        log.info("SubscriptionService booted successfully");
        // Start the scheduled task
        startScheduledRemovalOfExpiredSubscribersAndPublishers();

        while (_running) {
            try {
                SubscriptionTask task = queue.take();
                log.debug(task.getType() + " job received, executing task...");
                // Perform the task
                task.run();
            } catch (InterruptedException e) {
                log.warn("Interrupt caught, consider sending a No-Op Task to the queue to awaken the thread.");
            }
        }
        log.debug("SubscriptionService exited main run loop");
    }

    /**
     * Graceful shutdown method
     */
    @Override
    public void stop() {
        _running = false;
        removeAllListeners();
        Runnable job = () -> log.info("Stopping SubscriptionService...");
        try {
            queue.put(new SubscriptionTask(SubscriptionTask.Type.SHUTDOWN, job));
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to inject shutdown event to queue");
        }
    }

    /* ------------------------------------------------------------------------------------------ */

    /* Begin Service-Local methods */

    /**
     * This method starts a scheduled job that periodically removes expired subscribers and publishers
     * from the SubscriptionService registry. Delegates its work to the purgeExpiredSubscribersAndPublishers()
     * method.
     */
    private void startScheduledRemovalOfExpiredSubscribersAndPublishers() {
        if (!autoPurgeRunning) {
            log.info("Starting periodic removal of expired subscribers and publishers (1 min interval)");
            scheduler.scheduleAtFixedRate(() -> purgeExpiredSubscribersAndPublishers(), 1, 1, TimeUnit.MINUTES);
        } else {
            log.warn("Attempt to start scheduled removal of subscribers and publishers when its already started");
        }
    }

    /**
     * Purge expired Subscribers and Publishers. This method should be run as a periodic job, and it delegates
     * the actual removal to the removeSubscriber and removePublisher methods, that in turn injects the operations
     * as SubscriptionTask into the task queue.
     */
    private void purgeExpiredSubscribersAndPublishers() {
        log.debug("Running scheduled purge of expired subscribers and publishers");
        getAllSubscribers().stream().filter(s -> s.hasExpired()).forEach(s -> removeSubscriber(s));
        getAllPublishers().stream().filter(p -> p.hasExpired()).forEach(p -> removePublisher(p));
    }

    /**
     * This helper method injects a task into the task queue and handles interrupt exceptions
     *
     * @param task The SubscriptionTask to be executed
     */
    private void insertTask(SubscriptionTask task) {
        try {
            // Inject the task into the task queue
            this.queue.put(task);
        } catch (InterruptedException e) {
            log.error("Interrupted while injecting task into queue");
        }
    }

    /**
     * Service-local private method to add a Subscriber to the list of subscribers
     *
     * @param s : A Subscriber instance with the proper fields set
     */
    private void addSubscriberLocal(Subscriber s) {
        if (!_subscribers.contains(s)) {
            // Add the subscriber
            _subscribers.add(s);
            log.info("Added new subscriber: " + s);
            // Fire the subscribe event
            fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.SUBSCRIBE);
        } else {
            log.warn("Attempt to add a subscriber that already exists!");
        }
    }

    /**
     * Service-local private method to remove a subscriber from the list of subscribers
     *
     * @param s : A Subscriber instance that exists in the subscribers set
     */
    private void removeSubscriberLocal(Subscriber s) {
        if (_subscribers.contains(s)) {
            // Remove the subscriber
            _subscribers.remove(s);
            log.info("Removed subscriber: " + s);
            // Fire the unsubscribe event
            fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.UNSUBSCRIBE);
        } else {
            log.warn("Attempt to remove a subscriber that did not exist!");
        }
    }

    /**
     * Service-local private method to renew the subscription for a particular subscriber
     *
     * @param s       : The subscriber that is to be changed
     * @param timeout : The new timeout time represented as seconds since unix epoch
     */
    private void renewSubscriberLocal(Subscriber s, long timeout) {
        if (_subscribers.contains(s)) {
            // Update the timeout field
            s.setTimeout(timeout);
            log.info("Renewed subscriber: " + s);
            // Fire the renew event
            fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.RENEW);
        } else {
            log.warn("Attempt to modify a subscriber that does not exist in the service!");
        }
    }

    /**
     * Service-local private method to pause the subscription for a particular subscriber
     *
     * @param s : The subscriber that is to be paused
     */
    private void pauseSubscriberLocal(Subscriber s) {
        if (_subscribers.contains(s)) {
            // Set the Paused attribute to true
            s.setAttribute("paused", "true");
            log.info("Paused subscriber: " + s);
            // Fire the pause event
            fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.PAUSE);
        } else {
            log.warn("Attempt to modify a subscriber that does not exist in the service!");
        }
    }

    /**
     * Service-local private method to reusme the subscription for a particular subscriber
     *
     * @param s The subscriber that is to be resumed
     */
    private void resumeSubscriberLocal(Subscriber s) {
        if (_subscribers.contains(s)) {
            // Set the Paused attribute to false
            s.setAttribute("paused", "false");
            log.info("Resumed subscriber: " + s);
            // FIre the resume event
            fireSubcriptionChangeEvent(s, SubscriptionChangeEvent.Type.RESUME);
        } else {
            log.warn("Attempt to modify a subscriber that does not exist in the service!");
        }
    }

    /**
     * Service-local private method to register a publisher to the publisher set
     *
     * @param p : The publisher object that is to be registered
     */
    private void addPublisherLocal(Publisher p) {
        if (!_publishers.contains(p)) {
            // Add the publisher
            _publishers.add(p);
            log.info("Added publisher: " + p);
            // Fire the register event
            firePublisherChangeEvent(p, PublisherChangeEvent.Type.REGISTER);
        } else {
            log.warn("Attempt to add a publisher that already exists!");
        }
    }

    /**
     * Service-local private method to unregister a publisher from the publisher set
     *
     * @param p : A publisher object that exists in the publishers set
     */
    private void removePublisherLocal(Publisher p) {
        if (_publishers.contains(p)) {
            // Remove the publisher
            _publishers.remove(p);
            log.info("Removed publisher: " + p);
            // Fire the remove event
            firePublisherChangeEvent(p, PublisherChangeEvent.Type.UNREGISTER);
        }
    }
    /* End Service-Local methods */

    /* ------------------------------------------------------------------------------------------ */

    /* Begin subscriber public API */

    /**
     * Public method to add a Subscriber
     *
     * @param s The subscriber to be added
     */
    public void addSubscriber(Subscriber s) {
        if (s == null) {
            log.warn("Received null argument!");
            return;
        }
        if (!_subscribers.contains(s)) {
            // Create the job
            Runnable job = () -> addSubscriberLocal(s);
            // Initialize the SubscriptionTask wrapper
            SubscriptionTask task = new SubscriptionTask(SubscriptionTask.Type.NEW_SUBSCRIBER, job);
            // Inject the task
            insertTask(task);
        } else {
            log.warn("Attempt to add a subscriber that already exists!");
        }
    }

    /**
     * Public method to remove a Subscriber
     *
     * @param s A subscriber that exists in the subscribers set
     */
    public void removeSubscriber(Subscriber s) {
        if (s == null) {
            log.warn("Received null argument!");
            return;
        }
        if (_subscribers.contains(s)) {
            // Create the job
            Runnable job = () -> removeSubscriberLocal(s);
            // Initialize the SubscriptionTask wrapper
            SubscriptionTask task = new SubscriptionTask(SubscriptionTask.Type.DELETE_SUBSCRIBER, job);
            // Inject the task
            insertTask(task);
        } else {
            log.warn("Attempt to remove a subscriber that did not exist!");
        }
    }

    /**
     * Public method to renew a subscription
     *
     * @param s       The subscriber object that is to be renewed
     * @param timeout The new timeout of the subscription represented as seconds since unix epoch
     */
    public void renewSubscriber(Subscriber s, Long timeout) {
        if (s == null) {
            log.warn("Received null argument!");
            return;
        }
        if (_subscribers.contains(s)) {
            // Create the job
            Runnable job = () -> renewSubscriberLocal(s, timeout);
            // Initialize the SubscriptionTask wrapper
            SubscriptionTask task = new SubscriptionTask(SubscriptionTask.Type.UPDATE_SUBSCRIBER, job);
            // Inject the task
            insertTask(task);
        } else {
            log.warn("Attempt to modify a subscriber that did not exist in the service!");
        }
    }

    /**
     * Public method to pause a subscription
     *
     * @param s The subciber object that is to be paused
     */
    public void pauseSubscriber(Subscriber s) {
        if (s == null) {
            log.warn("Received null argument!");
            return;
        }
        if (_subscribers.contains(s)) {
            // Create the job
            Runnable job = () -> pauseSubscriberLocal(s);
            // Initialize the SubscriptionTask wrapper
            SubscriptionTask task = new SubscriptionTask(SubscriptionTask.Type.UPDATE_SUBSCRIBER, job);
            // Inject the task
            insertTask(task);
        } else {
            log.warn("Attempt to modify a subscriber that did not exist in the service!");
        }
    }

    /**
     * Public method to resume a subscription
     *
     * @param s The subscriber object that is to be resumed
     */
    public void resumeSubscriber(Subscriber s) {
        if (s == null) {
            log.warn("Received null argument!");
            return;
        }
        if (_subscribers.contains(s)) {
            // Create the job
            Runnable job = () -> resumeSubscriberLocal(s);
            // Initialize the SubscriptionTask wrapper
            SubscriptionTask task = new SubscriptionTask(SubscriptionTask.Type.UPDATE_SUBSCRIBER, job);
            // Inject the task
            insertTask(task);
        }
    }
    /* End subscriber public API */

    /* ------------------------------------------------------------------------------------------ */

    /* Begin publisher public API */

    /**
     * Public method to register a publisher
     *
     * @param p The publisher object that is to be registered
     */
    public void addPublisher(Publisher p) {
        if (p == null) {
            log.warn("Received null argument!");
            return;
        }
        if (!_publishers.contains(p)) {
            // Create the job
            Runnable job = () -> addPublisherLocal(p);
            // Initialize the SubscriptionTask wrapper
            SubscriptionTask task = new SubscriptionTask(SubscriptionTask.Type.NEW_PUBLISHER, job);
            // Inject the task
            insertTask(task);
        } else {
            log.warn("Attempt to add a publisher that already exists!");
        }
    }

    /**
     * Public method to unregister a publisher
     *
     * @param p A publisher object that exists in the publishers set
     */
    public void removePublisher(Publisher p) {
        if (p == null) {
            log.warn("Received null argument!");
            return;
        }
        if (_publishers.contains(p)) {
            // Create the job
            Runnable job = () -> removePublisherLocal(p);
            // Initialize the SubscriptionTask wrapper
            SubscriptionTask task = new SubscriptionTask(SubscriptionTask.Type.DELETE_PUBLISHER, job);
            // Inject the task
            insertTask(task);
        } else {
            log.warn("Attempt to add a publisher that did not exist in the service!");
        }
    }
    /* End publisher public API */

    /* ------------------------------------------------------------------------------------------ */

    /**
     * Attempt to locate a subscriber by the ID.
     *
     * @param id : The ID for the subscriber
     * @return The subscriber, if found, null otherwise.
     */
    public Subscriber getSubscriberByID(String id) {
        List<Subscriber> result = _subscribers.stream()
                .filter(s -> s.getSubscriberID().equals(id))
                .collect(Collectors.toList());

        if (result.size() > 1) {
            log.warn("Found multiple subscribers with the same hash/ID.");
            return null;
        } else if (result.size() != 1) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Retrieve a HashSet of all subscribers that have subscribed to a specific topic
     *
     * @param topic A raw topic string of the topic to select subscribers from
     * @return A HashSet of Subscriber objects that have subscribed to the specified topic
     */
    public HashSet<Subscriber> getAllSubscribersForTopic(String topic) {
        // Initialize a collector
        HashSet<Subscriber> results = new HashSet<>();

        // Iterate over all subscribers
        getAllSubscribers().stream()
                // Only pass on those who match topic argument
                .filter(s -> s.getTopic() == null || s.getTopic().equals(topic))
                        // Collect in the results set
                .forEach(s -> results.add(s));

        return results;
    }

    /**
     * Retrieve a HashSet of all subscribers that have subscribed to a specific topic
     *
     * @param topic A raw topic string of the topic to select subscribers from
     * @return A HashSet of Subscriber objects that have subscribed to the specified topic
     */
    public HashSet<Publisher> getAllPublishersForTopic(String topic) {
        // Initialize a collector
        HashSet<Publisher> results = new HashSet<>();

        // Iterate over all subscribers
        getAllPublishers().stream()
                // Only pass on those who match topic argument
                .filter(p -> p.getTopic().equals(topic))
                        // Collect in the results set
                .forEach(p -> results.add(p));

        return results;
    }

    /**
     * Retrive a HashSet of all subscribers on the broker. This HashSet is a shallow clone of the internal
     * data structure, preventing unintended modification.
     *
     * @return A HashSet of Subscriber objects that have subscribed on the broker
     */
    public HashSet<Subscriber> getAllSubscribers() {
        HashSet<Subscriber> result = new HashSet<>();
        _subscribers.iterator().forEachRemaining(s -> result.add(s));
        return result;
    }

    /**
     * Retrieve a HashSet of all publishers on the broker. This HashSet is a shallow clone of the internal
     * data structure, preventing unintended modification.
     *
     * @return A HashSet of Publisher objects that have registered on the broker
     */
    public HashSet<Publisher> getAllPublishers() {
        HashSet<Publisher> result = new HashSet<>();
        _publishers.iterator().forEachRemaining(p -> result.add(p));
        return result;
    }

    /**
     * Fetch the number of registered subscribers
     *
     * @return The total number of subscribers
     */
    public int getNumberOfSubscribers() {
        return _subscribers.size();
    }

    /**
     * Fetch the number of registered publishers
     *
     * @return The total number of publishers
     */
    public int getNumberOfPublishers() {
        return _publishers.size();
    }

    /* ------------------------------------------------------------------------------------------ */

    /* Begin listener support */

    /**
     * Purges all registered listener objects from the SubscriptionService
     */
    public synchronized void removeAllListeners() {
        _subscriptionListeners.clear();
        _registrationListeners.clear();
    }

    /**
     * SubscriptionChange event listener support
     *
     * @param s : An object implementing the SubscriptionChangeListener interface
     */
    public synchronized void addSubscriptionChangeListener(SubscriptionChangeListener s) {
        _subscriptionListeners.add(s);
    }

    /**
     * SubscriptionChange event listener support
     *
     * @param s : An object implementing the SubscriptionChangeListener interface
     */
    public synchronized void removeSubscriptionChangeListener(SubscriptionChangeListener s) {
        if (_subscriptionListeners.contains(s)) _subscriptionListeners.remove(s);
    }

    /**
     * Private helper method fo fire the subscriptionChange method on all listners.
     *
     * @param sub  : The particular subscriber object that has changed.
     * @param type : What type of action is associated with the subscriber object.
     */
    private void fireSubcriptionChangeEvent(Subscriber sub, SubscriptionChangeEvent.Type type) {
        SubscriptionChangeEvent sce = new SubscriptionChangeEvent(type, sub);
        _subscriptionListeners.stream().forEach(l -> l.subscriptionChanged(sce));
    }

    /**
     * PublisherChange event listener support
     *
     * @param r : An object implementing the PublisherChangeListener interface
     */
    public synchronized void addPublisherChangeListener(PublisherChangeListener r) {
        _registrationListeners.add(r);
    }

    /**
     * PublisherChange event listener support
     *
     * @param r : An object implementing the PublisherChangeListener interface
     */
    public synchronized void removePublisherChangeListener(PublisherChangeListener r) {
        if (_registrationListeners.contains(r)) _registrationListeners.remove(r);
    }

    /**
     * Private helper method fo fire the publisherChange method on all listners.
     *
     * @param reg  : The particular publisher object that has changed.
     * @param type : What type of action is associated with the publisher object.
     */
    private void firePublisherChangeEvent(Publisher reg, PublisherChangeEvent.Type type) {
        PublisherChangeEvent pce = new PublisherChangeEvent(type, reg);
        _registrationListeners.stream().forEach(l -> l.publisherChanged(pce));
    }

    /* End listener support */

    /* Start observation methods */

    @Override
    public void topicChanged(TopicChangeEvent event) {
        if (event.getType().equals(TopicChangeEvent.Type.DELETE)) {

            // Fetch the raw topic string from the event Topic object
            String fullRawTopicString = event.getData().getFullTopicString();

            // Remove all the subscribers for the topic that was deleted
            getAllSubscribersForTopic(fullRawTopicString).forEach(s -> removeSubscriber(s));
            // Remove all the publishers for the topic that was deleted
            getAllPublishersForTopic(fullRawTopicString).forEach(p -> removePublisher(p));
        }
    }

    /* End observation methods */
}
