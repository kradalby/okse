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
import org.apache.qpid.proton.amqp.transport.*;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.engine.impl.DeliveryImpl;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.impl.Address;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumSet;

import static org.testng.Assert.*;


@Test(singleThreaded = true, threadPoolSize = 0, sequential = false)
public class AMQPServerTest {


    AMQProtocolServer ps;

    @BeforeMethod
    public void setUp() throws Exception {
        ps = AMQProtocolServer.getInstance();
        ps.boot();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        ps.stopServer();
        ps = null;
    }

    @Test(groups = "amqp")
    public void testSendReceiveAMQPMessagesWhenQueueIsUsed() throws Exception {
        //AMQProtocolServer psTest = AMQProtocolServer.getInstance("localhost", 63660);
        AMQProtocolServer psTest = ps;
        //psTest.boot();
        psTest.useQueue = true;
//        if (AMQProtocolServer.getInstance().useQueue) {
        String message = "Megatest";
        String topic = "test/test";
        int numberOfMessages = 5;


        Messenger sendMessenger = Messenger.Factory.create();
        sendMessenger.start();
        org.apache.qpid.proton.message.Message msg = org.apache.qpid.proton.message.Message.Factory.create();


        msg.setAddress(psTest.getHost() + ":" + psTest.getPort() + "/" + topic);
        msg.setBody(new AmqpValue(message));


        System.out.println("Preparing to send messages...");
        for (int i = 0; i < numberOfMessages; i++) {
            sendMessenger.put(msg);
            System.out.println(String.format("Sending message %d", i));
            sendMessenger.send();
        }
        sendMessenger.stop();
        ArrayList<org.apache.qpid.proton.message.Message> in = new ArrayList<>();

        Messenger receiveMessenger = Messenger.Factory.create();
        receiveMessenger.start();
        System.out.println("Subscribing...");
        receiveMessenger.subscribe(psTest.getHost() + ":" + psTest.getPort() + "/" + topic);
        System.out.printf("Receiving messages...");
        receiveMessenger.recv(numberOfMessages);
        while (receiveMessenger.incoming() > 0) {
            msg = receiveMessenger.get();
            in.add(msg);
        }

        assertEquals(numberOfMessages, in.size());
        System.out.println(String.format("Got %d messages", in.size()));
//        }
    }


    @Test(groups = "amqp")
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

    @Test(groups = "amqp")
    public void testConvertOkseMessageToAMQP() throws Exception {
        System.out.println("testConvertOkseMessageToAMQP");
        String topic = "test";
        Message okseMessage = new Message("Hei", topic, null, "AMQP");

        org.apache.qpid.proton.message.Message AMQPMessage = AMQPServer.convertOkseMessageToAMQP(okseMessage);

        Address address = new Address(AMQPMessage.getAddress());


        assertEquals(okseMessage.getMessage(), (String) ((AmqpValue) AMQPMessage.getBody()).getValue());
        assertEquals(okseMessage.getTopic(), address.getName());
    }

    @Test(groups = "amqp")
    public void testCreateAddress() throws Exception {
        String topic = "test";
        String address1 = "amqp://127.0.0.1/test";
        String address2 = "127.0.0.1/test";
        String address3 = "127.0.0.1:61050/test";
        String address4 = "test";
        String address5 = "m.fest.no:6666/test";
        String address6 = "test/test";

        Delivery dlv = new Delivery() {
            @Override
            public byte[] getTag() {
                return new byte[0];
            }

            @Override
            public Link getLink() {
                return new Link() {
                    @Override
                    public String getName() {
                        return null;
                    }

                    @Override
                    public Delivery delivery(byte[] bytes) {
                        return null;
                    }

                    @Override
                    public Delivery delivery(byte[] bytes, int i, int i1) {
                        return null;
                    }

                    @Override
                    public Delivery head() {
                        return null;
                    }

                    @Override
                    public Delivery current() {
                        return null;
                    }

                    @Override
                    public boolean advance() {
                        return false;
                    }

                    @Override
                    public Source getSource() {
                        return null;
                    }

                    @Override
                    public Target getTarget() {
                        return new Target() {
                            @Override
                            public String getAddress() {
                                return topic;
                            }
                        };
                    }

                    @Override
                    public void setSource(Source source) {

                    }

                    @Override
                    public void setTarget(Target target) {

                    }

                    @Override
                    public Source getRemoteSource() {
                        return null;
                    }

                    @Override
                    public Target getRemoteTarget() {
                        return null;
                    }

                    @Override
                    public Link next(EnumSet<EndpointState> enumSet, EnumSet<EndpointState> enumSet1) {
                        return null;
                    }

                    @Override
                    public int getCredit() {
                        return 0;
                    }

                    @Override
                    public int getQueued() {
                        return 0;
                    }

                    @Override
                    public int getUnsettled() {
                        return 0;
                    }

                    @Override
                    public Session getSession() {
                        return null;
                    }

                    @Override
                    public SenderSettleMode getSenderSettleMode() {
                        return null;
                    }

                    @Override
                    public void setSenderSettleMode(SenderSettleMode senderSettleMode) {

                    }

                    @Override
                    public SenderSettleMode getRemoteSenderSettleMode() {
                        return null;
                    }

                    @Override
                    public ReceiverSettleMode getReceiverSettleMode() {
                        return null;
                    }

                    @Override
                    public void setReceiverSettleMode(ReceiverSettleMode receiverSettleMode) {

                    }

                    @Override
                    public ReceiverSettleMode getRemoteReceiverSettleMode() {
                        return null;
                    }

                    @Override
                    public void setRemoteSenderSettleMode(SenderSettleMode senderSettleMode) {

                    }

                    @Override
                    public int drained() {
                        return 0;
                    }

                    @Override
                    public int getRemoteCredit() {
                        return 0;
                    }

                    @Override
                    public boolean getDrain() {
                        return false;
                    }

                    @Override
                    public void detach() {

                    }

                    @Override
                    public EndpointState getLocalState() {
                        return null;
                    }

                    @Override
                    public EndpointState getRemoteState() {
                        return null;
                    }

                    @Override
                    public ErrorCondition getCondition() {
                        return null;
                    }

                    @Override
                    public void setCondition(ErrorCondition errorCondition) {

                    }

                    @Override
                    public ErrorCondition getRemoteCondition() {
                        return null;
                    }

                    @Override
                    public void free() {

                    }

                    @Override
                    public void open() {

                    }

                    @Override
                    public void close() {

                    }

                    @Override
                    public void setContext(Object o) {

                    }

                    @Override
                    public Object getContext() {
                        return null;
                    }
                };
            }

            @Override
            public DeliveryState getLocalState() {
                return null;
            }

            @Override
            public DeliveryState getRemoteState() {
                return null;
            }

            @Override
            public int getMessageFormat() {
                return 0;
            }

            @Override
            public void disposition(DeliveryState deliveryState) {

            }

            @Override
            public void settle() {

            }

            @Override
            public boolean isSettled() {
                return false;
            }

            @Override
            public boolean remotelySettled() {
                return false;
            }

            @Override
            public void free() {

            }

            @Override
            public Delivery getWorkNext() {
                return null;
            }

            @Override
            public Delivery next() {
                return null;
            }

            @Override
            public boolean isWritable() {
                return false;
            }

            @Override
            public boolean isReadable() {
                return false;
            }

            @Override
            public void setContext(Object o) {

            }

            @Override
            public Object getContext() {
                return null;
            }

            @Override
            public boolean isUpdated() {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public boolean isPartial() {
                return false;
            }

            @Override
            public int pending() {
                return 0;
            }

            @Override
            public boolean isBuffered() {
                return false;
            }
        };

        assertEquals(AMQPServer.createAddress(address1, dlv).getName(), topic);
        assertEquals(AMQPServer.createAddress(address2, dlv).getName(), topic);
        assertEquals(AMQPServer.createAddress(address3, dlv).getName(), topic);
        assertEquals(AMQPServer.createAddress(address4, dlv).getName(), topic);
        assertEquals(AMQPServer.createAddress(address5, dlv).getName(), topic);
        assertEquals(AMQPServer.createAddress(address6, dlv).getName(), topic);
    }

    @Test(groups = "amqp")
    public void testConvertAMQPmessageToOkseMessage() throws Exception {
        String message = "Hei på test";
        String topic = "test/testConvertAMQPMessageToMessageBytes";
        String address = "127.0.0.1" + "/" + topic;

        Section body = new AmqpValue(message);

        org.apache.qpid.proton.message.Message AMQPMessage = org.apache.qpid.proton.message.Message.Factory.create();

        Address addr = new Address(address);

        AMQPMessage.setAddress(address);
        AMQPMessage.setBody(body);

        no.ntnu.okse.core.messaging.Message okseMessage = AMQPServer.convertAMQPmessageToOkseMessage(AMQPMessage, addr);

        assertEquals(okseMessage.getMessage(), message);
        assertEquals(okseMessage.getMessage(), (String) ((AmqpValue) AMQPMessage.getBody()).getValue());
        assertEquals(okseMessage.getTopic(), topic);
        assertEquals(okseMessage.getOriginProtocol(), "AMQP");

    }

    @Test(groups = "amqp")
    public void testMessageStore() throws Exception {
        AMQPServer.TestMessageStore messageStore = AMQPServer.createMessageStoreFactory();

        String address1 = "test";
        String address2 = "test/test";

        MessageBytes mb1 = new MessageBytes("test1".getBytes());
        MessageBytes mb2 = new MessageBytes("test2".getBytes());
        MessageBytes mb3 = new MessageBytes("test3".getBytes());
        MessageBytes mb4 = new MessageBytes("test4".getBytes());
        MessageBytes mb5 = new MessageBytes("test5".getBytes());
        MessageBytes mb6 = new MessageBytes("test6".getBytes());

        messageStore.put(address1, mb1);
        messageStore.put(address1, mb2);
        messageStore.put(address1, mb3);

        messageStore.put(address2, mb4);
        messageStore.put(address2, mb5);
        messageStore.put(address2, mb6);

        assertEquals(mb1, messageStore.get(address1));
        assertFalse(mb3.equals(messageStore.get(address1)));
        assertEquals(mb3, messageStore.get(address1));

        assertFalse(mb6.equals(messageStore.get(address2)));
        assertFalse(mb6.equals(messageStore.get(address2)));
        assertEquals(mb6, messageStore.get(address2));

    }

    @Test(groups = "amqp")
    public void testRoutes() throws Exception {
        SubscriptionHandler.Routes<Sender> routes = new SubscriptionHandler.Routes();

        ArrayList<Sender> senders = new ArrayList<>();

        class TestSender implements Sender {

            @Override
            public void offer(int i) {

            }

            @Override
            public int send(byte[] bytes, int i, int i1) {
                return 0;
            }

            @Override
            public void abort() {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Delivery delivery(byte[] bytes) {
                return null;
            }

            @Override
            public Delivery delivery(byte[] bytes, int i, int i1) {
                return null;
            }

            @Override
            public Delivery head() {
                return null;
            }

            @Override
            public Delivery current() {
                return null;
            }

            @Override
            public boolean advance() {
                return false;
            }

            @Override
            public Source getSource() {
                return null;
            }

            @Override
            public Target getTarget() {
                return null;
            }

            @Override
            public void setSource(Source source) {

            }

            @Override
            public void setTarget(Target target) {

            }

            @Override
            public Source getRemoteSource() {
                return null;
            }

            @Override
            public Target getRemoteTarget() {
                return null;
            }

            @Override
            public Link next(EnumSet<EndpointState> enumSet, EnumSet<EndpointState> enumSet1) {
                return null;
            }

            @Override
            public int getCredit() {
                return 0;
            }

            @Override
            public int getQueued() {
                return 0;
            }

            @Override
            public int getUnsettled() {
                return 0;
            }

            @Override
            public Session getSession() {
                return null;
            }

            @Override
            public SenderSettleMode getSenderSettleMode() {
                return null;
            }

            @Override
            public void setSenderSettleMode(SenderSettleMode senderSettleMode) {

            }

            @Override
            public SenderSettleMode getRemoteSenderSettleMode() {
                return null;
            }

            @Override
            public ReceiverSettleMode getReceiverSettleMode() {
                return null;
            }

            @Override
            public void setReceiverSettleMode(ReceiverSettleMode receiverSettleMode) {

            }

            @Override
            public ReceiverSettleMode getRemoteReceiverSettleMode() {
                return null;
            }

            @Override
            public void setRemoteSenderSettleMode(SenderSettleMode senderSettleMode) {

            }

            @Override
            public int drained() {
                return 0;
            }

            @Override
            public int getRemoteCredit() {
                return 0;
            }

            @Override
            public boolean getDrain() {
                return false;
            }

            @Override
            public void detach() {

            }

            @Override
            public EndpointState getLocalState() {
                return null;
            }

            @Override
            public EndpointState getRemoteState() {
                return null;
            }

            @Override
            public ErrorCondition getCondition() {
                return null;
            }

            @Override
            public void setCondition(ErrorCondition errorCondition) {

            }

            @Override
            public ErrorCondition getRemoteCondition() {
                return null;
            }

            @Override
            public void free() {

            }

            @Override
            public void open() {

            }

            @Override
            public void close() {

            }

            @Override
            public void setContext(Object o) {

            }

            @Override
            public Object getContext() {
                return null;
            }
        }

        Sender snd1 = new TestSender();
        Sender snd2 = new TestSender();
        Sender snd3 = new TestSender();
        Sender snd4 = new TestSender();
        Sender snd5 = new TestSender();

        routes.add(snd1);
        senders.add(snd1);
        routes.add(snd2);
        senders.add(snd2);
        routes.add(snd3);
        senders.add(snd3);
        routes.add(snd4);
        senders.add(snd4);
        routes.add(snd5);
        senders.add(snd5);

        assertTrue(senders.contains(routes.choose()));
        assertEquals(routes.size(), senders.size());
        assertEquals(routes.getRoutes(), senders);
        assertFalse(routes.size() == 6);
        assertFalse(routes.size() == 4);

        routes.remove(snd2);
        assertFalse(routes.size() == 5);
        assertEquals(routes.size(), 4);

    }

    @Test(groups = "amqp")
    public void testGetAddress() throws Exception {

        class TestSender implements Sender {

            String address;

            TestSender(String address) {
                this.address = address;
            }

            @Override
            public void offer(int i) {

            }

            @Override
            public int send(byte[] bytes, int i, int i1) {
                return 0;
            }

            @Override
            public void abort() {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Delivery delivery(byte[] bytes) {
                return null;
            }

            @Override
            public Delivery delivery(byte[] bytes, int i, int i1) {
                return null;
            }

            @Override
            public Delivery head() {
                return null;
            }

            @Override
            public Delivery current() {
                return null;
            }

            @Override
            public boolean advance() {
                return false;
            }

            @Override
            public Source getSource() {
                return new Source() {
                    @Override
                    public String getAddress() {
                        return address;
                    }
                };
            }

            @Override
            public Target getTarget() {
                return new Target() {
                    @Override
                    public String getAddress() {
                        return address;
                    }
                };
            }

            @Override
            public void setSource(Source source) {

            }

            @Override
            public void setTarget(Target target) {

            }

            @Override
            public Source getRemoteSource() {
                return null;
            }

            @Override
            public Target getRemoteTarget() {
                return null;
            }

            @Override
            public Link next(EnumSet<EndpointState> enumSet, EnumSet<EndpointState> enumSet1) {
                return null;
            }

            @Override
            public int getCredit() {
                return 0;
            }

            @Override
            public int getQueued() {
                return 0;
            }

            @Override
            public int getUnsettled() {
                return 0;
            }

            @Override
            public Session getSession() {
                return null;
            }

            @Override
            public SenderSettleMode getSenderSettleMode() {
                return null;
            }

            @Override
            public void setSenderSettleMode(SenderSettleMode senderSettleMode) {

            }

            @Override
            public SenderSettleMode getRemoteSenderSettleMode() {
                return null;
            }

            @Override
            public ReceiverSettleMode getReceiverSettleMode() {
                return null;
            }

            @Override
            public void setReceiverSettleMode(ReceiverSettleMode receiverSettleMode) {

            }

            @Override
            public ReceiverSettleMode getRemoteReceiverSettleMode() {
                return null;
            }

            @Override
            public void setRemoteSenderSettleMode(SenderSettleMode senderSettleMode) {

            }

            @Override
            public int drained() {
                return 0;
            }

            @Override
            public int getRemoteCredit() {
                return 0;
            }

            @Override
            public boolean getDrain() {
                return false;
            }

            @Override
            public void detach() {

            }

            @Override
            public EndpointState getLocalState() {
                return null;
            }

            @Override
            public EndpointState getRemoteState() {
                return null;
            }

            @Override
            public ErrorCondition getCondition() {
                return null;
            }

            @Override
            public void setCondition(ErrorCondition errorCondition) {

            }

            @Override
            public ErrorCondition getRemoteCondition() {
                return null;
            }

            @Override
            public void free() {

            }

            @Override
            public void open() {

            }

            @Override
            public void close() {

            }

            @Override
            public void setContext(Object o) {

            }

            @Override
            public Object getContext() {
                return null;
            }
        }
        class TestReceiver implements Receiver {

            String address;

            TestReceiver(String address) {
                this.address = address;
            }

            @Override
            public void flow(int i) {

            }

            @Override
            public int recv(byte[] bytes, int i, int i1) {
                return 0;
            }

            @Override
            public void drain(int i) {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Delivery delivery(byte[] bytes) {
                return null;
            }

            @Override
            public Delivery delivery(byte[] bytes, int i, int i1) {
                return null;
            }

            @Override
            public Delivery head() {
                return null;
            }

            @Override
            public Delivery current() {
                return null;
            }

            @Override
            public boolean advance() {
                return false;
            }

            @Override
            public Source getSource() {
                return null;
            }

            @Override
            public Target getTarget() {
                return new Target() {
                    @Override
                    public String getAddress() {
                        return address;
                    }
                };
            }

            @Override
            public void setSource(Source source) {

            }

            @Override
            public void setTarget(Target target) {

            }

            @Override
            public Source getRemoteSource() {
                return null;
            }

            @Override
            public Target getRemoteTarget() {
                return null;
            }

            @Override
            public Link next(EnumSet<EndpointState> enumSet, EnumSet<EndpointState> enumSet1) {
                return null;
            }

            @Override
            public int getCredit() {
                return 0;
            }

            @Override
            public int getQueued() {
                return 0;
            }

            @Override
            public int getUnsettled() {
                return 0;
            }

            @Override
            public Session getSession() {
                return null;
            }

            @Override
            public SenderSettleMode getSenderSettleMode() {
                return null;
            }

            @Override
            public void setSenderSettleMode(SenderSettleMode senderSettleMode) {

            }

            @Override
            public SenderSettleMode getRemoteSenderSettleMode() {
                return null;
            }

            @Override
            public ReceiverSettleMode getReceiverSettleMode() {
                return null;
            }

            @Override
            public void setReceiverSettleMode(ReceiverSettleMode receiverSettleMode) {

            }

            @Override
            public ReceiverSettleMode getRemoteReceiverSettleMode() {
                return null;
            }

            @Override
            public void setRemoteSenderSettleMode(SenderSettleMode senderSettleMode) {

            }

            @Override
            public int drained() {
                return 0;
            }

            @Override
            public int getRemoteCredit() {
                return 0;
            }

            @Override
            public boolean getDrain() {
                return false;
            }

            @Override
            public void detach() {

            }

            @Override
            public boolean draining() {
                return false;
            }

            @Override
            public void setDrain(boolean b) {

            }

            @Override
            public EndpointState getLocalState() {
                return null;
            }

            @Override
            public EndpointState getRemoteState() {
                return null;
            }

            @Override
            public ErrorCondition getCondition() {
                return null;
            }

            @Override
            public void setCondition(ErrorCondition errorCondition) {

            }

            @Override
            public ErrorCondition getRemoteCondition() {
                return null;
            }

            @Override
            public void free() {

            }

            @Override
            public void open() {

            }

            @Override
            public void close() {

            }

            @Override
            public void setContext(Object o) {

            }

            @Override
            public Object getContext() {
                return null;
            }
        }

        Sender sender = new TestSender("address");
        Sender sender2 = new TestSender(null);

        Receiver recv = new TestReceiver("address");
        Receiver recv2 = new TestReceiver(null);

        assertEquals("address", SubscriptionHandler.getAddress(recv));
        assertEquals(null, SubscriptionHandler.getAddress(recv2));

        assertEquals("address", SubscriptionHandler.getAddress(sender));
        assertEquals(null, SubscriptionHandler.getAddress(sender2));
    }
}