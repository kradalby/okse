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

package no.ntnu.okse.core;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.event.Event;
import no.ntnu.okse.core.event.SystemEvent;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.protocol.ProtocolServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 2/25/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class CoreService extends AbstractCoreService {

    // Common service fields
    private static CoreService _singleton;
    private static Thread _serviceThread;
    private static boolean _invoked = false;

    // Service specific fields
    private LinkedBlockingQueue<Event> eventQueue;
    private ExecutorService executor;
    private HashSet<AbstractCoreService> services;
    private ArrayList<ProtocolServer> protocolServers;
    private Properties config;
    public static boolean protocolServersBooted = false;

    /**
     * Constructs the CoreService instance. Constructor is private due to the singleton pattern used for
     * core services.
     */
    protected CoreService() {
        // Pass the className to superclass for logger initialization
        super(CoreService.class.getName());
        if (_invoked) throw new IllegalStateException("Already invoked");
        init();
    }

    /**
     * Main instanciation method adhering to the singleton pattern
     *
     * @return The CoreService instance
     */
    public static CoreService getInstance() {
        if (!_invoked) _singleton = new CoreService();
        return _singleton;
    }

    /**
     * Initializing method
     */
    @Override
    protected void init() {
        config = Application.readConfigurationFiles();
        log.debug("Initializing CoreService");
        eventQueue = new LinkedBlockingQueue();
        services = new HashSet<>();
        protocolServers = new ArrayList<>();
        // Initialize the ExecutorService (Dynamic threadpool that increases and decreases on demand in runtime)
        executor = Executors.newCachedThreadPool();
        // Set the invoked flag
        _invoked = true;
    }

    /**
     * Startup method that sets up the service
     */
    @Override
    public void boot() {
        if (!_running) {
            log.info("Booting CoreService...");
            _serviceThread = new Thread(() -> {
                _running = true;
                _singleton.run();
            });
            _serviceThread.setName("CoreService");
            _serviceThread.start();
        }
    }

    /**
     * This method can contain registration calls to objects that CoreService should listen to.
     * Please note that in order for the CoreService itself to listen to other registered core services
     * this method must be called AFTER the bootAllCoreServices() method has completed in the boot() sequence of
     * this class
     */
    @Override
    public void registerListenerSupport() {
        // Add listener registration here
    }

    /**
     * Starts the main loop of the CoreService thread.
     */
    @Override
    public void run() {
        _running = true;
        log.info("CoreService booted successfully");

        // Call the boot() method on all registered Core Services
        log.info("Booting core services");
        this.bootCoreServices();
        log.info("Completed core services");

        // Call the boot() method on all registered ProtocolServers
        log.info("Booting ProtocolServers");
        // Since this is CoreService bootup, we iterate directly, avoiding unnecessary overhead caused
        // by the bootProtocolServers() method, that is used during live start / stop of protocol servers.
        protocolServers.forEach(ps -> ps.boot());
        protocolServersBooted = true;
        log.info("Completed booting ProtocolServers");

        // Call the registerListenerSupport() method on all registered Core , including self
        log.info("Setting up listener support for all core services");
        this.registerListenerSupportForAllCoreServices();
        log.info("Completed setting up listener support for all core services");

        log.info("OKSE " + Application.VERSION + " booted in " + Utilities.getDurationAsISO8601(Application.getRunningTime()));

        // Initiate main run loop, which awaits Events to be committed to the eventQueue
        while (_running) {
            try {
                Event e = eventQueue.take();
                log.debug("Consumed an event: " + e);
                // Are we shutting down protocol servers?
                if (e.getType().equals(SystemEvent.Type.SHUTDOWN_PROTOCOL_SERVERS)) stopAllProtocolServers();
                    // Are we booting protocol servers?
                else if (e.getType().equals(SystemEvent.Type.BOOT_PROTOCOL_SERVERS)) bootProtocolServers();
            } catch (InterruptedException e) {
                log.error("Interrupted while attempting to fetch next event from eventQueue");
            }
        }
        // We have passed the main run loop, which means we are shutting down.
        log.info("CoreService stopped");
    }

    /**
     * Stops execution of the CoreService thread.
     */
    @Override
    public void stop() {
        // Shut down all the Protocol Servers
        stopAllProtocolServers();

        // Give the threads a few seconds to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.warn("Interrupted during shutdown sleep");
        }
        // Shut down all the Core Services
        this.services.forEach(s -> s.stop());

        // Turn of run flag
        _running = false;

        try {
            // Inject a SHUTDOWN event into eventQueue
            eventQueue.put(new SystemEvent(SystemEvent.Type.SHUTDOWN, null));
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to inject the SHUTDOWN event to eventQueue");
        }
    }

    /* Begin Public API */

    /**
     * This command executes a job implementing the Runnable interface
     *
     * @param r The Runnable job to be executed
     */
    public void execute(Runnable r) {
        this.executor.execute(r);
    }

    /**
     * Fetches the eventQueue.
     * <p/>
     *
     * @return The eventQueue list
     */
    public LinkedBlockingQueue<Event> getEventQueue() {
        return eventQueue;
    }

    /**
     * Fetches the ExecutorService responsible for running tasks
     * <p/>
     *
     * @return The ExecutorService
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * This method takes in an instance extending the AbstractCoreService class, the foundation for all OKSE
     * core extensions and registers it to the Core Service for startup and execution
     *
     * @param service
     */
    public void registerService(AbstractCoreService service) {
        if (!services.contains(service)) services.add(service);
        else log.error("Attempt to register a core service that has already been registered!");
    }

    /**
     * This method takes in an instance extending the AbstractCoreService class, the foundation for all OKSE
     * core extensions, and removes it from the set of registered services. Thir process will first invoke
     * the stop() method on the service.
     *
     * @param service
     */
    public void removeService(AbstractCoreService service) {
        if (services.contains(service)) {
            // Stop the service
            service.stop();
            // Remove it from the set
            services.remove(service);
        } else {
            log.error("Attempt to remove a core service that does not exist in the registry!");
        }
    }

    /**
     * Retrieves a service based on its class. Can be used to test if a service is registered
     *
     * @param serviceClass The class of the service to fetch
     * @return A core service extending AbstractCoreService
     */
    public AbstractCoreService getService(Class serviceClass) {
        for (AbstractCoreService service : services) {
            if (service.getClass().equals(serviceClass)) return service;
        }
        return null;
    }

    /**
     * Adds a protocolserver to the protocolservers list.
     *
     * @param ps: An instance of a subclass of AbstractProtocolServer that implements ProtocolServer
     */
    public void addProtocolServer(ProtocolServer ps) {
        if (!protocolServers.contains(ps)) protocolServers.add(ps);
    }

    /**
     * Removes a protocolserver to the protocolservers list.
     *
     * @param ps: An instance of a subclass of AbstractProtocolServer that implements ProtocolServer
     */
    public void removeProtocolServer(ProtocolServer ps) {
        if (protocolServers.contains(ps)) protocolServers.remove(ps);
    }

    /**
     * Statistics for total number of requests that has passed through all protocol servers
     *
     * @return: An integer representing the total amount of requests.
     */
    public int getTotalRequestsFromProtocolServers() {
        return getAllProtocolServers().stream().map(ProtocolServer::getTotalRequests).reduce(0, (a, b) -> a + b);
    }

    /**
     * Statistics for total number of messages that has been received through all protocol servers
     *
     * @return: An integer representing the total amount of messages received.
     */
    public int getTotalMessagesReceivedFromProtocolServers() {
        return getAllProtocolServers().stream().map(ProtocolServer::getTotalMessagesReceived).reduce(0, (a, b) -> a + b);
    }

    /**
     * Statistics for total number of messages that has been sent through all protocol servers
     *
     * @return An integer representing the total number of messages sent
     */
    public int getTotalMessagesSentFromProtocolServers() {
        return getAllProtocolServers().stream().map(ProtocolServer::getTotalMessagesSent).reduce(0, (a, b) -> a + b);
    }

    /**
     * Statistics for total number of bad or malformed requests that has passed through all protocol servers
     *
     * @return: An integer representing the total amount of bad or malformed requests
     */
    public int getTotalBadRequestsFromProtocolServers() {
        return getAllProtocolServers().stream().map(ProtocolServer::getTotalBadRequests).reduce(0, (a, b) -> a + b);
    }

    /**
     * Statistics for total number of errors generated through all protocol servers
     *
     * @return: An integer representing the total amount of errors from protocol servers.
     */
    public int getTotalErrorsFromProtocolServers() {
        return getAllProtocolServers().stream().map(ProtocolServer::getTotalErrors).reduce(0, (a, b) -> a + b);
    }

    /**
     * Fetches the ArrayList of ProtocolServers currently added to CoreService.
     *
     * @return: An ArrayList of ProtocolServers that are registered. Returns an empty ArrayList if not booted.
     */
    public ArrayList<ProtocolServer> getAllProtocolServers() {
        if (protocolServersBooted) return this.protocolServers;
        else return new ArrayList<>();
    }

    /**
     * Helper method to fetch a protocol server defined by the actual Class
     *
     * @param className: The class which the protocol server should be an actual instance of (e.g not subclass etc)
     * @return The ProtocolServer that matches the specified Class, null otherwise. If not null, the returned object
     * can be safely cast to the specified Class.
     */
    public ProtocolServer getProtocolServer(Class className) {
        for (ProtocolServer ps : protocolServers) {
            if (className.equals(ps.getClass())) return ps;
        }
        return null;
    }

    /**
     * This method stops all protocol servers after delivering a system message through the message service.
     * Based on the settings for BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS, it might distribute the system
     * message to all topics first, causing this method to have to sleep cycle until the system message has been processed.
     */
    public void stopAllProtocolServers() {

        log.info("Stopping all ProtocolServers...");

        // Create a system message
        Message m = new Message("The broker is shutting down", null, null, Application.OKSE_SYSTEM_NAME);
        m.setSystemMessage(true);

        // Distribute the message to the Message Service
        MessageService.getInstance().distributeMessage(m);

        // Wait until message is processed
        while (!m.isProcessed()) {
            try {
                // Make the thread sleep a bit, before re-checking message status
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Interrupted during protocol server shutdown message confirmation sleep cycle");
            }
        }

        // Iterate over all protocol servers and initiate shutdown process
        getAllProtocolServers().forEach(ps -> ps.stopServer());
        protocolServersBooted = false;

        // Let the thread wait a bit, for tasks to be completed.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Interrupt during shutdown wait, please dont.");
        }

        // Removes all listeners
        TopicService.getInstance().removeAllListeners();
        SubscriptionService.getInstance().removeAllListeners();
        // Reinitialize listener support for the core services
        registerListenerSupportForAllCoreServices();

        log.info("Completed dispatching SHUTDOWN to all ProtocolServers");
    }

    /**
     * Helper method to fetch a protocol server defined by a protocolServerType string.
     *
     * @param protocolServerType: A string representing the type of the protocol server you want to fetch.
     * @return The ProtocolServer that matches the specified string, null otherwise. If not null, the returned object
     * can be safely cast to the class that has a defined protocolServerType field equal to the specified
     * argument.
     */
    public ProtocolServer getProtocolServer(String protocolServerType) {
        for (ProtocolServer ps : protocolServers) {
            if (ps.getProtocolServerType().equalsIgnoreCase(protocolServerType)) return ps;
        }
        return null;
    }

    /* Begin private helper methods */

    /**
     * Helper method that boots all registered core services
     */
    private void bootCoreServices() {
        services.forEach(s -> s.boot());
    }

    /**
     * Helper method that boots all added protocolservers
     */
    public void bootProtocolServers() {
        // If they are already booted, return.
        if (protocolServersBooted) return;

        // Fetch the protocolserver Classes registered
        ArrayList<Class> protocolServerClasses = new ArrayList<>();
        protocolServers.forEach(ps -> protocolServerClasses.add(ps.getClass()));
        // Clear the cached protocol servers from core service registry
        protocolServers.clear();
        // For each of the fetched classes that was in cache
        protocolServerClasses.forEach(clazz -> {
            try {
                // Fetch the getInstance method from the class
                Method instanceMethod = clazz.getMethod("getInstance", null);
                // Cast the returned instance as a ProtocolServer
                ProtocolServer ps = (ProtocolServer) instanceMethod.invoke(null, null);
                // Add the invoked instance to the ProtocolServer registry again
                addProtocolServer(ps);
            } catch (NoSuchMethodException e) {
                log.error("Failed to locate getInstance method on " + clazz + ". Is it implemented properly?");
            } catch (InvocationTargetException e) {
                log.error("Failed to invoke getInstance method on " + clazz + ". Is it implemented properly?");
            } catch (IllegalAccessException e) {
                log.error("Failed to invoke getInstance method on " + clazz + ". It needs to be public access.");
            }
        });
        // At this point, previously cached registered protocol servers have been removed, temporarily
        // retaining the Class from each protocol server. They have been reinvoked and the new instances
        // have been reinserted into the registry. Time to boot.
        protocolServers.forEach(ps -> ps.boot());
        protocolServersBooted = true;
    }


    /**
     * Private helper method that sets up listener support for all registered core services
     */
    private void registerListenerSupportForAllCoreServices() {
        // Register listener registration on self
        this.registerListenerSupport();
        // Register listener support on other registered core services
        services.forEach(s -> s.registerListenerSupport());

    }
}
