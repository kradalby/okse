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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package no.ntnu.okse.protocol.amqp;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.messaging.Message;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.impl.Address;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import static org.testng.Assert.*;

public class AMQPServerTest {


    @BeforeMethod
    public void setUp() throws Exception {
        // This is _not_ a good way to detect
        // if the AMQProtocolServer.getInstance().getServer() is initiated or not.
        try{
            new Socket("localhost", 8080).close();
        }
        catch(IOException e){
            Application.main(new String[0]);
        }

    }

    @AfterMethod
    public void tearDown() throws Exception {
        //CoreService.getInstance().stopAllProtocolServers();
        //CoreService.getInstance().getAllProtocolServers().forEach(ps ->  {
        //    CoreService.getInstance().removeProtocolServer(ps);
        //});
    }


    @Test
    public void testConvertAMQPMessageToMessageBytes() throws Exception {
        String message = "Hei på test";
        String topic = "test/testConvertAMQPMessageToMessageBytes";

        Section body = new AmqpValue(message);

        org.apache.qpid.proton.message.Message AMQPMessage = org.apache.qpid.proton.message.Message.Factory.create();

        AMQPMessage.setAddress("127.0.0.1" + "/" + topic);
        AMQPMessage.setSubject("Supertesty test");
        AMQPMessage.setBody(body);

        MessageBytes mb = AMQPServer.convertAMQPMessageToMessageBytes(AMQPMessage);

        org.apache.qpid.proton.message.Message AMQPMessageReconstruct = org.apache.qpid.proton.message.Message.Factory.create();

        AMQPMessageReconstruct.decode(mb.getBytes(), 0, mb.getBytes().length);

        assertEquals(AMQPMessage.getAddress(), AMQPMessageReconstruct.getAddress());
        assertEquals(AMQPMessage.getSubject(), AMQPMessageReconstruct.getSubject());
        //assertEquals(AMQPMessage.getBody(), AMQPMessageReconstruct.getBody());
        assertEquals((String) ((AmqpValue) AMQPMessage.getBody()).getValue(), (String) ((AmqpValue) AMQPMessageReconstruct.getBody()).getValue());
    }

    @Test
    public void testConvertOkseMessageToAMQP() throws Exception {
        String topic = "test";
        Message okseMessage = new Message("Hei", topic, null, "AMQP");

        org.apache.qpid.proton.message.Message AMQPMessage = AMQPServer.convertOkseMessageToAMQP(okseMessage);

        Address address = new Address(AMQPMessage.getAddress());


        assertEquals(okseMessage.getMessage(), (String) ((AmqpValue) AMQPMessage.getBody()).getValue());
        assertEquals(okseMessage.getTopic(), address.getName());
        assertEquals(okseMessage.getMessageID(), AMQPMessage.getSubject());
    }

//    @Test
//    public void testSendRecieveAMQPMessagesWhenQueueIsUsed() throws Exception {
//        AMQProtocolServer.getInstance().useQueue = true;
//        if (AMQProtocolServer.getInstance().useQueue) {
//            String message = "Megatest";
//            String topic = "test/test";
//            int numberOfMessages = 5;
//
//            Messenger sendMessenger = Messenger.Factory.create();
//            sendMessenger.start();
//            org.apache.qpid.proton.message.Message msg = org.apache.qpid.proton.message.Message.Factory.create();
//
//            msg.setAddress(AMQProtocolServer.getInstance().getHost() + ":" + AMQProtocolServer.getInstance().getPort() + "/" + topic);
//            msg.setBody(new AmqpValue(message));
//
//            for (int i = 0; i < numberOfMessages; i++) {
//                sendMessenger.put(msg);
//                sendMessenger.send();
//                Thread.sleep(50);
//            }
//
//            sendMessenger.stop();
//
//            ArrayList<org.apache.qpid.proton.message.Message> in = new ArrayList<>();
//
//            Messenger receiveMessenger = Messenger.Factory.create();
//            receiveMessenger.start();
//            receiveMessenger.subscribe(AMQProtocolServer.getInstance().getHost() + ":" + AMQProtocolServer.getInstance().getPort() + "/" + topic);
//
//            receiveMessenger.recv(numberOfMessages);
//            while (receiveMessenger.incoming() > 0) {
//                System.out.println("derp");
//                msg = receiveMessenger.get();
//                in.add(msg);
//            }
//
//            assertEquals(numberOfMessages, in.size());
//        }
//    }
}