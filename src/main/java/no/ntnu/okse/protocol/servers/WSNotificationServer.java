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

package no.ntnu.okse.protocol.servers;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;

/**
 * Created by Aleksander Skraastad (myth) on 3/12/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNotificationServer extends AbstractProtocolServer {

    private static final String configurationFile = "/config/wsnserver.xml";

    private static WSNotificationServer _singleton;

    private Server _server;
    private final ArrayList<Connector> _connectors = new ArrayList();
    private HttpClient _client;

    /**
     * Empty constructor, uses defaults from jetty configuration file for WSNServer
     */
    private WSNotificationServer() {
        this.init(null);
        this._invoked = true;
    }

    /**
     * Constructor that takes in a port that the WSNServer jetty instance should
     * listen to.
     * <p>
     * @param port: An integer representing the port the WSNServer should bind to.
     */
    private WSNotificationServer(Integer port) {
        this.init(port);
    }

    /**
     * Factory method providing an instance of WSNotificationServer, adhering to the
     * singleton pattern. (Using default port from config file.)
     * <p>
     * @return: The WSNotification instance.
     */
    public static WSNotificationServer getInstance() {
        if (WSNotificationServer._invoked) return _singleton;
        else {
            _singleton = new WSNotificationServer();
            WSNotificationServer._invoked = true;

            return _singleton;
        }
    }

    /**
     * Factory method providing an instance of WSNotificationServer, adhering to the
     * singleton pattern. (Using default port from config file.
     *
     * <p>Note: This factory method will ignore the port argument if an instance is already
     * running. In that case, one must stop the WSNServer instance and invoke it again to
     * specify a different port.</p>
     *
     * @param port: An integer representing the port WSNServer should bind to.
     *
     * @return: The WSNotification instance.
     */
    public static WSNotificationServer getInstance(Integer port) {
        if (WSNotificationServer._invoked) return _singleton;
        else {
            _singleton = new WSNotificationServer(port);
            WSNotificationServer._invoked = true;
            return _singleton;
        }
    }

    /**
     * Initialization method that reads the wsnserver.xml configuration file and constructs
     * a jetty server instance.
     *
     * @param port: An integer representing the port WSNServer should bind to.
     */
    protected void init(Integer port) {

        log = Logger.getLogger(WSNotificationServer.class.getName());

        // TODO: Initialize other needed variables

        _client = null;
        Resource configResource = null;
        try {
            configResource = Resource.newSystemResource(configurationFile);
            XmlConfiguration config = new XmlConfiguration(configResource.getInputStream());
            this._server = (Server)config.configure();
            log.debug("XMLConfig complete, server instanciated.");

        } catch (Exception e) {
            log.error("Unable to start WSNotificationServer: " + e.getMessage());
        }
    }

    /**
     * The primary boot method for starting a WSNServer instance. Will only perform actions if the
     * server instance is not already running.
     * <p>
     * Initializes a HttpClient, and starts it. Also adds predefined connectors to the jetty server
     * instance. Constructs a new serverThread and starts the jetty server instance in this new thread.
     * </p>
     */
    public void boot() {

        log.info("Booting WSNServer.");
        if (!_running) {
            try {
                this._client = new HttpClient();
                this._client.setFollowRedirects(false);
                this._client.start();
                log.info("Started WSNServer HTTPClient");

                this._connectors.stream().forEach(c -> this._server.addConnector(c));

                this._serverThread = new Thread(() -> {
                    try {
                        WSNotificationServer.this._server.start();
                        WSNotificationServer.this._server.join();

                    } catch (Exception serverError) {
                        log.trace(serverError.getStackTrace());
                    }
                });
                this._serverThread.setName("WSNServer");
                this._serverThread.start();
                this._serverThread.join();
                WSNotificationServer._running = true;
                log.info("WSNServer Thread started successfully.");
            } catch (Exception e) {
                log.trace(e.getStackTrace());
            }
        }
    }

}
