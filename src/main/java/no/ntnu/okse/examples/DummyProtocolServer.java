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

package no.ntnu.okse.examples;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.event.SystemEvent;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import no.ntnu.okse.protocol.wsn.WSNTools;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.util.Utilities;
import org.ntnunotif.wsnu.services.general.WsnUtilities;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Created by Aleksander Skraastad (myth) on 4/19/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class DummyProtocolServer extends AbstractProtocolServer {

    // Needed statics
    private static Logger log;
    private static DummyProtocolServer _singleton;
    private static Thread _serverThread;
    private static boolean _invoked;

    // Internal defaults
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 61001;

    // Fields
    private ServerSocketChannel serverChannel;
    private HashSet<SocketChannel> clients;
    private Selector selector;
    private static Properties config;

    /**
     * Private constructor
     *
     * @param host The host this server should bind to
     * @param port The port this server should bind to
     */
    private DummyProtocolServer(String host, Integer port) {
        init(host, port);
    }

    /**
     * The main DummuProtocolServer instanciation method
     *
     * @return The DummyProtocolServer instance
     */
    public static DummyProtocolServer getInstance() {
        // If not invoked, create an instance and inject as _singleton
        if (!_invoked) {
            // Read the config
            config = Application.readConfigurationFiles();
            // Attempt to extract host and port from configuration file
            Integer configPort = null;
            String configHost = null;
            // Fetch potential data from configuration file
            if (config.containsKey("DUMMYPROTOCOL_HOST")) configHost = config.getProperty("DUMMYPROTOCOL_HOST");
            if (config.containsKey("DUMMYPROTOCOL_PORT")) {
                try {
                    configPort = Integer.parseInt(config.getProperty("DUMMYPROTOCOL_PORT"));
                } catch (NumberFormatException e) {
                    log.error("Failed to parse DummyProtocol Port, using default: " + DEFAULT_PORT);
                }
            }
            // Update singleton
            _singleton = new DummyProtocolServer(configHost, configPort);
        }

        return _singleton;
    }

    /**
     * Initialization method
     *
     * @param port The port this server should bind to
     */
    @Override
    protected void init(String host, Integer port) {
        // Init logger
        log = Logger.getLogger(DummyProtocolServer.class.getName());
        // Set protocol name
        protocolServerType = "DummyProtocol";
        // Update invoked flag
        _invoked = true;
        // Initialize the client set
        clients = new HashSet<>();

        // If we have host or port provided, set them, otherwise use internal defaults
        this.port = port == null ? DEFAULT_PORT : port;
        this.host = host == null ? DEFAULT_HOST : host;

        try {

            // Create a multiplexer (Selector)
            selector = Selector.open();
            // Open a ServerSocketChanel
            serverChannel = ServerSocketChannel.open();

        } catch (IOException e) {
            log.error("Failed to initialize DummyProtocolServer");
        }
    }

    /**
     * Main startup sequence
     */
    @Override
    public void boot() {
        if (!_running) {
            _running = true;
            log.info("Booting DummyProtocolServer...");

            try {
                // Bind the serverchannel localhost on 61001
                serverChannel.socket().bind(new InetSocketAddress(this.host, this.port));
                // Set to non-blocking
                serverChannel.configureBlocking(false);
                // Register the serverChannel to the selector
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                log.debug(protocolServerType + " listening on " + serverChannel.socket().getInetAddress().getHostAddress() +
                        ":" + serverChannel.socket().getLocalPort());

            } catch (UnknownHostException e) {
                log.error("Could not bind socket: " + e.getMessage());
                totalErrors.incrementAndGet();
            } catch (ClosedChannelException e) {
                log.error("Closed channel: " + e.getMessage());
                totalErrors.incrementAndGet();
            } catch (IOException e) {
                log.error("I/O exception: " + e.getMessage());
                totalErrors.incrementAndGet();
            }

            // Create and start the serverThread
            _serverThread = new Thread(() -> this.run());
            _serverThread.setName("DummyProtocolServer");
            _serverThread.start();

            log.info("DummyProtocolServer booted successfully");
        }
    }

    /**
     * Main run method
     */
    @Override
    public void run() {
        // Main run loop
        while (_running) {
            try {
                // Start multiplexing, this will block until a channel is in a ready state
                selector.select();

                // Initialize the read and write buffers
                ByteBuffer readBuffer, writeBuffer;

                // Fetch the selected keys and make iterator
                Set selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {

                    // Fetch and store next key and remove from iterator
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    // Check if it is a new connection from a client (Only serverSocketChannel will have this flag)
                    if (key.isAcceptable()) {
                        // Accept the connection and store the channel
                        SocketChannel client = serverChannel.accept();
                        clients.add(client);
                        // Set the client to non-blocking mode
                        client.configureBlocking(false);
                        // Register the client in the selector
                        client.register(selector, SelectionKey.OP_READ);

                        log.info("New client:" + client.socket().getInetAddress() + ":" + client.socket().getPort());

                        // Continue with next key
                        continue;
                    }

                    if (key.isReadable()) {
                        // Fetch the client in question
                        SocketChannel client = (SocketChannel) key.channel();
                        // Allocate a buffer
                        readBuffer = ByteBuffer.allocate(4096);

                        // Read from the client
                        int bytesRead = client.read(readBuffer);

                        // Extract the message
                        readBuffer.flip();
                        // Create byte array to store read data
                        byte[] rawData = new byte[bytesRead];
                        // Fetch the read data from the buffer
                        readBuffer.get(rawData, 0, bytesRead);
                        // Create a string from the read data
                        String command = new String(rawData).trim();
                        // Clear the buffer
                        readBuffer.clear();

                        log.debug("Command received: " + command);
                        totalRequests.incrementAndGet();

                        // Write a response
                        byte[] response = new String("Executing: " + command + "\n").getBytes();
                        writeBuffer = ByteBuffer.wrap(response);
                        client.write(writeBuffer);

                        // Parse and execute the command
                        boolean valid = parseCommand(command);
                        String result;
                        if (valid) {
                            result = "Command executed.\n";
                        } else {
                            result = "Invalid command.\n";
                            totalBadRequests.incrementAndGet();
                        }

                        // Send confirmation of execution
                        writeBuffer = ByteBuffer.wrap(result.getBytes());
                        client.write(writeBuffer);
                        writeBuffer.clear();

                        // If we received an exit, close socket and cancel the key
                        if (command.equalsIgnoreCase("exit")) {
                            clients.remove(client);
                            log.info("Disconnected client:" + client.socket().getInetAddress() + ":" + client.socket().getPort());
                            client.socket().close();
                            key.cancel();
                        }

                        break;
                    }
                }

            } catch (IOException e) {
                totalErrors.incrementAndGet();
                log.error("I/O exception during select operation: " + e.getMessage());
            } catch (ClosedSelectorException e) {
                log.debug("Caught SelectorClose, shutting down");
            } catch (Exception e) {
                log.error("Caught unknown exception: " + e);
            }
        }
        log.info("DummypPotocolServer stopped.");
    }

    /**
     * Shuts down the server
     */
    @Override
    public void stopServer() {
        _running = false;
        clients.forEach(socket -> {
            try {
                socket.write(ByteBuffer.wrap("System is shutting down.\n".getBytes()));
                socket.socket().close();
            } catch (IOException e) {
                log.error("IOException during client close.");
            }
        });
        try {
            selector.close();
            serverChannel.socket().close();
        } catch (IOException e) {
            log.error("IOException during shutdown");
        }
        _invoked = false;
        _serverThread = null;
        _singleton = null;
    }

    /**
     * Returns the type of this protocolserver as a string
     *
     * @return A string representing the name of the protocol in question.
     */
    @Override
    public String getProtocolServerType() {
        return this.protocolServerType;
    }

    /**
     * Send a message using DummyProtocol
     *
     * @param message An instance of Message containing the required data to distribute a message.
     */
    @Override
    public void sendMessage(Message message) {
        if (!message.getOriginProtocol().equals(protocolServerType)) {
            log.info("[FAKE] Sending message: " + message);
        }
    }

    /* Private helper methods */

    /**
     * Parse an incoming command from the raw string
     *
     * @param command The command string received from the client
     * @return boolean based on command success
     */
    private boolean parseCommand(String command) {
        String[] args = command.split(" ");
        try {
            // message <topic> <message content>
            if (args[0].equalsIgnoreCase("message")) {
                // If it exists build a message string and distribute it
                if (args.length > 2) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        builder.append(args[i] + " ");
                    }
                    String msg = builder.toString().trim();
                    Message message = new Message(msg, args[1], null, protocolServerType);
                    MessageService.getInstance().distributeMessage(message);
                    totalMessagesReceived.incrementAndGet();

                    return true;
                }
            } else if (args[0].equalsIgnoreCase("relay")) {
                if (Utilities.isValidInetAddress(args[1])) {
                    if (Utilities.isValidInetAddress(args[2])) {
                        // relay <remote URI> <URI of this broker>
                        log.debug(WSNTools.extractSubscriptionReferenceFromRawXmlResponse(
                                WsnUtilities.sendSubscriptionRequest(
                                        args[1], args[2],
                                        WSNotificationServer.getInstance().getRequestParser()
                                )
                        ));
                        return true;
                    }
                }
            } else if (args[0].equalsIgnoreCase("testuri")) {
                log.debug(WSNotificationServer.getInstance().getURI());
            } else if (args[0].equalsIgnoreCase("shutdownprotocolservers")) {
                log.debug("SHUTDOWN PROTOCOL SERVERS RECIEVED");
                try {
                    CoreService.getInstance().getEventQueue().put(new SystemEvent(
                            SystemEvent.Type.SHUTDOWN_PROTOCOL_SERVERS,
                            null
                    ));
                } catch (InterruptedException e) {
                    log.error("Interrupted while attempting to insert an event into CoreService");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("exit")) {
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Received invalid command: " + command);
        }

        return false;
    }
}
