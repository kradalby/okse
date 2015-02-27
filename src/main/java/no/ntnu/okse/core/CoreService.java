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

import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 2/25/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class CoreService extends Thread {

    private boolean running;
    private static Logger log;
    private LinkedBlockingQueue eventQueue;
    private TaskRunner taskRunner;
    private Integer eventCount;

    /**
     * Constructs the CoreService thread, initiates the logger,
     * event queue and task runner.
     */
    public CoreService() {
        running = false;
        log = Logger.getLogger(CoreService.class.getName());
        eventQueue = new LinkedBlockingQueue();
        taskRunner = new TaskRunner();
        eventCount = new Integer(0);
    }

    /**
     * Fetches the eventQueue.
     *
     * @return The eventQueue list
     */
    public LinkedBlockingQueue getEventQueue() {
        return eventQueue;
    }

    /**
     * Fetches the taskRunner
     *
     * @return The taskRunner object
     */
    public TaskRunner getTaskRunner() {
        return taskRunner;
    }

    /**
     * Starts the main loop of the CoreService thread.
     */
    @Override
    public void run() {
        running = true;
        Thread.currentThread().setName("Thread: CoreService");
        log.info("CoreService started.");
        while (running) {
            try {
                eventQueue.take();
                eventCount++;
                log.info("Consumed an event.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("CoreService stopped.");
    }

    /**
     *
     * @return The number of events processed during the lifetime of the CoreService instance.
     */
    public Integer getEventCount() {
        return eventCount;
    }

    /**
     * Stops execution of the CoreService thread.
     */
    public void stopThread() {
        running = false;
    }

}
