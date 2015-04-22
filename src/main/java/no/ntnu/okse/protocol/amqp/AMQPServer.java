package no.ntnu.okse.protocol.amqp;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.Publisher;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.impl.MessengerImpl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Created by Trond Walleraunet on 21.04.2015.
 */
public class AMQPServer extends AbstractProtocolServer{

    // Needed statics
    private static Logger log;
    private static AMQPServer _singleton;
    private static Thread _serverThread;
    private static boolean _invoked;

    // Fields
    private ServerSocket serverSocket;
    private HashSet<Socket> connections;

    /**
     * Private constructor
     * @param port The port this server should bind to
     */
    private AMQPServer(Integer port) {
        init(port);
    }

    /**
     * The main AMQPServer instanciation method
     * @return The AMQPServer instance
     */
    public static AMQPServer getInstance() {
        if (!_invoked) _singleton = new AMQPServer(61001);
        return _singleton;
    }

    /**
     * Initialization method
     * @param port The port this server should bind to
     */
    @Override
    protected void init(Integer port) {
        log = Logger.getLogger(AMQPServer.class.getName());
        protocolServerType = "AMQP";
        _invoked = true;
        connections = new HashSet<>();
        this.port = port;
        try {
            this.serverSocket = new ServerSocket(this.port);
            log.info("ServerSocket listening on port " + this.port);
        } catch (IOException e) {
            log.error("Failed to initialize AMQPServer");
        }
    }

    /**
     * Main startup sequence
     */
    @Override
    public void boot() {
        if (!_running) {
            _running = true;
            log.info("Booting AMQPServer...");
            _serverThread = new Thread(() -> this.run());
            _serverThread.setName("AMQPServer");
            _serverThread.start();
            log.info("AMQPServer booted successfully");
        }
        //Tester sendmessage()
        sendMessage(new Message("Test message", new Topic("Test","raw"), new Publisher("Test", "127.0.0.1", 1234, "OKSE")));
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

                    // Update stats
                    totalMessages++;
                    totalRequests++;

                    // Return a response
                    writer.write("OK\n".getBytes("UTF-8"));
                    writer.flush();
                }

            } catch (IOException e) {
                totalErrors++;
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
     * Send a message using AMQPServer
     *
     * @param message An instance of Message containing the required data to distribute a message.
     */
    @Override
    public void sendMessage(Message message) {
        log.info("[AMQP] Sending message: " + message);
        try {
            Messenger mng = new MessengerImpl();
            mng.start();
            MessageImpl msg = new MessageImpl();
            msg.setAddress("127.0.0.1");
            msg.setSubject(message.getTopic().getFullTopicString());
            for (String body : new String[]{message.getMessage()}) {
                msg.setBody(new AmqpValue(body));
                mng.put(msg);
            }
            mng.send();
            mng.stop();
        } catch (Exception e) {
            log.info("Qpid proton error", e);
        }
    }
}
