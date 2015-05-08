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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.protocol.amqp;

import org.apache.qpid.proton.amqp.transport.*;
import org.apache.qpid.proton.engine.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.*;

public class SubscriptionHandlerTest {


    AMQProtocolServer ps = AMQProtocolServer.getInstance();

    @BeforeMethod
    public void setUp() throws Exception {
        ps.boot();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        ps.stopServer();
    }

    @Test
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