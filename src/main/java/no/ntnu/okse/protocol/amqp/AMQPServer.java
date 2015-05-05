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
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Most of this code is from the qpid-proton-demo (https://github.com/rhs/qpid-proton-demo) by Rafael Schloming
 * Created by kradalby on 24/04/15.
 *
 */
public class AMQPServer extends BaseHandler {

    private class MessageStore {

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

    private byte[] nextTag() {
        return String.format("%s", tag++).getBytes();
    }

//    private int send(String address) {
//        return send(address, null);
//    }

    private int send(String address, Sender snd) {
        if (snd == null) {
            SubscriptionHandler.Routes<Sender> routes = subscriptionHandler.getOutgoing(address);
            snd = routes.choose();
            if (snd == null) {
                return 0;
            }
        }
        log.debug("Fetched this sender: " + snd.toString());

        int count = 0;
        while (snd.getCredit() > 0 && snd.getQueued() < 1024) {
            MessageBytes mb = messages.get(address);
            if (mb == null) {
                snd.drained();
                return count;
            }
            log.debug(String.format("Preparing to send: %s", mb.toString()));
            Delivery dlv = snd.delivery(nextTag());
            byte[] bytes = mb.getBytes();
            snd.send(bytes, 0, bytes.length);
            dlv.settle();
            count++;
            if (!quiet) {
                log.debug(String.format("Sent message(%s): %s to %s", address, mb.toString(), snd.toString()));
            }
        }

        return count;
    }

    private int send(String address) {

        List<Sender> sendersOnTopic = subscriptionHandler.getOutgoing(address).getRoutes();

        int count = 0;
        MessageBytes mb = messages.get(address);
        for (Sender snd : sendersOnTopic) {
            //while (snd.getCredit() > 0 && snd.getQueued() < 1024) {
            if (mb == null) {
                snd.drained();
                return count;
            }
            log.debug(String.format("Preparing to send: %s", mb.toString()));
            Delivery dlv = snd.delivery(nextTag());

            byte[] bytes = mb.getBytes();
            snd.send(bytes, 0, bytes.length);
            AMQProtocolServer.getInstance().incrementTotalMessagesSent();

            dlv.disposition(Accepted.getInstance());
            dlv.settle();

            count++;
            if (!quiet) {
                log.debug(String.format("Sent message(%s): %s to %s", address, mb.toString(), snd.toString()));
            }
            //}
        }

        return count;
    }

    public void addMessageToQueue(no.ntnu.okse.core.messaging.Message message) {
        Message msg = convertOkseMessageToAMQP(message);

        MessageBytes mb = convertAMQPMessageToMessageBytes(msg);

        String address = message.getTopic().getFullTopicString();
        messages.put(address, mb);
        queue.add(address);

        log.debug("Added message on topic: " + address + " to queue");

        log.debug("The first message in the queue is currently: " + queue.peek());

        AMQProtocolServer.getInstance().getDriver().wakeUp();

    }

    public MessageBytes convertAMQPMessageToMessageBytes(Message msg) {

        /*int bytes = 0;
        for (Method m : msg.getClass().getMethods()) {
            if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
                Object r = null;
                try {
                    r = m.invoke(msg);
                    bytes += r.toString().getBytes().length;
                } catch (IllegalAccessException e) {
                    //e.printStackTrace();
                } catch (InvocationTargetException e) {
                    //e.printStackTrace();
                } catch (NullPointerException e) {
                    //e.printStackTrace();
                }
            }
        }*/
        /*ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.writeChars(msg.getBody().toString());
            dout.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] storingData = out.toByteArray();*/


        //System.out.println("Totalt antall bytes: " + storingData.length);
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
        System.out.println("Totalt antall bytes from guestimate int: " + guestimateByteSize);
        int encoded;

       /* if(msg.getAddress().getBytes() != null && msg.getSubject().getBytes() != null ){
            test = msg.getAddress().getBytes().length + msg.getSubject().getBytes().length + body.getBytes().length;
        }

        System.out.println("Dette er test: " + test);
        System.out.println(msg.getBody().toString());*/

        byte[] buffer = new byte[guestimateByteSize];
        System.out.println("This is buffer.length: " + buffer.length);
        while (true) {
            try {
                log.debug("While loop: encode block, buffer length: " + buffer.length);
                encoded = msg.encode(buffer, 0, buffer.length);
                break;
            } catch (java.nio.BufferOverflowException e) {
                buffer = new byte[buffer.length+1];
            }
        }
        MessageBytes mb = new MessageBytes(buffer);
        System.out.println("This is mb.length: " + mb.getBytes().length);
        return mb;
    }

    public Message convertOkseMessageToAMQP(no.ntnu.okse.core.messaging.Message message) {
        Message msg = Message.Factory.create();

        Section body = new AmqpValue(message.getMessage());

        msg.setAddress("127.0.0.1/" + message.getTopic().getFullTopicString());
        msg.setSubject("bang");
        msg.setBody(body);
        return msg;
    }

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
                Address address = new Address(msg.getAddress());

                Topic t = TopicService.getInstance().getTopic(address.getName());

                AmqpValue amqpMessageBodyString = (AmqpValue)msg.getBody();

                // Add straight to AMQP queue
                try {
                    messages.put(address.getName(), mb);
                    queue.put(address.getName());
                } catch (InterruptedException e) {
                    AMQProtocolServer.getInstance().incrementTotalErrors();
                    log.error("Got interrupted: " + e.getMessage());
                }


                if (t != null) {
                    no.ntnu.okse.core.messaging.Message message =
                            new no.ntnu.okse.core.messaging.Message((String)amqpMessageBodyString.getValue(), t, null, AMQProtocolServer.getInstance().getProtocolServerType());
                    message.setOriginProtocol(AMQProtocolServer.getInstance().getProtocolServerType());

                    MessageService.getInstance().distributeMessage(message);
                    AMQProtocolServer.getInstance().incrementTotalMessagesRecieved();
                    log.debug(String.format("Got and distributed message(%s): %s from %s", address, message, rcv.toString()));

                }


            }
        }
    }
}