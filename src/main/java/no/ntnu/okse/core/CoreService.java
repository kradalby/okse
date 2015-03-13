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

import no.ntnu.okse.protocol.WSNotificationServer;
import org.apache.log4j.Logger;

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
    private WSNotificationServer wsnServer;

    /**
     * Constructs the CoreService thread, initiates the logger and eventQueue.
     */
    public CoreService() {
        super("CoreService");
        running = false;
        log = Logger.getLogger(CoreService.class.getName());
        eventQueue = new LinkedBlockingQueue();
        executor = Executors.newFixedThreadPool(10);
        wsnServer = WSNotificationServer.getInstance();
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
     * Fetches the WSNotificationServer instance
     * <p>
     * @return: The WSNotificationServer instance
     */
    public WSNotificationServer getWsnServer() {
        return this.wsnServer;
    }


    /**
     * Starts the main loop of the CoreService thread.
     */
    @Override
    public void run() {
        running = true;
        log.info("CoreService started.");
        log.info("Attempting to boot WSNServer");
        this.wsnServer.boot();
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
