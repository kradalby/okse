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

package no.ntnu.okse.protocol.wsn;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;

import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.ntnunotif.wsnu.base.util.InternalMessage;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    private HttpHandler _handler;

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
        Resource configResource;
        try {
            configResource = Resource.newSystemResource(configurationFile);
            XmlConfiguration config = new XmlConfiguration(configResource.getInputStream());
            this._server = (Server)config.configure();
            HttpHandler handler = new WSNotificationServer.HttpHandler();
            this._server.setHandler(handler);
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
                // Initialize a plain HttpClient
                this._client = new HttpClient();
                // Turn off following HTTP 30x redirects for the client
                this._client.setFollowRedirects(false);
                this._client.start();
                log.info("Started WSNServer HTTPClient");

                // For all registered connectors in WSNotificationServer, add these to the Jetty Server
                this._connectors.stream().forEach(c -> this._server.addConnector(c));

                // Create a new thread for the Jetty Server to run in
                this._serverThread = new Thread(() -> {
                    try {
                        WSNotificationServer.this._server.start();
                        WSNotificationServer.this._server.join();

                    } catch (Exception serverError) {
                        log.trace(serverError.getStackTrace());
                    }
                });
                this._serverThread.setName("WSNServer");
                // Start the Jetty Server
                this._serverThread.start();
                this._serverThread.join();
                WSNotificationServer._running = true;
                log.info("WSNServer Thread started successfully.");
            } catch (Exception e) {
                log.trace(e.getStackTrace());
            }
        }
    }

    private class HttpHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {

            log.info("HttpHandle invoked on target: " + target);

            boolean isChunked = false;

            Enumeration headerNames = request.getHeaderNames();

            log.info("Checking headers...");
            while(headerNames.hasMoreElements()) {
                String outMessage = (String)headerNames.nextElement();
                Enumeration returnMessage = request.getHeaders(outMessage);

                while(returnMessage.hasMoreElements()) {
                    String inputStream = (String)returnMessage.nextElement();
                    log.info(outMessage + "=" + inputStream);
                    if(outMessage.equals("Transfer-Encoding") && inputStream.equals("chunked")) {
                        log.info("Found Transfer-Encoding was chunked.");
                        isChunked = true;
                    }
                }
            }

            log.info("Accepted message, trying to instantiate WSNu InternalMessage");
            InternalMessage outMessage1;
            if(request.getContentLength() <= 0 && !isChunked) {
                outMessage1 = new InternalMessage(1, null);
            } else {
                ServletInputStream returnMessage1 = request.getInputStream();
                outMessage1 = new InternalMessage(5, returnMessage1);
            }
            log.info("InternalMessage: " + outMessage1);

            outMessage1.getRequestInformation().setEndpointReference(request.getRemoteHost());
            outMessage1.getRequestInformation().setRequestURL(request.getRequestURI());
            outMessage1.getRequestInformation().setParameters(request.getParameterMap());

            //log.info("OutMessage: " + outMessage1.getMessage());
            log.info("OutMessage: " + outMessage1.getRequestInformation().getEndpointReference());
            log.info("OutMessage: " + outMessage1.getRequestInformation().getRequestURL());
            log.info("OutMessage: " + outMessage1.getRequestInformation().getHttpStatus());

            OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
            writer.write("Success, you tried to access " + target + " from " +
                    request.getRemoteAddr());
            writer.flush();

            response.setStatus(200);
            baseRequest.setHandled(true);

            // And at this point WSNu forwards the outMessage1 to the forwardingHub, and proceeds to await
            // a new InternalMessage, named returnMessage with correct status flags and proper content.

            // We need to look into the hub to what the method acceptNetMessage does.
        }
    }
}
