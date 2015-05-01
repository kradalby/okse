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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.instrument.Instrumentation;
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

            System.out.println(dlv.getLink().getRemoteSource().getAddress());
            System.out.println(dlv.getLink().getRemoteTarget().getAddress());

            byte[] bytes = mb.getBytes();
            snd.send(bytes, 0, bytes.length);

            System.out.println(bytes.length);
            System.out.println(snd.current());
            System.out.println(snd.current() == dlv);

            System.out.println(snd.getSession().getConnection().getHostname());
            System.out.println(snd.getSession().getConnection().getRemoteHostname());
            System.out.println(snd.getSession().getConnection().getRemoteProperties());


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

        //send(address);

        System.out.println(message.getMessage());
    }

    public static MessageBytes convertAMQPMessageToMessageBytes(Message msg) {
        int encoded;
        byte[] buffer = new byte[1];
        while (true) {
            try {
                encoded = msg.encode(buffer, 0, buffer.length);
                break;
            } catch (java.nio.BufferOverflowException e) {
                buffer = new byte[buffer.length+1];
            }
        }
        MessageBytes mb = new MessageBytes(buffer);
        return mb;
    }

    public static Message convertOkseMessageToAMQP(no.ntnu.okse.core.messaging.Message message) {
        Message msg = Message.Factory.create();

        Section body = new AmqpValue(message.getMessage());

        msg.setAddress("127.0.0.1/" + message.getTopic().getFullTopicString());
        msg.setSubject("bang");
        msg.setBody(body);
        System.out.println(msg.getAddress());
        System.out.println(msg.getSubject());
        System.out.println(msg.getBody());
        return msg;
    }

    public void sendNextMessageInQueue() {
        try {
            if (queue.size() > 0) {
                send(queue.take());
                System.out.println("queue and shit");
            }
        } catch (InterruptedException e) {
            AMQProtocolServer.getInstance().incrementTotalErrors();
            log.error("This happened: " + e.getMessage());
        }
    }

    @Override
    public void onLinkFlow(Event evt) {
        Link link = evt.getLink();
        if (link instanceof Sender) {
            Sender snd = (Sender) link;
            send(subscriptionHandler.getAddress(snd), snd);
            AMQProtocolServer.getInstance().getTotalRequests();
        }
    }

    @Override
    public void onDelivery(Event event) {
        log.debug("I got a delivery");
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

                System.out.println(bytes.toString());
                System.out.println(bytes.length);

                //System.out.println(rcv.getRemoteSource().getAddress());
                //System.out.println(rcv.getRemoteTarget().getAddress());
                //System.out.println(rcv.getSource().getAddress());
                //System.out.println(rcv.getTarget().getAddress());

                Message msg = Message.Factory.create();
                msg.decode(bytes, 0, bytes.length);
                Address address = new Address(msg.getAddress());

                MessageBytes mb = convertAMQPMessageToMessageBytes(msg);
                MessageBytes mb2 = new MessageBytes(bytes);

                System.out.println(msg.getAddress());

                System.out.println("This shit: " + mb.toString());
                System.out.println("This shit: " + mb2.toString());


                System.out.println(msg.getAddress());
                System.out.println(msg.getBody());
                System.out.println(msg.getSubject());

                Topic t = TopicService.getInstance().getTopic(address.getName());

                if (t != null) {
                    no.ntnu.okse.core.messaging.Message message =
                            //new no.ntnu.okse.core.messaging.Message(msg.getBody().toString(), t, null);
                            new no.ntnu.okse.core.messaging.Message(msg.getBody().toString(), t, null,"AMQP");
                    message.setOriginProtocol(AMQProtocolServer.getInstance().getProtocolServerType());

                    MessageService.getInstance().distributeMessage(message);
                    AMQProtocolServer.getInstance().incrementTotalMessages();
                    log.debug(String.format("Got and distributed message(%s): %s from %s", address, message, rcv.toString()));

                    //addMessageToQueue(message);
                }


            }
        }
    }
}

//    @Override
//    public void onDelivery(Event evt) {
//        Delivery dlv = evt.getDelivery();
//        Link link = dlv.getLink();
//        if (link instanceof Sender) {
//            dlv.settle();
//        } else {
//            Receiver rcv = (Receiver) link;
//            if (!dlv.isPartial()) {
//                byte[] bytes = new byte[dlv.pending()];
//                rcv.recv(bytes, 0, bytes.length);
//                String address = subscriptionHandler.getAddress(rcv);
//                MessageBytes message = new MessageBytes(bytes);
//                System.out.println(message.toString());
//                System.out.println(message.getBytes());
//                messages.put(address, message);
//                dlv.disposition(Accepted.getInstance());
//                dlv.settle();
//                if (!quiet) {
//                    log.debug(String.format("Got message(%s): %s from %s", address, message, rcv.toString()));
//                }
//                send(address);
//            }
//        }
//    }

/**
 * Send a message using AMQPServer
 *
 * @param message An instance of Message containing the required data to distribute a message.
 */
/* sendMessage useing MesengerImpl
    @Override
    public void sendMessage(Message message) {
        SubscriptionService subservice = SubscriptionService.getInstance();
        //Mulig vi må lage en egen bindings klasse for filtrering (* og #)
        HashSet<Subscriber> subscribers = subservice.getAllSubscribersForTopic(message.getTopic().getFullTopicString());
        subscribers.forEach(s -> {
            if (s.getOriginProtocol().equals(this.getProtocolServerType())) {
                    //Do the shait
                    log.info("[AMQP] Sending message to " +s.getHost() + " " + message);
                    try {
                        //Deprecated, useing factory insted
                        //Messenger mng = new MessengerImpl();
                        Messenger mng = Messenger.Factory.create();
                        mng.start();
                        //Deprecate, useing factory insted
                        //MessageImpl msg = new MessageImpl();
                        org.apache.qpid.proton.message.Message msg = org.apache.qpid.proton.message.Message.Factory.create();
                        msg.setAddress(s.getHost());
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
            });

    }

*/
