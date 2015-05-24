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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package no.ntnu.okse.protocol.amqp;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;
import org.apache.qpid.proton.engine.Collector;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by kradalby on 21/04/15.
 */
public class AMQProtocolServer extends AbstractProtocolServer {

    private static Logger log;
    private static Thread _serverThread;
    private static boolean _invoked;

    private static AMQProtocolServer _singleton;

    private static SubscriptionHandler sh;
    private static boolean shuttingdown = false;

    // Internal default values
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 5672;
    private static final String DEFAULT_USE_QUEUE = "true";
    private static final String DEFAULT_USE_SASL = "true";

    public boolean useQueue;
    protected boolean useSASL;

    private Driver driver;
    private static Properties config;

    private AMQProtocolServer(String host, Integer port) {
        this.init(host, port);
    }

    public static AMQProtocolServer getInstance() {
        // If not invoked, create an instance and inject as _singleton
        if (!_invoked) {
            // Read config
            config = Application.readConfigurationFiles();

            // Attempt to extract host and port from configuration file
            String configHost = config.getProperty("AMQP_HOST", DEFAULT_HOST);
            Integer configPort = null;
            try {
                configPort = Integer.parseInt(config.getProperty("AMQP_PORT", Integer.toString(DEFAULT_PORT)));
            } catch (NumberFormatException e) {
                log.error("Failed to parse AMQP Port, using default: " + DEFAULT_PORT);
            }

            // Update singleton
            _singleton = new AMQProtocolServer(configHost, configPort);
            _singleton.useQueue = Boolean.parseBoolean(config.getProperty("AMQP_USE_QUEUE", DEFAULT_USE_QUEUE));
            _singleton.useSASL = Boolean.parseBoolean(config.getProperty("AMQP_USE_SASL", DEFAULT_USE_SASL));
        }

        return _singleton;
    }

    public static AMQProtocolServer getInstance(String host, Integer port) {
        if (!_invoked) {
            // Read config
            config = Application.readConfigurationFiles();
            // Instantiate
            _singleton = new AMQProtocolServer(host, port);
        }
        return _singleton;
    }

    @Override
    protected void init(String host, Integer port) {
        log = Logger.getLogger(AMQProtocolServer.class.getName());
        protocolServerType = "AMQP";
        _invoked = true;
        // If we have host or port provided, set them, otherwise use internal defaults
        this.port = port == null ? DEFAULT_PORT : port;
        this.host = host == null ? DEFAULT_HOST : host;
    }

    @Override
    public void boot() {
        if (!_running) {
            _running = true;
            _serverThread = new Thread(() -> this.run());
            _serverThread.setName("AMQProtocolServer");
            _serverThread.start();
            log.info("AMQProtocolServer booted successfully");
        }

    }

    @Override
    public void run() {
        try {
            Collector collector = Collector.Factory.create();
            //Router router = new Router();
            //SubscriptionHandler sh = new SubscriptionHandler();
            this.sh = new SubscriptionHandler();
            SubscriptionService.getInstance().addSubscriptionChangeListener(sh);
            server = new AMQPServer(sh, false);
            driver = new Driver(collector, new Handshaker(),
                    new FlowController(1024), sh,
                    server);
            driver.listen(this.host, this.port);
            driver.run();
        } catch (IOException e) {
            totalErrors.incrementAndGet();
            log.error("I/O exception during accept(): " + e.getMessage());
        }
    }

    @Override
    public void stopServer() {
        log.info("Stopping AMQProtocolServer");
        shuttingdown = true;
        driver.stop();
        sh.unsubscribeAll();
        sh = null;
        _running = false;
        server = null;
        _singleton = null;
        _invoked = false;
        driver = null;
        log.info("AMQProtocolServer is stopped");
    }

    @Override
    public String getProtocolServerType() {
        return protocolServerType;
    }

    @Override
    public void sendMessage(Message message) {
        if (!message.getOriginProtocol().equals(protocolServerType) || message.getAttribute("duplicate") != null) {
            server.addMessageToQueue(message);
        }
    }

    public void incrementTotalMessagesSent() {
        totalMessagesSent.incrementAndGet();
    }

    public void incrementTotalMessagesReceived() {
        totalMessagesReceived.incrementAndGet();
    }

    public void incrementTotalRequests() {
        totalRequests.incrementAndGet();
    }

    public void incrementTotalBadRequest() {
        totalBadRequests.incrementAndGet();
    }

    public void incrementTotalErrors() {
        totalErrors.incrementAndGet();
    }

    public void decrementTotalErrors() {
        totalErrors.decrementAndGet();
    }

    private AMQPServer server;

    public Driver getDriver() {
        return driver;
    }

    public SubscriptionHandler getSubscriptionHandler() {
        return sh;
    }

    public boolean isShuttingDown() {
        return shuttingdown;
    }
}
