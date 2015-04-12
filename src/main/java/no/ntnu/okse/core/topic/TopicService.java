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

import no.ntnu.okse.core.event.TopicChangeEvent;
import no.ntnu.okse.core.event.listeners.TopicChangeListener;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 4/11/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class TopicService implements Runnable {

    private static Logger log;
    private static boolean _running = false;
    private static boolean _invoked = false;
    private static TopicService _singleton = null;
    private static Thread _serverThread;
    private LinkedBlockingQueue<TopicTask> queue;
    private HashSet<Topic> rootTopics, leafTopics;
    private HashMap<String, Topic> allTopics;
    private HashSet<TopicChangeListener> _listeners;

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
        allTopics = new HashMap<>();
        leafTopics = new HashSet<>();
        _listeners = new HashSet<>();
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
            _serverThread.start();
            log.info("TopicService started");
        }
    }

    /**
     * This method Stops the TopicService
     * @throws InterruptedException An exception that might occur if thread is interrupted while waiting for put
     * command thread lock to open up.
     */
    public void stop() throws InterruptedException {
        _running = false;
        _singleton.getQueue().put(() -> log.info("Stopping TopicService..."));
        _serverThread = null;
    }

    /**
     * This method is called after the singleton has been invoked and booted
     */
    public void run() {
        while (_running) {
            try {
                TopicTask task = queue.take();
                log.debug("Task recieved, executing task...");
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
    public HashSet<Topic> getAllRootTopics() {
        return (HashSet<Topic>) rootTopics.clone();
    }

    /**
     * Get all topic nodes.
     * @return A HashSet of all topic nodes.
     */
    public HashSet<Topic> getAllTopics() {
        HashSet<Topic> collector = new HashSet<>();
        allTopics.forEach((s, t) -> collector.add(t));

        return collector;
    }

    /**
     * Get all leaf nodes as a shallow copy of the internal topic service hash set.
     * @return A HashSet of all leaf topic nodes.
     */
    public HashSet<Topic> getAllLeafTopics() {
        return (HashSet<Topic>) leafTopics.clone();
    }

    /**
     * This method allows registration for TopicChange listeners.
     * @param listener An object implementing the TopicChangeListener interface
     */
    public synchronized void addTopicChangeListener(TopicChangeListener listener) {
        this._listeners.add(listener);
    }

    /**
     * This method allows removal of TopicChange listeners.
     * @param listener The object implementing TopigChangeListener interface that is to be removed.
     */
    public synchronized void removeTopicChangeListener(TopicChangeListener listener) {
        if (this._listeners.contains(listener)) this._listeners.remove(listener);
    }

    /**
     * This method should never be called from outside of a TopicTask processed through the TopicService queue.
     * @param t The topic to be added.
     */
    public void addTopicLocal(Topic t) {
        this.allTopics.put(t.getFullTopicString(), t);
        if (t.isRoot()) this.rootTopics.add(t);
        if (t.isLeaf()) this.leafTopics.add(t);
        log.info("Added new topic: " + t);
        fireTopicChangeEvent(t, TopicChangeEvent.Type.NEW);
    }

    /**
     * Checks to see if a topic node exists in the TopicService.
     * @param topic The topic node instance to check.
     * @return true if it exists, false otherwise.
     */
    public boolean topicExists(Topic topic) {
        return allTopics.containsValue(topic);
    }

    /**
     * Checks to see if a topic node exists based on the raw topic string.
     * @param topic The raw topic string you wish to check existance for.
     * @return true if it exists, false otherwise.
     */
    public boolean topicExists(String topic) {
        return this.allTopics.containsKey(topic);
    }

    public HashSet<Topic> generateTopicNodesFromRawTopicString(String topic) {

        HashSet<Topic> collector = new HashSet<>();

        // If the topic already exists just return the collector
        if (allTopics.containsKey(topic)) return collector;

        // Split our topic string into parts
        ArrayList<String> topicParts = new ArrayList(Arrays.asList(topic.split("/")));

        // Generate the leaf node, set name and type and add to collector
        Topic currentTopic = new Topic();
        currentTopic.setName(topicParts.get(topicParts.size() - 1));
        currentTopic.setType("Default");
        collector.add(currentTopic);
        topicParts.remove(currentTopic.getName());

        // While there
        while (topicParts.size() > 0) {
            // Concatenate the remaining parts
            String activeRawTopicString = String.join("/", topicParts);

            // Does this topic level exist?
            if (allTopics.containsKey(activeRawTopicString)) {
                currentTopic.setParent(allTopics.get(activeRawTopicString));
                break;
            } else {
                // Create a new topic node and set proper values
                Topic parentTopic = new Topic();
                parentTopic.setName(topicParts.get(topicParts.size() - 1));
                parentTopic.setType("Default");

                // Set the parent of our current topic to the new parentTopic
                currentTopic.setParent(parentTopic);
                // Add the new parent to the collector
                collector.add(parentTopic);
                // Remove the most recently used topic sub part
                topicParts.remove(parentTopic.getName());
                // Set the parentTopic as our new currentTopic
                currentTopic = parentTopic;
            }
        }

        // Return what we have created for registration in the master containers
        return collector;
    }

    /**
     * Add a topic to the TopicService
     * @param topic The raw topic string that should be added. E.g "no/okse/current"
     */
    public synchronized void addTopic(String topic) {
        // Check that the topic does not already exist
        if (!allTopics.containsKey(topic)) {
            // Create a new topic task
            TopicTask task = new TopicTask() {
                @Override
                public void performJob() {
                    // Generate topic nodes based on the raw topic string, and add them all
                    HashSet<Topic> topicNodes = generateTopicNodesFromRawTopicString(topic);
                    topicNodes.forEach(t -> addTopicLocal(t));
                }
            };
            try {
                // Put the task into the task queue
                getQueue().put(task);
            } catch (InterruptedException e) {
                log.error("Interrupted while attempting to add Topic.");
            }
        }
    }

    /**
     * Public helper method to fire a topic change event on all listeners
     * @param topic The topic that has had an event
     * @param type The type of topic event that occured
     */
    public void fireTopicChangeEvent(Topic topic, TopicChangeEvent.Type type) {
        TopicChangeEvent topicEvent = new TopicChangeEvent(type, topic);
        log.debug("Firing topicchange event of type " + type + " on topic " + topic);
        this._listeners.stream().forEach(t -> t.topicChanged(topicEvent));
    }
}
