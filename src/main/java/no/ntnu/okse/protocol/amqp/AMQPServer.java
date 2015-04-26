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
import org.apache.qpid.proton.engine.BaseHandler;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.impl.Address;

import java.util.*;


/**
 * Most of this code is from the qpid-proton-demo (https://github.com/rhs/qpid-proton-demo) by Rafael Schloming
 * Created by kradalby on 24/04/15.
 *
 */
public class AMQPServer extends BaseHandler {

    private class MessageStore {

        Map<String,Deque<MessageBytes>> messages = new HashMap<String,Deque<MessageBytes>>();

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
            if (queue == null) { return null; }
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

    public AMQPServer(SubscriptionHandler subscriptionHandler, boolean quiet) {
        this.subscriptionHandler = subscriptionHandler;
        this.quiet = quiet;
        log = Logger.getLogger(AMQPServer.class.getName());
    }

    private byte[] nextTag() {
        return String.format("%s", tag++).getBytes();
    }

    private int send(String address) {
        return send(address, null);
    }

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
            MessageBytes msg = messages.get(address);
            if (msg == null) {
                snd.drained();
                return count;
            }
            log.debug(String.format("Preparing to send: %s", msg.toString()));
            Delivery dlv = snd.delivery(nextTag());
            byte[] bytes = msg.getBytes();
            snd.send(bytes, 0, bytes.length);
            dlv.settle();
            count++;
            if (!quiet) {
                log.debug(String.format("Sent message(%s): %s to %s", address, msg.toString(), snd.toString()));
            }
        }

        return count;
    }

//    private int send(String address, Sender snd) {
//        Router.Routes<Sender> routes = router.getOutgoing(address);
//        List<Sender> senders;
//        senders = routes.getAllRoutes();
//        System.out.println(senders.toString());
//        //snd = routes.choose();
//        if (senders == null) {
//            return 0;
//        }
//
//        int count = 0;
//        MessageBytes msg = messages.get(address);
//        for (Sender sender : senders) {
//            while (sender.getCredit() > 0 && sender.getQueued() < 1024) {
//                System.out.println(msg.toString());
//                System.out.println(sender.getRemoteSource().getAddress());
//                System.out.println(sender.getRemoteTarget().getAddress());
//                System.out.println(sender.getSource().getAddress());
//                System.out.println(sender.getTarget().getAddress());
//                if (msg == null) {
//                    sender.drained();
//                    break;
//                }
//                Delivery dlv = sender.delivery(nextTag());
//                byte[] bytes = msg.getBytes();
//                sender.send(bytes, 0, bytes.length);
//                dlv.settle();
//                count++;
//                if (!quiet) {
//                    log.debug(String.format("Sent message(%s): %s", address, msg));
//                }
//            }
//        }
//
//        return count;
//    }

    @Override
    public void onLinkFlow(Event evt) {
        Link link = evt.getLink();
        if (link instanceof Sender) {
            Sender snd = (Sender) link;
            send(subscriptionHandler.getAddress(snd), snd);
        }
    }

    public void addMessageToQueue(no.ntnu.okse.core.messaging.Message message) {
        MessageBytes msg = new MessageBytes(message.getMessage().getBytes());
        String address = message.getTopic().getFullTopicString();
        messages.put(address, msg);
        log.debug("Added message on topic: " + address + " to queue");
        send(address);
//
//            Receiver rcv = (Receiver) link;
//            if (!dlv.isPartial()) {
//                byte[] bytes = new byte[dlv.pending()];
//                rcv.recv(bytes, 0, bytes.length);
//                String address = router.getAddress(rcv);
//                MessageBytes messageBytes = new MessageBytes(bytes);
//                messages.put(address, messageBytes);
//                dlv.disposition(Accepted.getInstance());
//                dlv.settle();
//                if (!quiet) {
//                    log.debug(String.format("Got message(%s): %s", address, messageBytes));
//                }
//                send(address);
//            }
//        }
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
                System.out.println(rcv.getRemoteSource().getAddress());
                System.out.println(rcv.getRemoteTarget().getAddress());
                System.out.println(rcv.getSource().getAddress());
                System.out.println(rcv.getTarget().getAddress());
                String derp = subscriptionHandler.getAddress(rcv);
                System.out.println("HERE: " + derp);
                Message msg = Message.Factory.create();
                msg.decode(bytes, 0, bytes.length);
                Address address = new Address(msg.getAddress());

                TopicService.getInstance().addTopic(address.getName());

                Topic t = TopicService.getInstance().getTopic(address.getName());

                no.ntnu.okse.core.messaging.Message message =
                        new no.ntnu.okse.core.messaging.Message(msg.getBody().toString(), t, null);
                message.setOriginProtocol(AMQProtocolServer.getInstance().getProtocolServerType());

                MessageService.getInstance().distributeMessage(message);
                AMQProtocolServer.getInstance().incrementTotalMessages();

                System.out.println(address.getName());
                System.out.println(msg.getBody().toString());

                dlv.disposition(Accepted.getInstance());
                dlv.settle();
                log.debug(String.format("Got message(%s): %s from %s", address, message, rcv.toString()));

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
//                messages.put(address, message);
//                dlv.disposition(Accepted.getInstance());
//                dlv.settle();
//                if (!quiet) {
//                    System.out.println(String.format("Got message(%s): %s", address, message));
//                }
//                send(address);
//            }
//        }
//    }


}
