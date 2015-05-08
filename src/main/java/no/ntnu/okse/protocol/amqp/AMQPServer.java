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

import no.ntnu.okse.core.messaging.MessageService;
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.topic.TopicService;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.log4j.Logger;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.engine.BaseHandler;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.impl.Address;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * This code is a heavily modified version of the qpid-proton-demo (https://github.com/rhs/qpid-proton-demo) by Rafael Schloming
 * Created by kradalby on 24/04/15.
 *
 */
public class AMQPServer extends BaseHandler {

    /**
     * Internal message queue for AMQP.
     * Basically one Deque with bytes for each
     * Queue/Topic
     */

    private static class MessageStore {

        Map<String, Deque<MessageBytes>> messages = new HashMap<String, Deque<MessageBytes>>();

        void put(String address, MessageBytes messageBytes) {
            Deque<MessageBytes> queue = messages.get(address);
            if (queue == null) {
                queue = new ArrayDeque<MessageBytes>();
                messages.put(address, queue);
            }
            queue.add(messageBytes);
        }

        MessageBytes get(String address) {
            Deque<MessageBytes> queue = messages.get(address);
            if (queue == null) {
                return null;
            }
            MessageBytes msg = queue.remove();
            if (queue.isEmpty()) {
                messages.remove(address);
            }
            return msg;
        }
    }

    public static class TestMessageStore extends MessageStore {}

    public static TestMessageStore createMessageStoreFactory() {
        return new TestMessageStore();
    }

    final private MessageStore messages = new MessageStore();
    final private SubscriptionHandler subscriptionHandler;
    private static Logger log;
    private boolean quiet;
    private int tag = 0;
    private LinkedBlockingQueue<String> queue;

    public AMQPServer(SubscriptionHandler subscriptionHandler, boolean quiet) {
        this.subscriptionHandler = subscriptionHandler;
        this.quiet = quiet;
        log = Logger.getLogger(AMQPServer.class.getName());
        queue = new LinkedBlockingQueue<>();
    }

    /**
     * Get the tag for the next message as a byte array.
     * @return byte[]
     */
    private byte[] nextTag() {
        return String.format("%s", tag++).getBytes();
    }

    /**
     * Send a AMQP message with address and sender.
     * If sender is null, the sender will be chosen
     * at random from the queue for this address
     * @param address / topic / queue
     * @param snd Sender object or null
     * @return int of sent bytes
     */
    private int send(String address, Sender snd) {
        if (snd == null) {
            SubscriptionHandler.Routes<Sender> routes = subscriptionHandler.getOutgoing(address);
            snd = routes.choose();
            if (snd == null) {
                return 0;
            }
            // Queue based sending will have an initial snd == null, meaning that it is not a sub
            // and it is a message that is to be sent. Incrementing total sent here.
            AMQProtocolServer.getInstance().incrementTotalMessagesSent();
        }
        log.debug("Fetched this sender: " + snd.toString());

        int count = 0;
        while (snd.getCredit() > 0 && snd.getQueued() < 1024) {
            MessageBytes mb = messages.get(address);
            if (mb == null) {
                snd.drained();
                return count;
            }
            Delivery dlv = snd.delivery(nextTag());
            byte[] bytes = mb.getBytes();
            snd.send(bytes, 0, bytes.length);
            dlv.settle();
            count++;
        }

        return count;
    }

    /**
     * Send an AMQP message to the given address,
     * this method will choose either use Topic
     * or Queue based on the configuration of
     * OKSE.
     * @param address / topic / queue
     * @return int of bytes sent
     */
    private int send(String address) {
        int count = 0;

        if (AMQProtocolServer.getInstance().useQueue) {
            log.debug(String.format("Use Queue is set to: %b, using queue mode", AMQProtocolServer.getInstance().useQueue));
            return send(address, null);
        } else {
            log.debug(String.format("Use Queue is set to: %b, using topic mode", AMQProtocolServer.getInstance().useQueue));
            List<Sender> sendersOnTopic = subscriptionHandler.getOutgoing(address).getRoutes();

            MessageBytes mb = messages.get(address);
            for (Sender snd : sendersOnTopic) {
                if (mb == null) {
                    snd.drained();
                    return count;
                }
                Delivery dlv = snd.delivery(nextTag());

                byte[] bytes = mb.getBytes();
                snd.send(bytes, 0, bytes.length);
                AMQProtocolServer.getInstance().incrementTotalMessagesSent();

                dlv.disposition(Accepted.getInstance());
                dlv.settle();

                count++;
            }

        }

        return count;
    }

    /**
     * Convert a OKSE message to AMQP and add it the the message queue.
     * @param message
     */
    public void addMessageToQueue(no.ntnu.okse.core.messaging.Message message) {
        Message msg = convertOkseMessageToAMQP(message);

        MessageBytes mb = convertAMQPMessageToMessageBytes(msg);

        String address = message.getTopic();
        messages.put(address, mb);
        queue.add(address);

        log.debug("Added message on topic: " + address + " to queue");

        log.debug("The first message in the queue is currently: " + queue.peek());

        AMQProtocolServer.getInstance().getDriver().wakeUp();
    }

    /**
     * Convert a AMQP message object to a MessageBytes Object.
     * MessageBytes is basically a wrapper around a byte array.
     * @param msg
     * @return MessageBytes object
     */
    public static MessageBytes convertAMQPMessageToMessageBytes(Message msg) {


        byte[] buffer = gestimateMessageByteSize(msg);

        MessageBytes mb = new MessageBytes(buffer);
        System.out.println("This is mb.length: " + mb.getBytes().length);
        return mb;
    }

    /**
     * Get an AMQP message as a byte array.
     * This method uses qualified guessing to achieve its goal.
     * @param msg
     * @return byte[]
     */
    private static byte[] gestimateMessageByteSize(Message msg) {

        int guestimateByteSize = 0;
        if(msg.getBody().toString().length() != 0){
            guestimateByteSize += msg.getBody().toString().getBytes().length;
        }
        if(msg.getAddress().getBytes().length != 0){
            guestimateByteSize += msg.getAddress().getBytes().length;
        }
        if(msg.getSubject().getBytes().length != 0){
            guestimateByteSize += msg.getSubject().getBytes().length;
        }
        int encoded;

        byte[] buffer = new byte[guestimateByteSize];
        while (true) {
            try {
                //log.debug("While loop: encode block, buffer length: " + buffer.length);
                encoded = msg.encode(buffer, 0, buffer.length);
                break;
            } catch (java.nio.BufferOverflowException e) {
                buffer = new byte[buffer.length+1];
            }
        }

        return buffer;
    }

    /**
     * Convert a OKSE message to a AMQP Message.
     * @param message
     * @return AMQP message
     */
    public static Message convertOkseMessageToAMQP(no.ntnu.okse.core.messaging.Message message) {
        Message msg = Message.Factory.create();

        Section body = new AmqpValue(message.getMessage());

        msg.setAddress(AMQProtocolServer.getInstance().getHost() +"/" + message.getTopic());
        msg.setSubject("OKSE translated message");
        msg.setBody(body);
        return msg;
    }

    /**
     * Send the messages that are ready to go out.
     */
    public void sendNextMessagesInQueue() {
        try {
            if (queue.size() > 0) {
                while (queue.size() > 0) {
                    String messageTopic = queue.take();
                    send(messageTopic);
                    log.debug("Distributed messages with topic: " + messageTopic);
                }
            }
        } catch (InterruptedException e) {
            AMQProtocolServer.getInstance().incrementTotalErrors();
            log.error("Got interrupted: " + e.getMessage());
        }
    }

    @Override
    public void onLinkFlow(Event evt) {
        Link link = evt.getLink();
        if (link instanceof Sender) {
            Sender snd = (Sender) link;
            send(subscriptionHandler.getAddress(snd), snd);
            //AMQProtocolServer.getInstance().incrementTotalRequests();
        }
    }

    /**
     * onDelviery is triggered when the AMQP socket receives
     * a message. When the message is received it will create
     * a OKSE message and pass it into the MessageService.
     * It will also add AMQP messages back into the
     * internal AMQP queue to generate less overhead.
     * @param event
     */
    @Override
    public void onDelivery(Event event) {
        log.debug("Received AMQP message");
        Delivery dlv = event.getDelivery();
        Link link = dlv.getLink();
        if (link instanceof Sender) {
            dlv.settle();
        } else {
            Receiver rcv = (Receiver) link;
            if (!dlv.isPartial()) {
                byte[] bytes = new byte[dlv.pending()];
                rcv.recv(bytes, 0, bytes.length);
                dlv.disposition(Accepted.getInstance());
                dlv.settle();

                Message msg = Message.Factory.create();
                msg.decode(bytes, 0, bytes.length);

                MessageBytes mb = new MessageBytes(bytes);

                Address address = createAddress(msg.getAddress(), dlv);

                log.debug("Received a message with queue/topic: " + address.getName());


                // Add straight to AMQP queue
                try {
                    messages.put(address.getName(), mb);
                    queue.put(address.getName());
                } catch (InterruptedException e) {
                    AMQProtocolServer.getInstance().incrementTotalErrors();
                    log.error("Got interrupted: " + e.getMessage());
                }

                no.ntnu.okse.core.messaging.Message message = convertAMQPmessageToOkseMessage(msg, address);

                MessageService.getInstance().distributeMessage(message);

                log.debug(String.format("Got and distributed message(%s): %s from %s", address.getName(), message, rcv.toString()));

                AMQProtocolServer.getInstance().incrementTotalMessagesReceived();
                AMQProtocolServer.getInstance().incrementTotalRequests();

            }
        }
    }

    public static no.ntnu.okse.core.messaging.Message convertAMQPmessageToOkseMessage(Message AMQPMessage, Address address) {
        AmqpValue amqpMessageBodyString = (AmqpValue)AMQPMessage.getBody();

        no.ntnu.okse.core.messaging.Message okseMessage =
                new no.ntnu.okse.core.messaging.Message(
                        (String)amqpMessageBodyString.getValue(),
                        address.getName(),
                        null,
                        AMQProtocolServer.getInstance().getProtocolServerType()
                );

        okseMessage.setOriginProtocol(AMQProtocolServer.getInstance().getProtocolServerType());

        return okseMessage;
    }

    public static Address createAddress(String addr, Delivery dlv) {
        Address address = new Address(addr);

        // This handles an edgecase where client only sends
        // the topic as address, which causes the Address
        // object creation to fail.
        if (address.getName() == null || !address.getName().equals(dlv.getLink().getTarget().getAddress())) {
            address.setName(addr);
            address.setHost("");
        }

        return address;
    }



}
