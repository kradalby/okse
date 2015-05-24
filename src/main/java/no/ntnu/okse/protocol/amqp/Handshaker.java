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

import org.apache.qpid.proton.engine.*;

/**
 * This code is a heavily modified version of the qpid-proton-demo (https://github.com/rhs/qpid-proton-demo) by Rafael Schloming
 * Created by kradalby on 24/04/15.
 */
public class Handshaker extends BaseHandler {


    @Override
    public void onConnectionRemoteOpen(Event evt) {
        Connection conn = evt.getConnection();
        if (conn.getLocalState() == EndpointState.UNINITIALIZED) {
            conn.open();
        }
    }

    @Override
    public void onSessionRemoteOpen(Event evt) {
        Session ssn = evt.getSession();
        if (ssn.getLocalState() == EndpointState.UNINITIALIZED) {
            ssn.open();
        }
    }

    @Override
    public void onLinkRemoteOpen(Event evt) {
        Link link = evt.getLink();
        if (link.getLocalState() == EndpointState.UNINITIALIZED) {
            link.setSource(link.getRemoteSource());
            link.setTarget(link.getRemoteTarget());
            link.open();
        }
    }

    @Override
    public void onConnectionRemoteClose(Event evt) {
        Connection conn = evt.getConnection();
        if (conn.getLocalState() != EndpointState.CLOSED) {
            conn.close();
        }
    }

    @Override
    public void onSessionRemoteClose(Event evt) {
        Session ssn = evt.getSession();
        if (ssn.getLocalState() != EndpointState.CLOSED) {
            ssn.close();
        }
    }

    @Override
    public void onLinkRemoteClose(Event evt) {
        Link link = evt.getLink();
        if (link.getLocalState() != EndpointState.CLOSED) {
            link.close();
        }
    }


}
