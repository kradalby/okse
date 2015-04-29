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

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.protocol.AbstractProtocolServer;

import org.apache.log4j.Logger;
import org.apache.qpid.proton.engine.Collector;

import java.io.IOException;

/**
 * Created by kradalby on 21/04/15.
 */
public class AMQProtocolServer extends AbstractProtocolServer {

    private static Logger log;
    private static Thread _serverThread;
    private static boolean _invoked;

    private static final String configurationFile = "";

    private static AMQProtocolServer _singleton;
    private static String hostname = "127.0.0.1";

    private Driver driver;
    private AMQPServer server;

    private AMQProtocolServer(Integer port) {this.init(port);}


    public static AMQProtocolServer getInstance() {
        if (!_invoked) _singleton = new AMQProtocolServer(5672);
        return _singleton;
    }



    @Override
    protected void init(Integer port) {
        log = Logger.getLogger(AMQProtocolServer.class.getName());
        protocolServerType = "AMQP";
        _invoked = true;
        this.port = port;


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
            SubscriptionHandler sh = new SubscriptionHandler();
            server = new AMQPServer(sh, false);
            driver = new Driver(collector, new Handshaker(),
                new FlowController(1024), sh,
                server);
            driver.listen(hostname, port);
            driver.run();
        } catch (IOException e) {
            totalErrors++;
            log.error("I/O exception during accept(): " + e.getMessage());
        }
    }

    @Override
    public void stopServer() {
        _running = false;
        //TODO: implement driver.stop()

    }

    @Override
    public String getProtocolServerType() {
        return protocolServerType;
    }

    @Override
    public void sendMessage(Message message) {
        server.addMessageToQueue(message);
    }

    public void incrementTotalMessages() {
        totalMessages++;
    }
}
