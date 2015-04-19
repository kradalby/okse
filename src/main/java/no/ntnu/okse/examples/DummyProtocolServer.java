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
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

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
    private ServerSocket serverSocket;
    private HashSet<Socket> connections;

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
        connections = new HashSet<>();
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(this.port);
            log.info("ServerSocket listening on port " + this.port);
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
        // Declare reader and writer
        BufferedReader reader;
        DataOutputStream writer;

        while (_running) {
            try {
                // Await a connection
                Socket connection = serverSocket.accept();
                log.info("New connection: " + connection.getRemoteSocketAddress().toString());
                // Connect the reader to the socket connection's inputstream
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                // Connect the writer to the socket connection's outputstream
                writer = new DataOutputStream(connection.getOutputStream());

                String command;

                // While the connection is active, read a command
                while ((command = reader.readLine()) != null) {
                    // Log the recieved command
                    log.info("Command recieved: " + command);

                    // Return a response
                    writer.write("OK\n".getBytes("UTF-8"));
                    writer.flush();
                }

            } catch (IOException e) {
                log.error("I/O exception during accept(): " + e.getMessage());
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
        log.info("[FAKE] Sending message: " + message);
    }
}
