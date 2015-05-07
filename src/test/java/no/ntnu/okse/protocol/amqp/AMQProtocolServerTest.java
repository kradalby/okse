package no.ntnu.okse.protocol.amqp;

import junit.framework.TestCase;
import no.ntnu.okse.Application;
import org.junit.Test;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.net.Socket;

public class AMQProtocolServerTest extends TestCase {

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        try{
            new Socket("localhost", 8080).close();
        }
        catch(IOException e){
            Application.main(new String[0]);
        }
        AMQProtocolServer amqp = AMQProtocolServer.getInstance();
    }

    public void tearDown() throws Exception {

    }

    @Test
    public void testIncrementTotalMessagesSent() throws Exception {
        AMQProtocolServer amqp = AMQProtocolServer.getInstance();
        int result = amqp.getTotalMessagesSent();
        assertEquals(0, result);

        amqp.incrementTotalMessagesSent();
        result = amqp.getTotalMessagesSent();
        assertEquals(1, result);
    }

    @Test
    public void testIncrementTotalMessagesReceived() throws Exception {
        AMQProtocolServer amqp = AMQProtocolServer.getInstance();
        int result = amqp.getTotalMessagesReceived();
        assertEquals(0, result);

        amqp.incrementTotalMessagesReceived();
        result = amqp.getTotalMessagesReceived();
        assertEquals(1, result);
    }

    @Test
    public void testIncrementTotalRequests() throws Exception {
        AMQProtocolServer amqp = AMQProtocolServer.getInstance();
        int result = amqp.getTotalRequests();
        assertEquals(0, result);

        amqp.incrementTotalRequests();
        result = amqp.getTotalRequests();
        assertEquals(1, result);
    }

    @Test
    public void testIncrementTotalBadRequest() throws Exception {
        AMQProtocolServer amqp = AMQProtocolServer.getInstance();
        int result = amqp.getTotalBadRequests();
        assertEquals(0, result);

        amqp.incrementTotalBadRequest();
        result = amqp.getTotalBadRequests();
        assertEquals(1, result);
    }

    @Test
    public void testIncrementTotalErrors() throws Exception {
        AMQProtocolServer amqp = AMQProtocolServer.getInstance();
        int result = amqp.getTotalErrors();
        assertEquals(0, result);

        amqp.incrementTotalErrors();
        result = amqp.getTotalErrors();
        assertEquals(1, result);
    }

    @Test
    public void testIsShuttingDown() throws Exception {
        AMQProtocolServer amqp = AMQProtocolServer.getInstance();
        boolean result = amqp.isShuttingDown();
        assertEquals(false, result);
    }
}