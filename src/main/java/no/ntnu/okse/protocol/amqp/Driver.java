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

import org.apache.log4j.Logger;
import org.apache.qpid.proton.engine.BaseHandler;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Handler;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Most of this code is from the qpid-proton-demo (https://github.com/rhs/qpid-proton-demo) by Rafael Schloming
 * Created by kradalby on 24/04/15.
 */
public class Driver extends BaseHandler {

    final private Collector collector;
    final private Handler[] handlers;
    final private Selector selector;
    private static Logger log;
    private boolean _running;
    private Acceptor acceptor;

    public Driver(Collector collector, Handler ... handlers) throws IOException {
        this.collector = collector;
        this.handlers = handlers;
        this.selector = Selector.open();
        log = Logger.getLogger(Driver.class.getName());
    }

    public void listen(String host, int port) throws IOException {
        //acceptor = new Acceptor(host, port);
        new Acceptor(host, port);
    }

    public void run() throws IOException {
        if (!_running) {
            _running = true;
        }
        while (true) {

            for (Handler h : handlers) {
                if (h instanceof AMQPServer) {
                    log.debug("Executing sendNextMessagesInQueue");
                    ((AMQPServer) h).sendNextMessagesInQueue();
                }
            }

            log.debug(collector.toString());
            processEvents();


            // I don't know if there is a better way to do this, but
            // the only way canceled selection keys are removed from
            // the key set is via a select operation, so we do this
            // first to figure out whether we should exit. Without
            // this we would block indefinitely when there are only
            // cancelled keys remaining.
            selector.selectNow();
            if (selector.keys().isEmpty()) {
                log.info("No sockets open - closing the selector");
                selector.close();
                return;
            }

            selector.selectedKeys().clear();
            selector.select();
            for (SelectionKey key : selector.selectedKeys()) {
                Selectable selectable = (Selectable) key.attachment();
                selectable.selected();
            }

            if (!_running) {
                Iterator keys = selector.keys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    key.cancel();
                    key.channel().close();
                }
            }

        }
    }

    public void processEvents() {
        while (_running) {
            Event ev = collector.peek();
            if (ev == null) break;
            log.debug("Dispatching event of type: " + ev.getType().name());
            ev.dispatch(this);
            for (Handler h : handlers) {
                ev.dispatch(h);
                if (h instanceof AMQPServer) {
                    log.debug("Executing sendNextMessagesInQueue in processEvents");
                    ((AMQPServer) h).sendNextMessagesInQueue();
                }
            }
            collector.pop();
        }
    }

    //Driver.stop() method, for stoping the driver and closing the socket
    public void stop() {
        _running = false;
        selector.wakeup();
    }

    @Override
    public void onTransport(Event evt) {
        Transport transport = evt.getTransport();
        ChannelHandler ch = (ChannelHandler) transport.getContext();
        ch.selected();
    }

    @Override
    public void onConnectionLocalOpen(Event evt) {
        Connection conn = evt.getConnection();
        if (conn.getRemoteState() == EndpointState.UNINITIALIZED) {
            try {
                new Connector(conn);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private interface Selectable {
        void selected() throws IOException;
    }

    private class Acceptor implements Selectable {

        final private ServerSocketChannel socket;
        final private SelectionKey key;

        Acceptor(String host, int port) throws IOException {
            socket = ServerSocketChannel.open();
            socket.configureBlocking(false);
            socket.bind(new InetSocketAddress(host, port));
            socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            key = socket.register(selector, SelectionKey.OP_ACCEPT, this);
        }

        public void selected() throws IOException {
            SocketChannel sock = socket.accept();
            log.debug("ACCEPTED: " + sock);
            Connection conn = Connection.Factory.create();
            conn.collect(collector);
            Transport transport = Transport.Factory.create();
            Sasl sasl = transport.sasl();
            sasl.setMechanisms("ANONYMOUS");
            sasl.server();
            sasl.done(Sasl.PN_SASL_OK);
            transport.bind(conn);
            new ChannelHandler(sock, SelectionKey.OP_READ, transport);
        }
    }


    private class ChannelHandler implements Selectable {

        final SocketChannel socket;
        final SelectionKey key;
        final Transport transport;

        ChannelHandler(SocketChannel socket, int ops, Transport transport) throws IOException {
            this.socket = socket;
            socket.configureBlocking(false);
            key = socket.register(selector, ops, this);
            this.transport = transport;
            transport.setContext(this);
        }

        boolean update() {
            if (socket.isConnected()) {
                int c = transport.capacity();
                int p = transport.pending();
                if (key.isValid()) {
                    key.interestOps((c != 0 ? SelectionKey.OP_READ : 0) |
                            (p > 0 ? SelectionKey.OP_WRITE : 0));
                }
                if (c < 0 && p < 0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        public void selected() {
            if (!key.isValid()) { return; }

            try {
                if (key.isConnectable()) {
                    log.debug("CONNECTED: " + socket);
                    socket.finishConnect();
                }

                if (key.isReadable()) {
                    int c = transport.capacity();
                    if (c > 0) {
                        ByteBuffer tail = transport.tail();
                        int n = socket.read(tail);
                        if (n > 0) {
                            try {
                                transport.process();
                            } catch (TransportException e) {
                                e.printStackTrace();
                            }
                        } else if (n < 0) {
                            transport.close_tail();
                        }
                    }
                }

                if (key.isWritable()) {
                    int p = transport.pending();
                    if (p > 0) {
                        ByteBuffer head = transport.head();
                        int n = socket.write(head);
                        if (n > 0) {
                            transport.pop(n);
                        } else if (n < 0) {
                            transport.close_head();
                        }
                    }
                }

                if (update()) {
                    transport.unbind();
                    log.debug("CLOSING: " + socket);
                    socket.close();
                }
            } catch (IOException e) {
                transport.unbind();
                log.debug(String.format("CLOSING(%s): %s", e, socket));
                try {
                    socket.close();
                } catch (IOException e2) {
                    throw new RuntimeException(e2);
                }
            }

        }

    }

    private static Transport makeTransport(Connection conn) {
        Transport transport = Transport.Factory.create();
        Sasl sasl = transport.sasl();
        sasl.setMechanisms("ANONYMOUS");
        sasl.client();
        transport.bind(conn);
        return transport;
    }

    private class Connector extends ChannelHandler {

        Connector(Connection conn) throws IOException {
            super(SocketChannel.open(), SelectionKey.OP_CONNECT, makeTransport(conn));
            log.debug("CONNECTING: " + conn.getHostname());
            socket.connect(new InetSocketAddress(conn.getHostname(), 5672));
        }
    }

    public void wakeUp() {
        log.debug("Waking up the selector to get out the next messages from the queue");
        selector.wakeup();
    }

}
