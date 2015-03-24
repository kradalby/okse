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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;

import com.google.common.io.ByteStreams;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.ntnunotif.wsnu.base.util.InternalMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Aleksander Skraastad (myth) on 3/12/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNotificationServer extends AbstractProtocolServer {

    // Statistics
    private static int totalRequests;
    private static int totalMessages;

    // Path to configuration file on classpath
    private static final String configurationFile = "/config/wsnserver.xml";

    // The singleton containing the WSNotificationServer instance
    private static WSNotificationServer _singleton;

    private Server _server;
    private WSNRequestParser _requestParser;
    private final ArrayList<Connector> _connectors = new ArrayList();
    private HttpClient _client;
    private HttpHandler _handler;

    /**
     * Empty constructor, uses defaults from jetty configuration file for WSNServer
     */
    private WSNotificationServer() {
        this.init(null);
        _invoked = true;
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

        // Initialize statistics
        totalRequests = 0;
        totalMessages = 0;

        // Set the servertype
        protocolServerType = "WSNotification";

        // Declare HttpClient field
        _client = null;

        // Declare configResource (Fetched from classpath as a Resource from system)
        Resource configResource;
        try {
            // Try to parse the configFile for WSNServer to set up the Server instance
            configResource = Resource.newSystemResource(configurationFile);
            XmlConfiguration config = new XmlConfiguration(configResource.getInputStream());
            this._server = (Server)config.configure();

            // Initialize the RequestParser for WSNotification
            this._requestParser = new WSNRequestParser();

            // Initialize and set the HTTPHandler for the Server instance
            HttpHandler handler = new WSNotificationServer.HttpHandler();
            this._server.setHandler(handler);

            log.debug("XMLConfig complete, server instanciated.");

        } catch (Exception e) {
            log.error("Unable to start WSNotificationServer: " + e.getMessage());
        }
    }

    /**
     * Total amount of requests from this WSNotificationServer that has passed through this server instance.
     * @return An integer representing the total amount of request.
     */
    @Override
    public int getTotalRequests() {
        return totalRequests;
    }

    /**
     * Total amount of messages from this WSNotificationServer that has passed through this server instance.
     * @return: An integer representing the total amount of messages.
     */
    @Override
    public int getTotalMessages() {
        return totalMessages;
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
                WSNotificationServer._running = true;
                log.info("WSNServer Thread started successfully.");
            } catch (Exception e) {
                log.trace(e.getStackTrace());
            }
        }
    }

    @Override
    public void stopServer() {
        try {
            log.info("Stopping WSNServer...");
            this._client.stop();
            this._server.stop();
            log.info("WSNServer Client and ServerThread stopped");
        } catch (Exception e) {
            log.trace(e.getStackTrace());
        }
    }

    private class HttpHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {

            log.debug("HttpHandle invoked on target: " + target);

            // Do some stats.
            totalRequests++;

            boolean isChunked = false;

            Enumeration headerNames = request.getHeaderNames();

            log.debug("Checking headers...");

            while(headerNames.hasMoreElements()) {
                String outMessage = (String)headerNames.nextElement();
                Enumeration returnMessage = request.getHeaders(outMessage);

                while(returnMessage.hasMoreElements()) {
                    String inputStream = (String)returnMessage.nextElement();
                    log.debug(outMessage + "=" + inputStream);
                    if(outMessage.equals("Transfer-Encoding") && inputStream.equals("chunked")) {
                        log.debug("Found Transfer-Encoding was chunked.");
                        isChunked = true;
                    }
                }
            }

            log.info("Accepted message, trying to instantiate WSNu InternalMessage");

            // Get message content, if any
            WSNInternalMessage outgoingMessage;
            if(request.getContentLength() > 0 || isChunked) {
                InputStream inputStream = request.getInputStream();
                outgoingMessage = new WSNInternalMessage(InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE, inputStream);
            } else {
                outgoingMessage = new WSNInternalMessage(InternalMessage.STATUS_OK, null);
            }

            log.info("WSNInternalMessage: " + outgoingMessage);

            // Update the outgoingMessage with correct information from the Jetty ServletRequest Object
            outgoingMessage.getRequestInformation().setEndpointReference(request.getRemoteHost());
            outgoingMessage.getRequestInformation().setRequestURL(request.getRequestURI());
            outgoingMessage.getRequestInformation().setParameters(request.getParameterMap());

            log.info("EndpointReference: " + outgoingMessage.getRequestInformation().getEndpointReference());
            log.info("Request URI: " + outgoingMessage.getRequestInformation().getRequestURL());

            log.info("Forwarding message to requestParser...");

            // Push the outgoingMessage to the request parser. Based on the status flags of the return message
            // we should know what has happened, and which response we should send.
            WSNInternalMessage returnMessage = WSNotificationServer.this._requestParser.parseMessage(outgoingMessage, response.getOutputStream());

            // Improper response from WSNRequestParser! FC WHAT DO?
            if (returnMessage == null) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                baseRequest.setHandled(true);
            }

            /* Handle possible errors */
            if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT) > 0){

                /* Have we got an error message to return? */
                if ((returnMessage.statusCode & InternalMessage.STATUS_HAS_MESSAGE) > 0){
                    response.setContentType("application/soap+xml;charset=utf-8");

                    InputStream inputStream = (InputStream)returnMessage.getMessage();
                    OutputStream outputStream = response.getOutputStream();

                    /* google.commons helper function*/
                    ByteStreams.copy(inputStream, outputStream);

                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    outputStream.flush();
                    baseRequest.setHandled(true);

                    return;
                }

                /* If no valid destination was found for the request (Endpoint non-existant) */
                if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_DESTINATION) > 0) {
                    response.setStatus(HttpStatus.NOT_FOUND_404);
                    baseRequest.setHandled(true);

                    return;

                /* If there was an internal server error */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INTERNAL_ERROR) > 0) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    baseRequest.setHandled(true);

                    return;

                /* If there was syntactical errors or otherwise malformed request content */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_PAYLOAD) > 0) {
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    baseRequest.setHandled(true);

                    return;

                /* If the requested method or access to endpoint is forbidden */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_ACCESS_NOT_ALLOWED) > 0) {
                    response.setStatus(HttpStatus.FORBIDDEN_403);
                    baseRequest.setHandled(true);

                    return;
                }

                /*
                    Otherwise, there has been an exception of some sort with no message attached,
                    and we will reply with a server error
                */
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                baseRequest.setHandled(true);

                return;

            // Check if we have status=OK and also we have a message
            } else if (((InternalMessage.STATUS_OK & returnMessage.statusCode) > 0) &&
                    (InternalMessage.STATUS_HAS_MESSAGE & returnMessage.statusCode) > 0){

                /* Liar liar pants on fire */
                if (returnMessage.getMessage() == null) {

                    log.error("The HAS_RETURNING_MESSAGE flag was checked, but there was no returning message content");
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    baseRequest.setHandled(true);

                    return;
                }

                // Prepare the response content type
                response.setContentType("application/soap+xml;charset=utf-8");

                // Allocate the input and output streams
                InputStream inputStream = (InputStream)returnMessage.getMessage();
                OutputStream outputStream = response.getOutputStream();

                /* Copy the contents of the input stream into the output stream */
                ByteStreams.copy(inputStream, outputStream);

                /* Set proper OK status and flush out the stream for response to be sent */
                response.setStatus(HttpStatus.OK_200);
                outputStream.flush();

                baseRequest.setHandled(true);

            /* Everything is fine, and nothing is expected */
            } else if ((InternalMessage.STATUS_OK & returnMessage.statusCode) > 0) {

                response.setStatus(HttpStatus.OK_200);
                baseRequest.setHandled(true);

            } else {
                // We obviously should never land in this block, hence we set the 500 status.
                log.error("HandleMessage: The message returned to the WSNotificationServer was not flagged with either STATUS_OK or" +
                        "STATUS_FAULT. Please set either of these flags at all points");

                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                baseRequest.setHandled(true);

            }
        }
    }
}
