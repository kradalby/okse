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

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.io.ByteStreams;
import no.ntnu.okse.Application;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.ntnunotif.wsnu.base.internal.ServiceConnection;
import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * Created by Aleksander Skraastad (myth) on 3/12/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNotificationServer extends AbstractProtocolServer {

    // Runstate variables
    private static boolean _invoked, _running;

    // Path to internal configuration file on classpath
    private static final String wsnInternalConfigFile = "/config/wsnserver.xml";

    // Internal Default Values
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 61000;
    private static final Long DEFAULT_CONNECTION_TIMEOUT = 5L;
    private static final Integer DEFAULT_HTTP_CLIENT_DISPATCHER_POOL_SIZE = 50;
    private static final String DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME = "Content";

    // Flag and defaults for operation behind NAT
    private static boolean behindNAT = false;
    private static String publicWANHost = "0.0.0.0";
    private static Integer publicWANPort = 61000;

    // HTTP Client fields
    private static Long connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private static Integer clientPoolSize = DEFAULT_HTTP_CLIENT_DISPATCHER_POOL_SIZE;

    // The singleton containing the WSNotificationServer instance
    private static WSNotificationServer _singleton;

    // Non-XMl Content Wrapper Name
    private static String contentWrapperElementName = DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME;

    // Instance fields
    private Server _server;
    private WSNRequestParser _requestParser;
    private WSNCommandProxy _commandProxy;
    private final ArrayList<Connector> _connectors = new ArrayList();
    private HttpClient _client;
    private HttpHandler _handler;
    private HashSet<ServiceConnection> _services;
    private ExecutorService clientPool;
    private Properties config;

    /**
     * Empty constructor, uses internal defaults or provided from config file
     */
    private WSNotificationServer() {
        // Check config file
        config = Application.readConfigurationFiles();
        String configHost = config.getProperty("WSN_HOST", DEFAULT_HOST);
        Integer configPort = null;
        try {
            configPort = Integer.parseInt(config.getProperty("WSN_PORT", Integer.toString(DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            log.error("Failed to parse WSN Port from config file! Using default: " + DEFAULT_PORT);
        }

        // Call init with what the results were
        this.init(configHost, configPort);

        _running = false;
        _invoked = true;
    }

    /**
     * Constructor that takes in a port that the WSNServer jetty instance should
     * listen to.
     * <p>
     * @param host A string representing the host the WSNServer should bind to
     * @param port An integer representing the port the WSNServer should bind to.
     */
    private WSNotificationServer(String host, Integer port) {
        // Check config file
        config = Application.readConfigurationFiles();
        this.init(host, port);
    }

    /**
     * Factory method providing an instance of WSNotificationServer, adhering to the
     * singleton pattern. (Using default port from config file.)
     * <p>
     * @return: The WSNotification instance.
     */
    public static WSNotificationServer getInstance() {
        if (!_invoked) _singleton = new WSNotificationServer();
        return _singleton;
    }

    /**
     * Factory method providing an instance of WSNotificationServer, adhering to the
     * singleton pattern. This method allows overriding of host and port defined in the
     * config file.
     *
     * @param host A string representing the hostname the server should bind to
     * @param port An integer representing the port WSNServer should bind to.
     *
     * @return: The WSNotification instance.
     */
    public static WSNotificationServer getInstance(String host, Integer port) {
        if (!_invoked) _singleton = new WSNotificationServer(host, port);
        return _singleton;
    }

    /**
     * Initialization method that reads the wsnserver.xml configuration file and constructs
     * a jetty server instance.
     *
     * @param port: An integer representing the port WSNServer should bind to.
     */
    protected void init(String host, Integer port) {

        log = Logger.getLogger(WSNotificationServer.class.getName());

        // Set the servertype
        protocolServerType = "WSNotification";

        // Declare HttpClient field
        _client = null;

        // Attempt to fetch connection timeout from settings, otherwise use 5 seconds as default
        try {
            connectionTimeout = Long.parseLong(config.getProperty("WSN_CONNECTION_TIMEOUT",
                    connectionTimeout.toString()));
        } catch (NumberFormatException e) {
            log.error("Failed to parse WSN Connection Timeout, using default: " + connectionTimeout);
        }

        // Attempt to fetch the HTTP Client pool size from settings, otherwise use default
        try {
            clientPoolSize = Integer.parseInt(config.getProperty("WSN_POOL_SIZE",
                    Integer.toString(DEFAULT_HTTP_CLIENT_DISPATCHER_POOL_SIZE)));
        } catch (NumberFormatException e) {
            log.error("Failed to parse WSN Client pool size from config file! Using default: " +
                    DEFAULT_HTTP_CLIENT_DISPATCHER_POOL_SIZE);
        }
        clientPool = Executors.newFixedThreadPool(clientPoolSize);

        // If a default message content wrapper name is specified in config, set it, otherwise use default
        contentWrapperElementName = config.getProperty("WSN_MESSAGE_CONTENT_ELEMENT_NAME",
                DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME);

        if (contentWrapperElementName.contains("<") || contentWrapperElementName.contains(">")) {
            log.warn("Non-XML message payload element wrapper name cannot contain XML element characters (< or >)," +
                    " using default: " + DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME);
            contentWrapperElementName = DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME;
        }

        // If we have host or port provided, set them, otherwise use internal defaults
        this.port = port == null ? DEFAULT_PORT : port;
        this.host = host == null ? DEFAULT_HOST : host;

        /* Check if config file specifies that we are behind NAT, and update the provided WAN IP and PORT */
        // Check for use NAT flag
        if (config.getProperty("WSN_USES_NAT", "false").equalsIgnoreCase("true")) behindNAT = true;
        else behindNAT = false;

        // Check for WAN_HOST
        publicWANHost = config.getProperty("WSN_WAN_HOST", publicWANHost);

        // Check for WAN_PORT
        try {
            publicWANPort = Integer.parseInt(config.getProperty("WSN_WAN_PORT", publicWANPort.toString()));
        } catch (NumberFormatException e) {
            log.error("Failed to parse WSN WAN Port, using default: " + publicWANPort);
        }

        // Declare configResource (Fetched from classpath as a Resource from system)
        Resource configResource;
        try {
            // Try to parse the configFile for WSNServer to set up the Server instance
            configResource = Resource.newSystemResource(wsnInternalConfigFile);
            XmlConfiguration config = new XmlConfiguration(configResource.getInputStream());
            this._server = (Server)config.configure();
            // Remove the xmlxonfig connector
            this._server.removeConnector(this._server.getConnectors()[0]);

            // Add a the serverconnector
            log.debug("Adding WSNServer connector");
            this.addStandardConnector(this.host, this.port);

            // Initialize the RequestParser for WSNotification
            this._requestParser = new WSNRequestParser(this);

            // Initialize the collection of ServiceConnections
            this._services = new HashSet<>();

            // Initialize and set the HTTPHandler for the Server instance
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
                this._client.setConnectTimeout(connectionTimeout * 1000L);
                // Turn off following HTTP 30x redirects for the client
                this._client.setFollowRedirects(false);
                this._client.start();
                log.info("Started WSNServer HTTPClient");

                // For all registered connectors in WSNotificationServer, add these to the Jetty Server
                this._connectors.stream().forEach(c -> this._server.addConnector(c));

                /* OKSE custom WS-Nu web services */

                // Initialize the CommandProxy
                WSNCommandProxy broker = new WSNCommandProxy();
                _commandProxy = broker;
                // Initialize the WSN SubscriptionManager and PublisherRegistrationManager
                WSNSubscriptionManager subscriptionManager = new WSNSubscriptionManager();
                WSNRegistrationManager registrationManager = new WSNRegistrationManager();
                // Add listener support from the OKSE SubscriptionService
                SubscriptionService.getInstance().addSubscriptionChangeListener(subscriptionManager);
                SubscriptionService.getInstance().addPublisherChangeListener(registrationManager);

                // QuickBuild the broker
                broker.quickBuild("broker", this._requestParser);
                // QuickBuild the WSN SubManager
                subscriptionManager.quickBuild("subscriptionManager", this._requestParser);
                subscriptionManager.initCoreSubscriptionService(SubscriptionService.getInstance());
                // QuickBuild the WSN PubRegManager
                registrationManager.quickBuild("registrationManager", this._requestParser);
                registrationManager.initCoreSubscriptionService(SubscriptionService.getInstance());
                // Register the WSN managers to the command proxy (proxied broker)
                broker.setSubscriptionManager(subscriptionManager);
                broker.setRegistrationManager(registrationManager);

                // Create a new thread for the Jetty Server to run in
                this._serverThread = new Thread(() -> {
                    this.run();
                });
                this._serverThread.setName("WSNServer");
                // Start the Jetty Server
                this._serverThread.start();
                WSNotificationServer._running = true;
                log.info("WSNServer Thread started successfully.");
            } catch (Exception e) {
                totalErrors.incrementAndGet();
                log.trace(e.getStackTrace());
            }
        }
    }

    /**
     * This interface method should contain the main run loop initialization
     */
    @Override
    public void run() {
        try {
            WSNotificationServer.this._server.start();
            WSNotificationServer.this._server.join();

        } catch (Exception serverError) {
            totalErrors.incrementAndGet();
            log.trace(serverError.getStackTrace());
        }
    }

    /**
     * Fetch the HashSet containing all WebServices registered to the protocol server
     * @return A HashSet of ServiceConnections for all the registered web services.
     */
    public HashSet<ServiceConnection> getServices() {
        return _services;
    }

    /**
     * This method stops the execution of the WSNotificationServer instance.
     */
    @Override
    public void stopServer() {
        try {
            log.info("Stopping WSNServer...");
            // Removing all subscribers
            _commandProxy.getAllRecipients().forEach(s -> {
                _commandProxy.getProxySubscriptionManager().removeSubscriber(s);
            });
            // Removing all publishers
            _commandProxy.getProxyRegistrationManager().getAllPublishers().forEach(p -> {
                _commandProxy.getProxyRegistrationManager().removePublisher(p);
            });

            // Stop the HTTP Client
            this._client.stop();
            // Stop the ServerConnector
            this._server.stop();
            this._serverThread = null;
            // Reset flags
            this._singleton = null;
            this._invoked = false;
            log.info("WSNServer Client and ServerThread stopped");
        } catch (Exception e) {
            totalErrors.incrementAndGet();
            log.trace(e.getStackTrace());
        }
    }

    /**
     * Fetches the specified String representation of the Protocol that this ProtocolServer handles.
     * @return A string representing the name of the protocol that this ProtocolServer handles.
     */
    @Override
    public String getProtocolServerType() {
        return protocolServerType;
    }

    /**
     * Support method to allow other classes in the wsn package to increment total messages received
     */
    protected void incrementTotalMessagesReceived() {
        totalMessagesReceived.incrementAndGet();
    }

    /**
     * Retrieve the default element name for non-XML messages that are to be wrapped in a soap enveloped
     * WSNotification Notify element. This element will be the first and only child of the Message element.
     * @return The default name of the content wrapper element
     */
    public static String getMessageContentWrapperElementName() {
        return contentWrapperElementName;
    }

    /**
     * This interface method must take in an instance of Message, which contains the appropriate references
     * and flags needed to distribute the message to consumers. Implementation specific details can vary from
     * protocol to protocol, but the end result of a method call to sendMessage is that the message is delivered,
     * or an error is logged.
     *
     * @param message An instance of Message containing the required data to distribute a message.
     */
    @Override
    public void sendMessage(Message message) {
        log.debug("WSNServer received message for distribution");
        if (!message.getOriginProtocol().equals(protocolServerType) || message.getAttribute("duplicate") != null) {
            log.debug("The message originated from other protocol than WSNotification");

            WSNTools.NotifyWithContext notifywrapper = WSNTools.buildNotifyWithContext(message.getMessage(), message.getTopic(), null, null);
            // If it contained XML, we need to create properly marshalled jaxb node structure
            if (message.getMessage().contains("<") || message.getMessage().contains(">")) {
                // Unmarshal from raw XML
                Notify notify = WSNTools.createNotify(message);
                // If it was malformed, or maybe just a message containing < or >, build it as generic content element
                if (notify == null) {
                    WSNTools.injectMessageContentIntoNotify(WSNTools.buildGenericContentElement(message.getMessage()), notifywrapper.notify);
                    // Else inject the unmarshalled XML nodes into the Notify message attribute
                } else {
                    WSNTools.injectMessageContentIntoNotify(WSNTools.extractMessageContentFromNotify(notify), notifywrapper.notify);
                }
            }

            /*
                Start to resolve recipients. The reason we cannot re-use the WSNCommandProxy's
                sendNotification method is that it will inject the message to the MessageService for relay
                thus creating duplicate messages.
             */

            NuNamespaceContextResolver namespaceContextResolver = notifywrapper.nuNamespaceContextResolver;

            // bind namespaces to topics
            for (NotificationMessageHolderType holderType : notifywrapper.notify.getNotificationMessage()) {

                // Extract the topic
                TopicExpressionType topic = holderType.getTopic();

                if (holderType.getTopic() != null) {
                    NuNamespaceContextResolver.NuResolvedNamespaceContext context = namespaceContextResolver.resolveNamespaceContext(topic);

                    if (context == null) {
                        continue;
                    }

                    context.getAllPrefixes().forEach(prefix -> {
                        // check if this is the default xmlns attribute
                        if (!prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                            // add namespace context to the expression node
                            topic.getOtherAttributes().put(new QName("xmlns:" + prefix), context.getNamespaceURI(prefix));
                        }
                    });
                }
            }

            // For all valid recipients
            for (String recipient : _commandProxy.getAllRecipients()) {

                // If the subscription has expired, continue
                if (_commandProxy.getProxySubscriptionManager().getSubscriber(recipient).hasExpired()) continue;

                // Filter do filter handling, if any
                Notify toSend = _commandProxy.getRecipientFilteredNotify(recipient, notifywrapper.notify, namespaceContextResolver);

                // If any message was left to send, send it
                if (toSend != null) {
                    InternalMessage outMessage = new InternalMessage(
                            InternalMessage.STATUS_OK |
                                    InternalMessage.STATUS_HAS_MESSAGE |
                                    InternalMessage.STATUS_ENDPOINTREF_IS_SET,
                            toSend
                    );
                    // Update the requestinformation
                    outMessage.getRequestInformation().setEndpointReference(_commandProxy.getEndpointReferenceOfRecipient(recipient));

                    // Check if the subscriber has requested raw message format
                    // If the recipient has requested UseRaw, remove Notify payload wrapping
                    if (_commandProxy
                            .getProxySubscriptionManager()
                            .getSubscriber(recipient)
                            .getAttribute(WSNSubscriptionManager.WSN_USERAW_TOKEN) != null) {

                        Object content = WSNTools.extractMessageContentFromNotify(toSend);
                        // Update the InternalMessage with the content of the NotificationMessage
                        outMessage.setMessage(content);
                    }

                    // Pass it along to the request parser wrapped as a thread pool executed job
                    clientPool.execute(() -> _requestParser.acceptLocalMessage(outMessage));
                }
            }
        } else {
            log.debug("Message originated from WSN protocol, already processed");
        }
    }

    /**
     * Fetches the complete URI of this ProtocolServer
     * @return A string representing the complete URI of this ProtocolServer
     */
    public String getURI() {
        // Check if we are behind NAT
        if (behindNAT) {
            return "http://" + publicWANHost + ":" + publicWANPort;
        }
        // If somehow URI could not be retrieved
        if (_singleton._server.getURI() == null) {
            _singleton.log.warn("Failed to fetch URI of server");
            return "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT;
        }
        // Return the server connectors registered host and port
        return "http://" + _singleton._server.getURI().getHost()+ ":" + (_singleton._server.getURI().getPort() > -1 ? _singleton._server.getURI().getPort() : DEFAULT_PORT);
    }

    /**
     * Returns the public WAN Host if behindNAT is true. If behindNAT is false, the value of host is returned.
     * @return The public WAN Host
     */
    public String getPublicWANHost() {
        if (behindNAT) return publicWANHost;
        return host;
    }

    /**
     * Returns the public WAN Port if behindNAT is true. If behindNAT is false, the value of port is returned.
     * @return The public WAN Port
     */
    public Integer getPublicWANPort() {
        if (behindNAT) return publicWANPort;
        return port;
    }

    /**
     * Registers the specified ServiceConnection to the ProtocolServer
     * @param webServiceConnector: The ServiceConnection you wish to register.
     */
    public synchronized void registerService(ServiceConnection webServiceConnector) {
        _services.add(webServiceConnector);
    }

    /**
     * Unregisters the specified ServiceConnection from the ProtocolServer
     * @param webServiceConnector: The ServiceConnection you wish to remove.
     */
    public synchronized void removeService(ServiceConnection webServiceConnector) {
        _services.remove(webServiceConnector);
    }

    /**
     * Add a standard serverconnector to the server instance.
     * @param address The IP address you wish to bind the serverconnector to
     * @param port The port you with to bind the serverconnector to
     */
    public void addStandardConnector(String address, int port){
        ServerConnector connector = new ServerConnector(_server);
        connector.setHost(address);
        if(port == 80){
            log.warn("You have requested to use port 80. This will not work unless you are running as root." +
                    "Are you running as root? You shouldn't. Reroute port 80 to 8080 instead.");
        }
        connector.setPort(port);
        _connectors.add(connector);
        _server.addConnector(connector);
    }

    /**
     * Add a predefined serverconnector to the server instance.
     * @param connector A jetty ServerConnector
     */
    public void addConnector(Connector connector){
        _connectors.add(connector);
        this._server.addConnector(connector);
    }

    /**
     * Fetch the WSNRequestParser object
     * @return WSNRequestParser
     */
    public WSNRequestParser getRequestParser() {
        return this._requestParser;
    }

    // This is the HTTP Handler that the WSNServer uses to process all incoming requests
    private class HttpHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {

            log.debug("HttpHandle invoked on target: " + target);

            // Do some stats.
            totalRequests.incrementAndGet();

            boolean isChunked = false;

            Enumeration headerNames = request.getHeaderNames();

            log.debug("Checking headers...");

            // Check the request headers, check for chunked encoding
            while(headerNames.hasMoreElements()) {
                String outMessage = (String)headerNames.nextElement();
                Enumeration returnMessage = request.getHeaders(outMessage);

                while(returnMessage.hasMoreElements()) {
                    String inputStream = (String)returnMessage.nextElement();
                    if(outMessage.equals("Transfer-Encoding") && inputStream.equals("chunked")) {
                        log.debug("Found Transfer-Encoding was chunked.");
                        isChunked = true;
                    }
                }
            }

            log.debug("Accepted message, trying to instantiate WSNu InternalMessage");

            // Get message content, if any
            InternalMessage outgoingMessage;
            if (request.getContentLength() > 0) {
                log.debug("Content length was: " + request.getContentLength());
                InputStream inputStream = request.getInputStream();
                outgoingMessage = new InternalMessage(InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE | InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM, inputStream);
            } else if (isChunked) {
                log.debug("Chunked transfer encoding!");
                InputStream chunkedInputStream = request.getInputStream();

                outgoingMessage = new InternalMessage(
                        InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE | InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM,
                        chunkedInputStream);
            } else {
                outgoingMessage = new InternalMessage(InternalMessage.STATUS_OK, null);
            }

            log.debug("WSNInternalMessage: " + outgoingMessage);

            // Update the request information object
            outgoingMessage.getRequestInformation().setEndpointReference(request.getRemoteHost());
            outgoingMessage.getRequestInformation().setRequestURL(request.getRequestURI());
            outgoingMessage.getRequestInformation().setParameters(request.getParameterMap());

            log.debug("EndpointReference: " + outgoingMessage.getRequestInformation().getEndpointReference());
            log.debug("Request URI: " + outgoingMessage.getRequestInformation().getRequestURL());

            log.debug("Forwarding message to requestParser...");

            // Push the outgoingMessage to the request parser. Based on the status flags of the return message
            // we should know what has happened, and which response we should send.
            InternalMessage returnMessage = null;
            try {
                returnMessage = WSNotificationServer.this._requestParser.parseMessage(outgoingMessage, response.getOutputStream());
            } catch (Exception e) {
                log.error("Uncaught exception: " + e.getMessage());
                log.trace(e.getStackTrace());
            }

            // Improper response from WSNRequestParser! FC WHAT DO?
            if (returnMessage == null) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                totalErrors.incrementAndGet();
                baseRequest.setHandled(true);
                returnMessage = new InternalMessage(InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
            }

            /* Handle possible errors */
            if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT) > 0) {

                /* Have we got an error message to return? */
                if ((returnMessage.statusCode & InternalMessage.STATUS_HAS_MESSAGE) > 0) {
                    response.setContentType("application/soap+xml;charset=utf-8");

                    // Declare input and output streams
                    InputStream inputStream = (InputStream)returnMessage.getMessage();
                    OutputStream outputStream = response.getOutputStream();

                    // Pipe the data from input to output stream
                    ByteStreams.copy(inputStream, outputStream);

                    // Set proper HTTP status, flush the output stream and set the handled flag
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    outputStream.flush();
                    baseRequest.setHandled(true);
                    totalErrors.incrementAndGet();
                    outputStream.close();

                    return;
                }

                /* If no valid destination was found for the request (Endpoint non-existant) */
                if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_DESTINATION) > 0) {
                    response.setStatus(HttpStatus.NOT_FOUND_404);
                    baseRequest.setHandled(true);
                    totalBadRequests.incrementAndGet();

                    return;

                /* If there was an internal server error */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INTERNAL_ERROR) > 0) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    baseRequest.setHandled(true);
                    totalErrors.incrementAndGet();

                    return;

                /* If there was syntactical errors or otherwise malformed request content */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_PAYLOAD) > 0) {
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    baseRequest.setHandled(true);
                    totalBadRequests.incrementAndGet();

                    return;

                /* If the requested method or access to endpoint is forbidden */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_ACCESS_NOT_ALLOWED) > 0) {
                    response.setStatus(HttpStatus.FORBIDDEN_403);
                    baseRequest.setHandled(true);
                    totalBadRequests.incrementAndGet();

                    return;
                }

                /*
                    Otherwise, there has been an exception of some sort with no message attached,
                    and we will reply with a server error
                */
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                baseRequest.setHandled(true);
                totalErrors.incrementAndGet();

                // Check if we have status=OK and also we have a message
            } else if (((InternalMessage.STATUS_OK & returnMessage.statusCode) > 0) &&
                    (InternalMessage.STATUS_HAS_MESSAGE & returnMessage.statusCode) > 0){

                /* Liar liar pants on fire */
                if (returnMessage.getMessage() == null) {

                    log.error("The HAS_RETURNING_MESSAGE flag was checked, but there was no returning message content");
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    baseRequest.setHandled(true);
                    totalErrors.incrementAndGet();

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
                totalErrors.incrementAndGet();

            }
        }
    }

    public InternalMessage sendMessage(InternalMessage message) {

        // Fetch the requestInformation from the message, and extract the endpoint
        RequestInformation requestInformation = message.getRequestInformation();
        String endpoint = requestInformation.getEndpointReference();

        /* If we have nowhere to send the message */
        if(endpoint == null){
            log.error("Endpoint reference not set");
            totalErrors.incrementAndGet();
            return new InternalMessage(InternalMessage.STATUS_FAULT, null);
        }

        /* Create the actual http-request*/
        org.eclipse.jetty.client.api.Request request = _client.newRequest(requestInformation.getEndpointReference());
        _client.setIdleTimeout(2000);

        /* Try to send the message */
        try{
            /* Raw request */
            if ((message.statusCode & InternalMessage.STATUS_HAS_MESSAGE) == 0) {

                request.method(HttpMethod.GET);

                log.debug("Sending message without content to " + requestInformation.getEndpointReference());
                ContentResponse response = request.send();
                totalRequests.incrementAndGet();

                return new InternalMessage(InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE, response.getContentAsString());
            /* Request with message */
            } else {

                // Set proper request method
                request.method(HttpMethod.POST);

                // If the statusflag has set a message and it is not an input stream
                if ((message.statusCode & InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM) == 0) {
                    log.error("sendMessage(): " + "The message contained something else than an inputStream." +
                            "Please convert your message to an InputStream before calling this methbod.");

                    return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);

                } else {

                    // Check if we should have had a message, but there was none
                    if(message.getMessage() == null){
                        log.error("No content was found to send");
                        totalErrors.incrementAndGet();
                        return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
                    }

                    // Send the request to the specified endpoint reference
                    log.info("Sending message with content to " + requestInformation.getEndpointReference());
                    InputStream msg = (InputStream) message.getMessage();
                    request.content(new InputStreamContentProvider(msg), "application/soap+xml; charset=utf-8");
                    request.send((result) -> {
                        if (!HttpStatus.isSuccess(result.getResponse().getStatus())) {
                            totalBadRequests.incrementAndGet();
                        } else {
                            totalMessagesSent.incrementAndGet();
                        }
                    });
                }
            }
        } catch(ClassCastException e) {
            log.error("sendMessage(): The message contained something else than an inputStream." +
                    "Please convert your message to an InputStream before calling this method.");
            totalErrors.incrementAndGet();

            return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);

        } catch(Exception e) {
            totalErrors.incrementAndGet();
            e.printStackTrace();
            log.error("sendMessage(): Unable to establish connection: " + e.getMessage());
            return new InternalMessage(InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
        }

        // Since the client request is async, we just return OK since we cannot know if it fails
        return new InternalMessage(InternalMessage.STATUS_OK, null);
    }
}

