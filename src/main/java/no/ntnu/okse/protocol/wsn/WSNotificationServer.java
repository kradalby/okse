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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import com.google.common.io.ByteStreams;
import no.ntnu.okse.Application;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
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
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.NotificationBrokerImpl;
import org.ntnunotif.wsnu.services.implementations.publisherregistrationmanager.SimplePublisherRegistrationManager;
import org.ntnunotif.wsnu.services.implementations.subscriptionmanager.SimpleSubscriptionManager;
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

    // Path to configuration file on classpath
    private static final String configurationFile = "/config/wsnserver.xml";

    // The singleton containing the WSNotificationServer instance
    private static WSNotificationServer _singleton;

    private Server _server;
    private WSNRequestParser _requestParser;
    private WSNCommandProxy _commandProxy;
    private final ArrayList<Connector> _connectors = new ArrayList();
    private HttpClient _client;
    private HttpHandler _handler;
    private HashSet<ServiceConnection> _services;

    /**
     * Empty constructor, uses defaults from jetty configuration file for WSNServer
     */
    private WSNotificationServer() {
        this.init(null);
        _running = false;
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

            if (port != null) {
                this.addStandardConnector("0.0.0.0", port);
                this._server.setConnectors((Connector[]) this._connectors.toArray());
            }

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
                // Turn off following HTTP 30x redirects for the client
                this._client.setFollowRedirects(false);
                this._client.start();
                log.info("Started WSNServer HTTPClient");

                // For all registered connectors in WSNotificationServer, add these to the Jetty Server
                this._connectors.stream().forEach(c -> this._server.addConnector(c));

                /* OKSE custom WS-Nu web services */
                WSNCommandProxy broker = new WSNCommandProxy();
                _commandProxy = broker;
                WSNSubscriptionManager subscriptionManager = new WSNSubscriptionManager();
                WSNRegistrationManager registrationManager = new WSNRegistrationManager();
                SubscriptionService.getInstance().addSubscriptionChangeListener(subscriptionManager);
                SubscriptionService.getInstance().addPublisherChangeListener(registrationManager);

                broker.quickBuild("broker", this._requestParser);
                subscriptionManager.quickBuild("subscriptionManager", this._requestParser);
                subscriptionManager.initCoreSubscriptionService(SubscriptionService.getInstance());
                registrationManager.quickBuild("registrationManager", this._requestParser);
                registrationManager.initCoreSubscriptionService(SubscriptionService.getInstance());
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
                totalErrors++;
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
            totalErrors++;
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
            this._client.stop();
            this._server.stop();
            log.info("WSNServer Client and ServerThread stopped");
        } catch (Exception e) {
            totalErrors++;
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
     * This interface method must take in an instance of Message, which contains the appropriate references
     * and flags needed to distribute the message to consumers. Implementation specific details can vary from
     * protocol to protocol, but the end result of a method call to sendMessage is that the message is delivered,
     * or an error is logged.
     *
     * @param message An instance of Message containing the required data to distribute a message.
     */
    @Override
    public void sendMessage(Message message) {
        log.debug("WSNServer recieved message for distribution");
        if (!message.getOriginProtocol().equals(protocolServerType)) {
            log.debug("The message originated from other protocol than WSNotification");

            // Create wrapper provided the message, subscriptionref, publisherref and dialect
            Notify notify = WSNTools.generateNotificationMessage(
                    message,    // OKSE Message object
                    message.getAttribute(WSNSubscriptionManager.WSN_ENDPOINT_TOKEN), // Returns publisherKey found
                    null, // We do not yet know the recipient endpoints
                    WSNTools._ConcreteTopicExpression
            );

            /*
                Start to resolve recipients. The reason we cannot re-use the WSNCommandProxy's
                sendNotification method is that it will inject the message to the MessageService for relay
                thus creating duplicate messages.
             */

            NuNamespaceContextResolver namespaceContextResolver = new NuNamespaceContextResolver();

            // bind namespaces to topics
            for (NotificationMessageHolderType holderType : notify.getNotificationMessage()) {

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

            // Remember current message with context
            Notify currentMessage = notify;
            NuNamespaceContextResolver currentMessageNamespaceContextResolver = namespaceContextResolver;

            // For all valid recipients
            for (String recipient : _commandProxy.getAllRecipients()) {

                // Filter do filter handling, if any
                Notify toSend = _commandProxy.getRecipientFilteredNotify(recipient, notify, namespaceContextResolver);

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
                    // Pass it along to the requestparser
                    _requestParser.acceptLocalMessage(outMessage);
                }
            }
        }
    }

    /**
     * Fetches the complete URI of this ProtocolServer
     * @return A string representing the complete URI of this ProtocolServer
     */
    public static String getURI(){
        if(_singleton._server.getURI() == null){
            return "http://0.0.0.0:8080";
        }
        return "http://" + _singleton._server.getURI().getHost()+ ":" + (_singleton._server.getURI().getPort() > -1 ? _singleton._server.getURI().getPort() : 8080);
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

            log.debug("Accepted message, trying to instantiate WSNu InternalMessage");

            // Get message content, if any
            InternalMessage outgoingMessage;
            if(request.getContentLength() > 0 || isChunked) {
                InputStream inputStream = request.getInputStream();
                outgoingMessage = new InternalMessage(InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE, inputStream);
            } else {
                outgoingMessage = new InternalMessage(InternalMessage.STATUS_OK, null);
            }

            log.debug("WSNInternalMessage: " + outgoingMessage);

            outgoingMessage.getRequestInformation().setEndpointReference(request.getRemoteHost());
            outgoingMessage.getRequestInformation().setRequestURL(request.getRequestURI());
            outgoingMessage.getRequestInformation().setParameters(request.getParameterMap());

            log.debug("EndpointReference: " + outgoingMessage.getRequestInformation().getEndpointReference());
            log.debug("Request URI: " + outgoingMessage.getRequestInformation().getRequestURL());

            log.debug("Forwarding message to requestParser...");

            // Push the outgoingMessage to the request parser. Based on the status flags of the return message
            // we should know what has happened, and which response we should send.
            InternalMessage returnMessage = WSNotificationServer.this._requestParser.parseMessage(outgoingMessage, response.getOutputStream());

            // Improper response from WSNRequestParser! FC WHAT DO?
            if (returnMessage == null) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                totalErrors++;
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
                    totalErrors++;

                    return;
                }

                /* If no valid destination was found for the request (Endpoint non-existant) */
                if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_DESTINATION) > 0) {
                    response.setStatus(HttpStatus.NOT_FOUND_404);
                    baseRequest.setHandled(true);
                    totalBadRequests++;

                    return;

                /* If there was an internal server error */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INTERNAL_ERROR) > 0) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    baseRequest.setHandled(true);
                    totalErrors++;

                    return;

                /* If there was syntactical errors or otherwise malformed request content */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_PAYLOAD) > 0) {
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    baseRequest.setHandled(true);
                    totalBadRequests++;

                    return;

                /* If the requested method or access to endpoint is forbidden */
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_ACCESS_NOT_ALLOWED) > 0) {
                    response.setStatus(HttpStatus.FORBIDDEN_403);
                    baseRequest.setHandled(true);
                    totalBadRequests++;

                    return;
                }

                /*
                    Otherwise, there has been an exception of some sort with no message attached,
                    and we will reply with a server error
                */
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                baseRequest.setHandled(true);
                totalErrors++;

            // Check if we have status=OK and also we have a message
            } else if (((InternalMessage.STATUS_OK & returnMessage.statusCode) > 0) &&
                    (InternalMessage.STATUS_HAS_MESSAGE & returnMessage.statusCode) > 0){

                /* Liar liar pants on fire */
                if (returnMessage.getMessage() == null) {

                    log.error("The HAS_RETURNING_MESSAGE flag was checked, but there was no returning message content");
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    baseRequest.setHandled(true);
                    totalErrors++;

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
                totalErrors++;

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
            totalErrors++;
            return new InternalMessage(InternalMessage.STATUS_FAULT, null);
        }

        /* Create the actual http-request*/
        org.eclipse.jetty.client.api.Request request = _client.newRequest(requestInformation.getEndpointReference());

        /* Try to send the message */
        try{
            /* Raw request */
            if ((message.statusCode & InternalMessage.STATUS_HAS_MESSAGE) == 0) {

                request.method(HttpMethod.GET);
                log.debug("Sending message without content to " + requestInformation.getEndpointReference());
                ContentResponse response = request.send();
                totalRequests++;

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
                        totalErrors++;
                        return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
                    }

                    // Send the request to the specified endpoint reference
                    log.info("Sending message with content to " + requestInformation.getEndpointReference());
                    request.content(new InputStreamContentProvider((InputStream) message.getMessage()), "application/soap+xml;charset/utf-8");
                    ContentResponse response = request.send();
                    totalMessages++;

                    // Check what HTTP status we recieved, if is not A-OK, flag the internalmessage as fault
                    // and make the response content the message of the InternalMessage returned
                    if (response.getStatus() != HttpStatus.OK_200) {
                        totalBadRequests++;
                        return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_HAS_MESSAGE, response.getContentAsString());
                    } else {
                        return new InternalMessage(InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE, response.getContentAsString());
                    }
                }
            }
        } catch(ClassCastException e) {
            log.error("sendMessage(): The message contained something else than an inputStream." +
                    "Please convert your message to an InputStream before calling this method.");
            totalErrors++;

            return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);

        } catch(Exception e) {
            totalErrors++;
            log.error("sendMessage(): Unable to establish connection: " + e.getMessage());
            return new InternalMessage(InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
        }
    }
}

