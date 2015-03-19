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

import no.ntnu.okse.core.event.Event;

import no.ntnu.okse.protocol.ProtocolServer;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 2/25/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class CoreService extends Thread {

    private volatile boolean running;
    private static Logger log;
    private LinkedBlockingQueue<Event> eventQueue;
    private ExecutorService executor;
    private ArrayList<ProtocolServer> protocolServers;

    /**
     * Constructs the CoreService thread, initiates the logger and eventQueue.
     */
    public CoreService() {
        super("CoreService");
        running = false;
        log = Logger.getLogger(CoreService.class.getName());
        eventQueue = new LinkedBlockingQueue();
        executor = Executors.newFixedThreadPool(10);
        protocolServers = new ArrayList<>();
    }

    /**
     * Fetches the eventQueue.
     * <p>
     * @return The eventQueue list
     */
    public LinkedBlockingQueue<Event> getEventQueue() {
        return eventQueue;
    }

    /**
     * Fetches the ExecutorService responsible for running tasks
     * <p>
     * @return The ExecutorService
     */
    public ExecutorService getExecutor() { return executor; }

    /**
     * Adds a protocolserver to the protocolservers list.
     * @param ps: An instance of a subclass of AbstractProtocolServer that implements ProtocolServer
     */
    public void addProtocolServer(ProtocolServer ps) {
        if (!protocolServers.contains(ps)) protocolServers.add(ps);
    }

    /**
     * Removes a protocolserver to the protocolservers list.
     * @param ps: An instance of a subclass of AbstractProtocolServer that implements ProtocolServer
     */
    public void removeProtocolServer(ProtocolServer ps) {
        if (protocolServers.contains(ps)) protocolServers.remove(ps);
    }

    /**
     * Fetches the ArrayList of ProtocolServers currently added to CoreService.
     * @return: An ArrayList of ProtocolServers
     */
    public ArrayList<ProtocolServer> getAllProtocolServers() { return this.protocolServers; }

    /**
     * Helper method that boots all added protocolservers.
     */
    private void bootProtocolServers() {
        protocolServers.forEach(ps -> ps.boot());
    }


    // TODO: Create a method called getProtocolServer(Class classname) that locates the PS
    // TODO: that is an instance of the given class, and returns it, allowing for correct casting
    // TODO: on the recieving end.

    /**
     * Starts the main loop of the CoreService thread.
     */
    @Override
    public void run() {
        running = true;
        log.info("CoreService started.");
        log.info("Attempting to boot ProtocolServers.");

        // Call the boot() method on all registered ProtocolServers
        this.bootProtocolServers();

        log.info("Completed booting ProtocolServers.");

        // Initiate main run loop, which awaits Events to be committed to the eventQueue
        while (running) {
            try {
                Event e = eventQueue.take();
                log.info("Consumed an event: " + e.getOperation() + " DataType: " + e.getDataType());
            } catch (InterruptedException e) {
                log.trace(e.getStackTrace());
            }
        }
        log.info("CoreService stopped.");
    }

    /**
     * Stops execution of the CoreService thread.
     */
    public void stopThread() {
        running = false;
    }

}
