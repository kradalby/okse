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

import no.ntnu.okse.Application;
import no.ntnu.okse.core.AbstractCoreService;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.event.TopicChangeEvent;
import no.ntnu.okse.core.event.listeners.TopicChangeListener;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 4/11/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class TopicService extends AbstractCoreService {

    private static boolean _invoked = false;
    private static TopicService _singleton = null;
    private static Thread _serviceThread;
    private LinkedBlockingQueue<TopicTask> queue;
    private Properties config;
    private ConcurrentHashMap<String, Topic> allTopics;
    private ConcurrentHashSet<TopicChangeListener> _listeners;
    private ConcurrentHashMap<String, HashSet<String>> mappings;

    /**
     * Private constructor that passes this classname to superclass log instance. Uses getInstance to instanciate.
     */
    protected TopicService() {
        super(TopicService.class.getName());
        if (_invoked) throw new IllegalStateException("Already invoked");
        init();
    }

    /**
     * The getInstance method provides the public accessor for the TopicService instance, adhering to the singleton pattern.
     *
     * @return The TopicService instance.
     */
    public static TopicService getInstance() {
        if (!_invoked) _singleton = new TopicService();
        return _singleton;
    }

    /**
     * Private initialization method. All set-up operations are to be performed here.
     */
    protected void init() {
        config = Application.readConfigurationFiles();
        log.info("Initializing TopicService...");
        queue = new LinkedBlockingQueue<>();
        allTopics = new ConcurrentHashMap<>();
        _listeners = new ConcurrentHashSet<>();
        mappings = new ConcurrentHashMap<>();
        _invoked = true;

        log.info("Initializing topic mapping from configuration file");
        if (config.containsKey("TOPIC_MAPPING")) {

            Properties topicMapping = Utilities.readConfigurationFromFile(config.getProperty("TOPIC_MAPPING"));

            if (topicMapping == null) {
                log.error("Failed to load topic mapping from config file");
                return;
            }

            log.debug("Topic mapping properties: " + topicMapping.stringPropertyNames());
            for (String toMapFrom : topicMapping.stringPropertyNames()) {
                addTopic(toMapFrom);

                String[] toMapToList = topicMapping.getProperty(toMapFrom).split(",");

                for (String toMapTo : toMapToList) {
                    addTopic(toMapTo);
                    addMappingBetweenTopics(toMapFrom, toMapTo);
                }
            }
            log.debug("Predefined mappings are: " + mappings);
            log.info("Topic mapping configuration done");
        }
    }

    /**
     * This method boots the TopicService and spawns a separate thread for it.
     */
    public void boot() {
        if (!_running) {
            log.info("Booting TopicService...");
            _serviceThread = new Thread(() -> {
                _running = true;
                _singleton.run();
            });
            _serviceThread.setName("TopicService");
            _serviceThread.start();
        }
    }

    /**
     * This method must contain the operations needed for the subclass to register itself as a listener
     * to the different objects it wants to listen to. This method will be called after all Core Services have
     * been booted.
     */
    @Override
    public void registerListenerSupport() {
        // TODO: Register self as listener to stuff
    }

    /**
     * This method Stops the TopicService
     *
     * @throws InterruptedException An exception that might occur if thread is interrupted while waiting for put
     *                              command thread lock to open up.
     */
    public void stop() {
        _running = false;
        removeAllListeners();
        Runnable job = () -> log.info("Stopping TopicService...");
        try {
            _singleton.getQueue().put(new TopicTask(TopicTask.Type.SHUTDOWN, job));
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to inject shutdown event to queue");
        }
    }

    /**
     * This method is called after the singleton has been invoked and booted
     */
    public void run() {
        log.info("TopicService booted successfully");
        while (_running) {
            try {
                TopicTask task = queue.take();
                log.debug(task.getType() + " job received, executing task...");
                task.run();
            } catch (InterruptedException e) {
                log.warn("Interrupt caught, consider sending a No-Op TopicTask to the queue to awaken the thread.");
            }
        }
    }

    /**
     * Fetches the task queue.
     *
     * @return A LinkedBlockingQueue that accepts TopicTask objects to be performed on the TopicService thread.
     */
    public LinkedBlockingQueue<TopicTask> getQueue() {
        return queue;
    }

    /**
     * Retrieves the amount of topics currently registered.
     *
     * @return An integer representing the total number of registered topics.
     */
    public Integer getTotalNumberOfTopics() {
        return allTopics.size();
    }

    /**
     * Get all the root topic nodes as a shallow copy of the internal topic service hash set.
     *
     * @return A HashSet of all the root topic nodes.
     */
    public HashSet<Topic> getAllRootTopics() {
        HashSet<Topic> collector = new HashSet<Topic>();
        // Iterate over the key value pairs and add topic to roots if it is indeed a root topic node
        allTopics.forEach((k, t) -> {
            if (t.isRoot()) collector.add(t);
        });

        return collector;
    }

    /**
     * Get all topic nodes.
     *
     * @return A HashSet of all topic nodes.
     */
    public HashSet<Topic> getAllTopics() {
        HashSet<Topic> collector = new HashSet<>();
        allTopics.forEach((s, t) -> collector.add(t));

        return collector;
    }

    /**
     * Get all leaf nodes as a shallow copy of the internal topic service hash set.
     *
     * @return A HashSet of all leaf topic nodes.
     */
    public HashSet<Topic> getAllLeafTopics() {
        HashSet<Topic> collector = new HashSet<>();
        // Iterate over the key value pairs and add the topic if it is indeed a leaf topic node.
        allTopics.forEach((k, t) -> {
            if (t.isLeaf()) collector.add(t);
        });

        return collector;
    }

    /**
     * Attempts to fetch a topic based on a raw topic string.
     *
     * @param rawTopicString The topic string to use in search.
     * @return A Topic if we found one, null otherwise.
     */
    public Topic getTopic(String rawTopicString) {
        if (allTopics.containsKey(rawTopicString)) return allTopics.get(rawTopicString);
        return null;
    }

    /**
     * Attempts to fetch a topic based on the ID
     *
     * @param id The topic ID to use in the search
     * @return A topic if found, null otherwise.
     */
    public Topic getTopicByID(String id) {
        List<Topic> result = new ArrayList<>();

        allTopics.forEach((k, t) -> {
            if (t.getTopicID().equals(id)) result.add(t);
        });

        if (result.size() > 1) {
            log.warn("Found multiple topics with the same hash/ID.");
            return null;
        } else if (result.size() != 1) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Get all mappings registered mappings in the system as a shallow copy.
     *
     * @return A HashMap of all the registered mappings
     */
    public HashMap<String, HashSet<String>> getAllMappings() {
        HashMap<String, HashSet<String>> collector = new HashMap<>();

        mappings.forEach((k, v) -> collector.put(k, v));

        return collector;
    }

    /**
     * Attempts to fetch all mappings for a topic, based on the raw topic string
     *
     * @param rawTopicString The string to identify the topic
     * @return A HashSet containing all the found topics, null otherwise
     */
    public HashSet<Topic> getAllMappingsAgainstTopic(String rawTopicString) {
        HashSet<Topic> result = new HashSet<>();

        if (mappings.containsKey(rawTopicString)) {
            mappings.get(rawTopicString).forEach(topicToMapAgainst -> result.add(getTopic(topicToMapAgainst)));
        }

        return (result.size() > 0) ? result : null;
    }

    /* Begin local API */

    /**
     * This method should never be called from outside of a TopicTask processed through the TopicService queue.
     *
     * @param t The topic to be added.
     */
    public void addTopicLocal(Topic t) {
        this.allTopics.put(t.getFullTopicString(), t);
        log.info("Added new topic: " + t);
        fireTopicChangeEvent(t, TopicChangeEvent.Type.NEW);
    }

    /**
     * Local removal of a topic from the allTopics hashmap. Do not call outside a TopicTask job instance,
     * using the deleteTopic public method, as that method will also identify and remove connected children nodes.
     *
     * @param t The topic to be removed.
     */
    private void deleteTopicLocal(Topic t) {
        if (allTopics.containsValue(t)) {
            allTopics.remove(t.getFullTopicString());
            log.info("Deleted Topic: " + t);
            fireTopicChangeEvent(t, TopicChangeEvent.Type.DELETE);
        }
    }

    /* End local API */

    /**
     * Checks to see if a topic node exists in the TopicService.
     *
     * @param topic The topic node instance to check.
     * @return true if it exists, false otherwise.
     */
    public boolean topicExists(Topic topic) {
        return allTopics.containsValue(topic);
    }

    /**
     * Checks to see if a topic node exists based on the raw topic string.
     *
     * @param topic The raw topic string you wish to check existance for.
     * @return true if it exists, false otherwise.
     */
    public boolean topicExists(String topic) {
        return this.allTopics.containsKey(topic);
    }

    /**
     * Generate Topic instances and connect the nodes properly, from a raw topic string.
     * This method will quasi-recursively analyze the suffix part of the topic string,
     * in an attempt to locate a Topic node already existing in the system. If none are found, all
     * nodes needed to represent the topic tree are returned in the hashSet. If a higher level Topic node
     * is found, only the non-existing nodes needed are created, linked together, and connected to the existing
     * Topic node through the parent field.
     *
     * @param topic The full raw topic string you want to generate needed Topic nodes from
     * @return An empty set if we do not need create new nodes. A set of newly instanciated nodes that can be added
     * to the containers from the invoking method.
     */
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
        topicParts.remove(topicParts.size() - 1);

        // While there are more topic part names left
        while (!topicParts.isEmpty()) {
            // Concatenate the remaining parts
            String activeRawTopicString = String.join("/", topicParts);

            // Does this topic level exist?
            if (allTopics.containsKey(activeRawTopicString)) {
                log.debug("Found match for existing topic, setting parent of currentTopic to " + allTopics.get(activeRawTopicString));
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
                topicParts.remove(topicParts.size() - 1);
                // Set the parentTopic as our new currentTopic
                currentTopic = parentTopic;
            }
        }

        // Return what we have created for registration in the master containers
        return collector;
    }

    /**
     * Removes a mapping by a mapping key
     *
     * @param mapping The mapping represented as a string
     */
    public void deleteMapping(String mapping) {
        if (mappings.containsKey(mapping)) {
            HashSet<String> mappedAgainst = mappings.remove(mapping);
            log.info("Removed the mappings for Topic{" + mapping + "}");
        } else {
            log.warn("Attempt to remove a mapping that did in fact not exist ");
        }
    }

    /**
     * Removes a Topic given by a full raw topic string. Also locates all potential children from this topic
     * and removes them aswell.
     *
     * @param topic The full raw topic string representing the topic to be deleted
     */
    public void deleteTopic(String topic) {
        // If the topic actually exists
        if (allTopics.containsKey(topic)) {
            // Create a delete job
            Runnable job = () -> {
                // Fetch the Topic object
                Topic t = getTopic(topic);
                // Retrieve a set of all its children
                HashSet<Topic> children = TopicTools.getAllChildrenFromNode(t);
                // Remove all the children
                children.forEach(c -> deleteTopicLocal(c));
                // Delete the topic itself
                deleteTopicLocal(t);
            };

            // Create the TopicTask job wrapper
            TopicTask task = new TopicTask(TopicTask.Type.DELETE_TOPIC, job);

            // Put the task into the queue
            try {
                getQueue().put(task);
            } catch (InterruptedException e) {
                log.error("Interrupted while attempting to put DeleteTopic task to task queue.");
            }
        } else {
            log.warn("Attempt to remove a topic that did in fact not exist.");
        }
    }

    /**
     * Accepts two topic string and creates this topics. It also adds it to the mapping HashMap
     *
     * @param fromTopic Topic to map from
     * @param toTopic   Topic to map to
     */
    public void addMappingBetweenTopics(String fromTopic, String toTopic) {
        addTopic(fromTopic);
        addTopic(toTopic);

        if (!mappings.containsKey(fromTopic)) {
            mappings.put(fromTopic, new HashSet<String>(Arrays.asList(toTopic)));
        } else {
            mappings.get(fromTopic).add(toTopic);
        }
        log.debug("Added mapping between Topic{" + fromTopic + "} and Topic{" + toTopic + "}");
    }

    /**
     * Add a topic to the TopicService
     *
     * @param topic The raw topic string that should be added. E.g "no/okse/current"
     */
    public void addTopic(String topic) {
        // Check that the topic does not already exist
        if (!allTopics.containsKey(topic)) {
            // Create a new job
            Runnable job = () -> {
                // Generate topic nodes based on the raw topic string, and add them all
                HashSet<Topic> topicNodes = generateTopicNodesFromRawTopicString(topic);
                topicNodes.forEach(t -> addTopicLocal(t));
            };

            // Initialize the TopicTask object with proper type and the job itself
            TopicTask task = new TopicTask(TopicTask.Type.NEW_TOPIC, job);

            try {
                // Put the task into the task queue
                getQueue().put(task);
            } catch (InterruptedException e) {
                log.error("Interrupted while attempting to put AddTopic task to task queue.");
            }
        } else {
            log.debug("Attempt to add a topic from raw topic string that already exists (" + topic + ")");
        }
    }

    /* Begin listener support */

    /**
     * Purges all registered listener objects from the TopicService
     */
    public synchronized void removeAllListeners() {
        _listeners.clear();
    }

    /**
     * This method allows registration for TopicChange listeners.
     *
     * @param listener An object implementing the TopicChangeListener interface
     */
    public synchronized void addTopicChangeListener(TopicChangeListener listener) {
        this._listeners.add(listener);
    }

    /**
     * This method allows removal of TopicChange listeners.
     *
     * @param listener The object implementing TopigChangeListener interface that is to be removed.
     */
    public synchronized void removeTopicChangeListener(TopicChangeListener listener) {
        if (this._listeners.contains(listener)) this._listeners.remove(listener);
    }

    /**
     * Public helper method to fire a topic change event on all listeners
     *
     * @param topic The topic that has had an event
     * @param type  The type of topic event that occured
     */
    public void fireTopicChangeEvent(Topic topic, TopicChangeEvent.Type type) {
        TopicChangeEvent topicEvent = new TopicChangeEvent(type, topic);
        log.debug("Firing topicchange event of type " + type + " on topic " + topic);
        this._listeners.stream().forEach(t -> t.topicChanged(topicEvent));
    }

    /* End listener support */
}
