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

import no.ntnu.okse.core.AbstractCoreService;
import no.ntnu.okse.core.CoreService;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Aleksander Skraastad (myth) on 4/17/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class MessageService extends AbstractCoreService {

    private static boolean _invoked = false;
    private static MessageService _singleton;
    private static Thread _serverThread;
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
     *
     */
    public void boot() {
        if (!_running) {
            log.info("Booting MessageService...");
            _serverThread = new Thread(() -> {
                _running = true;
                _singleton.run();
            });
            _serverThread.setName("MessageService");
            _serverThread.start();
        }
    }

    public void run() {
        if (_invoked) {
            log.info("MessageService booted successfully");
            while (_running) {
                try {
                    Message m = queue.take();
                    log.info("Recieved a message for distrubution: " + m);
                    CoreService.getInstance().getAllProtocolServers().forEach(p -> p.sendMessage(m.getMessage()));
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

    }

}
