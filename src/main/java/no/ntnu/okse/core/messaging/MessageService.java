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

package no.ntnu.okse.core.messaging;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.AbstractCoreService;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.event.TopicChangeEvent;
import no.ntnu.okse.core.event.listeners.TopicChangeListener;
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.topic.TopicService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 4/17/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class MessageService extends AbstractCoreService implements TopicChangeListener {

    private static boolean _invoked = false;
    private static MessageService _singleton;
    private static Thread _serviceThread;
    private LinkedBlockingQueue<Message> queue;
    private ConcurrentHashMap<String, Message> latestMessages;
    private Properties config;

    /**
     * Private Constructor that recieves invocation from getInstance, enabling the singleton pattern for this class
     */
    protected MessageService() {
        super(MessageService.class.getName());
        if (_invoked) throw new IllegalStateException("Already invoked");
        init();
    }

    /**
     * Private initializer method that flags invocation state as true, and sets up message queue
     */
    protected void init() {
        config = Application.readConfigurationFiles();
        log.info("Initializing MessageService...");
        queue = new LinkedBlockingQueue<>();
        latestMessages = new ConcurrentHashMap<>();
        _invoked = true;
    }

    /**
     * The main invocation method of the MessageService. Instanciates a MessageService instance if needed,
     * and returns the active instance.
     *
     * @return The MessageService instance
     */
    public static MessageService getInstance() {
        if (!_invoked) _singleton = new MessageService();
        return _singleton;
    }

    /**
     * This method boots ans starts the thread running the MessageService
     */
    public void boot() {
        if (!_running) {
            log.info("Booting MessageService...");
            _serviceThread = new Thread(() -> {
                _running = true;
                _singleton.run();
            });
            _serviceThread.setName("MessageService");
            _serviceThread.start();
        }
    }

    /**
     * This method must contain the operations needed for ths class to register itself as a listener
     * to the different objects it wants to listen to. This method will be called after all Core Services have
     * been booted.
     */
    @Override
    public void registerListenerSupport() {
        // Register self as a listener for topic events
        TopicService.getInstance().addTopicChangeListener(this);
    }

    /**
     * This method should be called from within the run-scope of the serverThread thread instance
     */
    public void run() {
        if (_invoked) {
            log.info("MessageService booted successfully");
            while (_running) {
                try {
                    // Fetch the next job, will wait until a new message arrives
                    Message m = queue.take();
                    log.info("Received a message for distrubution: " + m);

                    // Do we have a system message?
                    if (m.isSystemMessage() && m.getTopic() == null) {

                        log.debug("Received message was a SystemMessage: " + m.getMessage());

                        // Check if we are to broadcast this system message
                        if (Application.BROADCAST_SYSTEM_MESSAGES_TO_SUBSCRIBERS) {

                            log.debug("System Message Broadcast set to TRUE, distributing system message...");

                            // Generate duplicate messages to all topics and iterate over them
                            generateMessageToAllTopics(m).stream().forEach(message -> {
                                // Fetch all protocol servers, and call sendMessage on each
                                CoreService.getInstance().getAllProtocolServers().forEach(s -> s.sendMessage(message));
                                // Flag the message as processed
                                message.setProcessed();
                            });

                            log.info("System message distribution completed");
                        }

                        // Set original message as processed.
                        m.setProcessed();

                        // Continue the run loop
                        continue;
                    }

                    HashSet<Topic> mappings = TopicService.getInstance().getAllMappingsAgainstTopic(m.getTopic());
                    if (mappings == null) {
                        log.debug("The Topic{" + m.getTopic() + "} has no mappings");
                    } else {
                        log.debug("Found the following mappings against Topic{" + m.getTopic() + "}: " + mappings);

                        generateMessageForAGivenTopicSet(m, mappings).forEach(duplicateMessage -> {
                            duplicateMessage.setAttribute("duplicate", m.getTopic());

                            if (m.getAttribute("duplicate") != null) {
                                if (!m.getTopic().equals(duplicateMessage.getAttribute("duplicate"))) {
                                    distributeMessage(duplicateMessage);
                                    log.debug("The message to Topic{" + duplicateMessage.getTopic() + "} was distributed");
                                } else {
                                    log.debug("The message to Topic{" + duplicateMessage.getTopic() + "} is a duplicate against Topic{" + m.getTopic() + "}, and will not be distributed");
                                }
                            } else if (TopicService.getInstance().topicExists(m.getTopic())) {
                                distributeMessage(duplicateMessage);
                                log.debug("The message to Topic{" + duplicateMessage.getTopic() + "} was distributed");
                            } else {
                                log.debug("The message was not sent. Most likely since the Topic{" + duplicateMessage.getTopic() + "} does not exist");
                            }

                        });
                    }


                    // Tell the ExecutorService to execute the following job
                    CoreService.getInstance().execute(() -> {
                        // Add message to latestMessages cache
                        latestMessages.put(m.getTopic(), m);
                        // Fetch all registered protocol servers, and call the sendMessage() method on them
                        CoreService.getInstance().getAllProtocolServers().forEach(p -> {
                            // Fire the sendMessage on all servers
                            p.sendMessage(m);
                        });
                        // Set the message as processed, and store the completion time
                        LocalDateTime completedAt = m.setProcessed();
                        log.info("Message successfully distributed: " + m + " (Finished at: " + completedAt + ")");
                    });

                } catch (InterruptedException e) {
                    log.error("Interrupted while attempting to fetch next Message from queue");
                }
            }
            log.debug("MessageService serverThread exited main run loop");
        } else {
            log.error("Run method called before invocation of the MessageService getInstance method");
        }
    }

    /**
     * Graceful shutdown method
     */
    @Override
    public void stop() {
        _running = false;
        // Create a new message with topic = null, hence it will reside upon the config flag for system messages
        // in the web admin if the message is distributed to all topics or just performed as a no-op.
        Message m = new Message("The broker is shutting down.", null, null, Application.OKSE_SYSTEM_NAME);
        // Set it to system message
        m.setSystemMessage(true);

        try {
            queue.put(m);
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to inject shutdown message to queue");
        }
    }

    /* Begin Public API */

    /**
     * Adds a Message object into the message queue for distribution
     *
     * @param m The message object to be distributed
     */
    public void distributeMessage(Message m) {
        try {
            this.queue.put(m);
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to inject message into queue");
        }
    }

    /**
     * Retrieves the latest message sent on a specific topic
     *
     * @param topic The topic to retrieve the latest message for
     * @return The message object for the specified topic, null if there has not been any messages yet
     */
    public Message getLatestMessage(String topic) {
        if (latestMessages.containsKey(topic)) return latestMessages.get(topic);

        return null;
    }

    /**
     * Check if the OKSE system is currently caching messages
     *
     * @return True if this setting is set to true, false otherwise
     */
    public boolean isCachingMessages() {
        return Application.CACHE_MESSAGES;
    }


    /**
     * Takes in a message and a HashSet of topics and creates duplicate messages of
     * the origin message, and returns it as a list.
     *
     * @param m      The origin message
     * @param topics A HashSet containing all the topics that the message topic is mapped against
     * @return A containing the new message objects, to dispatch into the queue.
     */
    public List<Message> generateMessageForAGivenTopicSet(Message m, HashSet<Topic> topics) {
        ArrayList<Message> collector = new ArrayList<>();

        topics.stream()
                .forEach(t -> {
                    Message msg = new Message(m.getMessage(), t.getFullTopicString(), m.getPublisher(), m.getOriginProtocol());
                    collector.add(msg);
                });

        return collector;
    }


    /* ----------------------------------------------------------------------------------------------- */

    /* Private helper methods */

    /**
     * Private helper method to duplicate an incoming message to be
     *
     * @param m The message to be duplicated to all topics
     * @return A HashSet of the generated messages
     */
    private HashSet<Message> generateMessageToAllTopics(Message m) {
        // Initialize the collector
        HashSet<Message> generated = new HashSet<>();
        // Iterate over all topics and generate individual messages per topic
        TopicService.getInstance().getAllTopics().stream().forEach(t -> {
            // Create the message wrapper
            Message msg = new Message(m.getMessage(), t.getFullTopicString(), m.getPublisher(), m.getOriginProtocol());
            // Flag the generated message the same as the originating message
            msg.setSystemMessage(m.isSystemMessage());
            // Add the message to the collector
            generated.add(msg);
        });

        return generated;
    }

    /* Begin observation methods */

    @Override
    public void topicChanged(TopicChangeEvent event) {
        if (event.getType().equals(TopicChangeEvent.Type.DELETE)) {
            // Fetch the raw topic string from the deleted topic
            String rawTopicString = event.getData().getFullTopicString();

            // If we have messages in cache for the topic in question, remove it to remove any remaining
            // reference to the Topic node, so the garbage collector can do its job.
            if (latestMessages.containsKey(rawTopicString)) {
                latestMessages.remove(rawTopicString);
                log.debug("Removed a message from cache due to its topic being deleted");
            }
        }
    }

    /* End observation methods */
}
