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

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import no.ntnu.okse.protocol.amqp.AMQProtocolServer;
import no.ntnu.okse.protocol.amqp.Driver;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Iterator;
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

    // Fields
    private ServerSocketChannel serverChannel;
    private HashSet<SocketChannel> clients;
    private Selector selector;

    /**
     * Private constructor
     * @param port The port this server should bind to
     */
    private DummyProtocolServer(Integer port) {
        init(port);
    }

    /**
     * The main DummuProtocolServer instanciation method
     * @return The DummyProtocolServer instance
     */
    public static DummyProtocolServer getInstance() {
        if (!_invoked) _singleton = new DummyProtocolServer(61001);
        return _singleton;
    }

    /**
     * Initialization method
     * @param port The port this server should bind to
     */
    @Override
    protected void init(Integer port) {
        log = Logger.getLogger(DummyProtocolServer.class.getName());
        protocolServerType = "DummyProtocol";
        _invoked = true;
        clients = new HashSet<>();
        this.port = port;
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
                serverChannel.socket().bind(new InetSocketAddress("0.0.0.0", this.port));
                // Set to non-blocking
                serverChannel.configureBlocking(false);
                // Register the serverChannel to the selector
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                log.debug(protocolServerType + " listening on " + serverChannel.socket().getInetAddress().getHostAddress() +
                        ":" + serverChannel.socket().getLocalPort());

            } catch (UnknownHostException e) {
                log.error("Could not bind socket: " + e.getMessage());
                totalErrors++;
            } catch (ClosedChannelException e) {
                log.error("Closed channel: " + e.getMessage());
                totalErrors++;
            } catch (IOException e) {
                log.error("I/O exception: " + e.getMessage());
                totalErrors++;
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

                        log.debug("Command recieved: " + command);
                        totalRequests++;

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
                            totalBadRequests++;
                        }

                        // Send confirmation of execution
                        writeBuffer = ByteBuffer.wrap(result.getBytes());
                        client.write(writeBuffer);
                        writeBuffer.clear();

                        // If we recieved an exit, close socket and cancel the key
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
                totalErrors++;
                log.error("I/O exception during select operation: " + e.getMessage());
            } catch (Exception e) {
                totalErrors++;
                log.error("Unknown exception: " + e.getMessage());
            }
        }

    }

    /**
     * Shuts down the server
     */
    @Override
    public void stopServer() {
        _running = false;
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
     * @param command The command string recieved from the client
     */
    private boolean parseCommand(String command) {
        String[] args = command.split(" ");
        try {
            if (args[0].equalsIgnoreCase("amqp")) {
                AMQProtocolServer amqp = AMQProtocolServer.getInstance();
                if (args[1].equalsIgnoreCase("stop")) {
                    amqp.stopServer();
                }
                else if (args[1].equalsIgnoreCase("start")) {
                    //amqp.boot();
                    amqp.run();
                }
                return true;
            }
            // message <topic> <message content>
            else if (args[0].equalsIgnoreCase("message")) {
                // Attempt to fetch the topic
                Topic t = TopicService.getInstance().getTopic(args[1]);
                // If it exists build a message string and distribute it
                if (t != null) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        builder.append(args[i] + " ");
                    }
                    String msg = builder.toString().trim();
                    Message message = new Message(msg, t, null);
                    message.setOriginProtocol(protocolServerType);
                    MessageService.getInstance().distributeMessage(message);
                    totalMessages++;

                    return true;
                }
            } else if (args[0].equalsIgnoreCase("exit")) {
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Recieved invalid command: " + command);
        }

        return false;
    }
}
