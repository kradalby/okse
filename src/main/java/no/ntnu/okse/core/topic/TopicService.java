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

package no.ntnu.okse.core.topic;

import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 4/11/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class TopicService {

    private static Logger log;
    private static boolean _running = false;
    private static boolean _invoked = false;
    private static TopicService _singleton = null;
    private static Thread _serverThread;
    private LinkedBlockingQueue<TopicTask> queue;
    private HashSet<Topic> allTopics, rootTopics, leafTopics;

    private TopicService() {
        init();
    }

    /**
     * The getInstance method provides the public accessor for the TopicService instance, adhering to the singleton pattern.
     * @return The TopicService instance.
     */
    public static TopicService getInstance() {
        if (!_invoked) _singleton = new TopicService();
        return _singleton;
    }

    /**
     * Private initialization method. All set-up operations are to be performed here.
     */
    private void init() {
        log = Logger.getLogger(TopicService.class.getName());
        log.info("Initializing TopicService");
        queue = new LinkedBlockingQueue<>();
        rootTopics = new HashSet<>();
        allTopics = new HashSet<>();
        _invoked = true;
        // TODO: Read some shit from config or database to initialize pre-set topics with attributes. Or should
        // TODO: that maybe be done in the Subscriber objets? Who knows.
    }

    /**
     * This method boots the TopicService and spawns a separate thread for it.
     */
    public void boot() {

        if (!_invoked) getInstance();

        if (!_running) {
            log.info("Booting TopicService...");
            _serverThread = new Thread(() -> {
                _running = true;
                _singleton.run();
            });
            _serverThread.setName("TopicService");
            log.info("TopicService started");
        }
    }

    /**
     * This method Stops the TopicService
     */
    public void stop() throws InterruptedException {
        _running = false;
        _singleton.getQueue().put(() -> log.info("Stopping TopicService..."));
        _serverThread = null;
    }

    /**
     * This method is called after the singleton has been invoked and booted
     */
    private void run() {
        while (_running) {
            try {
                TopicTask task = queue.take();
                log.info("Task recieved, executing task...");
                task.performJob();
            } catch (InterruptedException e) {
                log.warn("Interrupt caught, consider sending a No-Op TopicTask to the queue to awaken the thread.");
            }
        }
    }

    /**
     * Fetches the task queue.
     * @return A LinkedBlockingQueue that accepts TopicTask objects to be performed on the TopicService thread.
     */
    public LinkedBlockingQueue<TopicTask> getQueue() {
        return queue;
    }

    /**
     * Get all the root topic nodes as a shallow copy of the internal topic service hash set.
     * @return A HashSet of all the root topic nodes.
     */
    public HashSet<Topic> getRootTopics() {
        return (HashSet<Topic>) rootTopics.clone();
    }

    /**
     * Get all topic nodes as a shallow copy of the internal topic service hash set.
     * @return A HashSet of all topic nodes.
     */
    public HashSet<Topic> getAllTopics() {
        return (HashSet<Topic>) allTopics.clone();
    }

    /**
     * Get all leaf nodes as a shallow copy of the internal topic service hash set.
     * @return A HashSet of all leaf topic nodes.
     */
    public HashSet<Topic> getAllLeafTopics() {
        return (HashSet<Topic>) leafTopics.clone();
    }
}
