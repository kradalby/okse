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
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.topic.TopicService;
import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 4/17/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class MessageService extends AbstractCoreService {

    private static boolean _invoked = false;
    private static MessageService _singleton;
    private static Thread _serviceThread;
    private LinkedBlockingQueue<Message> queue;

    /**
     * Private Constructor that recieves invocation from getInstance, enabling the singleton pattern for this class
     */
    private MessageService() {
        super(MessageService.class.getName());
        init();
    }

    /**
     * Private initializer method that flags invocation state as true, and sets up message queue
     */
    protected void init() {
        log.info("Initializing MessageService...");
        queue = new LinkedBlockingQueue<>();
        _invoked = true;
    }

    /**
     * The main invocation method of the MessageService. Instanciates a MessageService instance if needed,
     * and returns the active instance.
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
     * This method should be called from within the run-scope of the serverThread thread instance
     */
    public void run() {
        if (_invoked) {
            log.info("MessageService booted successfully");
            while (_running) {
                try {
                    // Fetch the next job, will wait until a new message arrives
                    Message m = queue.take();
                    log.info("Recieved a message for distrubution: " + m);

                    // Do we have a system message?
                    if (m.isSystemMessage() && m.getTopic() == null) {

                        log.debug("Recieved message was a SystemMessage: " + m.getMessage());

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

                    // Tell the ExecutorService to execute the following job
                    CoreService.getInstance().execute(() -> {
                        // Fetch all registered protocol servers, and call the sendMessage() method on them
                        CoreService.getInstance().getAllProtocolServers().forEach(p -> p.sendMessage(m));
                        // Set the message as processed, and store the completion time
                        LocalDateTime completedAt = m.setProcessed();
                        log.info("Message successfully distributed: " + m + " (" + completedAt + ")");
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
        Message m = new Message("The broker is shutting down.", null, null);
        // Set it to system message
        m.setSystemMessage(true);

        try {
            queue.put(m);
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to inject shutdown message to queue");
        }
    }

    /* ----------------------------------------------------------------------------------------------- */

    /* Private helper methods */

    /**
     * Private helper method to duplicate an incoming message to be
     * @param m The message to be duplicated to all topics
     * @return A HashSet of the generated messages
     */
    private HashSet<Message> generateMessageToAllTopics(Message m) {
        // Initialize the collector
        HashSet<Message> generated = new HashSet<>();
        // Iterate over all topics and generate individual messages per topic
        TopicService.getInstance().getAllTopics().stream().forEach(t -> {
            // Create the message wrapper
            Message msg = new Message(m.getMessage(), t, m.getPublisher());
            // Flag the generated message the same as the originating message
            msg.setSystemMessage(m.isSystemMessage());
            // Add the message to the collector
            generated.add(msg);
        });

        return generated;
    }

}
