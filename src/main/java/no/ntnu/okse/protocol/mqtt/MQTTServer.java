package no.ntnu.okse.protocol.mqtt;

import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.log4j.Logger;

import org.dna.mqtt.moquette.server.Server;
import org.dna.mqtt.moquette.server.ServerAcceptor;
import org.dna.mqtt.moquette.server.netty.NettyAcceptor;
import org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Properties;

//Ny
//import static org.dna.mqtt.moquette.server.Constants.PERSISTENT_STORE_PROPERTY_NAME;
//Gammel
//import static org.eclipse.moquette.commons.Constants.PERSISTENT_STORE_PROPERTY_NAME;



/**
 * Created by Trond Walleraunet on 17.04.2015.
 */
public class MQTTServer extends AbstractProtocolServer {

    //moquette spesifikk
    ServerAcceptor m_acceptor;
    SimpleMessaging messaging;
    Properties m_properties;


    @Override
    protected void init(Integer port) {
        log = Logger.getLogger(MQTTServer.class.getName());

        // Set the servertype
        protocolServerType = "MQTT";

    }

    @Override
    public void boot(){
        //moquette spesifikk
        final Server server = new Server();
        try {
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server started, version 0.7-SNAPSHOT");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stopServer();
            }
        });
    }

    @Override
    public void stopServer() {

    }

    @Override
    public String getProtocolServerType() {
        return protocolServerType;
    }

    //Moquette spesifikk
    /**
     * Starts the server with the given properties.
     *
     * Its suggested to at least have the following properties:
     * <ul>
     *  <li>port</li>
     *  <li>password_file</li>
     * </ul>
     */
    public void startServer(Properties configProps) throws IOException {
        ConfigurationParser confParser = new ConfigurationParser(configProps);
        m_properties = confParser.getProperties();
        log.info("Persistent store file: " + m_properties.get(PERSISTENT_STORE_PROPERTY_NAME));
        messaging = SimpleMessaging.getInstance();
        messaging.init(m_properties);

        m_acceptor = new NettyAcceptor();
        m_acceptor.initialize(messaging, m_properties);
    }
}

