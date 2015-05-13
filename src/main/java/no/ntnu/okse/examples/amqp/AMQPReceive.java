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

package no.ntnu.okse.examples.amqp;

/**
 * Created by kradalby on 22/04/15.
 */


import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.impl.MessengerImpl;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AMQPReceive {
    private static Logger tracer = Logger.getLogger("proton.example");
    private boolean verbose = false;
    private int maxct = 555555;
    private List<String> addrs = new ArrayList<String>();

    private static void usage() {
        System.err.println("Usage: recv [-v] [-n MAXCT] [-a ADDRESS]*");
        System.exit(2);
    }

    private AMQPReceive(String args[]) {
        //System.out.println(args);
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            if (arg.startsWith("-")) {
                if ("-v".equals(arg)) {
                    verbose = true;
                } else if ("-a".equals(arg)) {
                    addrs.add(args[i++]);
                } else if ("-n".equals(arg)) {
                    maxct = Integer.valueOf(args[i++]);
                } else {
                    System.err.println("unknown option " + arg);
                    usage();
                }
            } else {
                usage();
            }
        }
        if (addrs.size() == 0) {
            addrs.add("amqp://localhost:61050/bang");
        }
    }

    private static String safe(Object o) {
        return String.valueOf(o);
    }

    private void print(int i, Message msg) {
        StringBuilder b = new StringBuilder("message: ");
        b.append(i).append("\n");
        b.append("Address: ").append(msg.getAddress()).append("\n");
        b.append("Subject: ").append(msg.getSubject()).append("\n");
        if (verbose) {
            b.append("Props:     ").append(msg.getProperties()).append("\n");
            b.append("App Props: ").append(msg.getApplicationProperties()).append("\n");
            b.append("Msg Anno:  ").append(msg.getMessageAnnotations()).append("\n");
            b.append("Del Anno:  ").append(msg.getDeliveryAnnotations()).append("\n");
        } else {
            ApplicationProperties p = msg.getApplicationProperties();
            String s = (p == null) ? "null" : safe(p.getValue());
            b.append("Headers: ").append(s).append("\n");
        }
        b.append(msg.getBody()).append("\n");
        b.append("END").append("\n");
        System.out.println(b.toString());
    }

    private void run() {
        try {
            Messenger mng = Messenger.Factory.create();
            mng.start();
            for (String a : addrs) {
                mng.subscribe(a);
            }
            int ct = 0;
            boolean done = false;
            while (!done) {
                mng.recv();
                while (mng.incoming() > 0) {
                    System.out.println("derp");
                    Message msg = mng.get();
                    ++ct;
                    print(ct, msg);
                    if (maxct > 0 && ct >= maxct) {
                        done = true;
                        break;
                    }
                }
            }
            mng.stop();
        } catch (Exception e) {
            tracer.log(Level.SEVERE, "proton error", e);
        }
    }

    public static void main(String args[]) {
        AMQPReceive o = new AMQPReceive(args);
        o.run();
    }
}
