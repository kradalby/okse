package no.ntnu.okse.examples.amqp;

/*
   2  *
   3  * Licensed to the Apache Software Foundation (ASF) under one
   4  * or more contributor license agreements.  See the NOTICE file
   5  * distributed with this work for additional information
   6  * regarding copyright ownership.  The ASF licenses this file
   7  * to you under the Apache License, Version 2.0 (the
   8  * "License"); you may not use this file except in compliance
   9  * with the License.  You may obtain a copy of the License at
  10  *
  11  *   http://www.apache.org/licenses/LICENSE-2.0
  12  *
  13  * Unless required by applicable law or agreed to in writing,
  14  * software distributed under the License is distributed on an
  15  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  16  * KIND, either express or implied.  See the License for the
  17  * specific language governing permissions and limitations
  18  * under the License.
  19  *
  20  */

import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.impl.MessengerImpl;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 35  * Example/test of the java Messenger/Message API.
 36  * Based closely qpid src/proton/examples/messenger/py/recv.py
 37  * @author mberkowitz@sf.org
 38  * @since 8/4/2013
 39  */
public class Recv {
    private static Logger tracer = Logger.getLogger("proton.example");
    private boolean verbose = false;
    private int maxct = 0;
    private List<String> addrs = new ArrayList<String>();

    private static void usage() {
        System.err.println("Usage: recv [-v] [-n MAXCT] [-a ADDRESS]*");
        System.exit(2);
    }

    private Recv(String args[]) {
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
            //addrs.add("amqp://~0.0.0.0");
            addrs.add("amqp://~127.0.0.1/test");
            //addrs.add("amqp://78.91.8.191");
            //System.out.println("adding address " + addrs.get(0));
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
             Messenger mng = new MessengerImpl();
             mng.start();
             for (String a : addrs) {
                 mng.subscribe(a);
             }
             int ct = 0;
             boolean done = false;
             while (!done) {
                 mng.recv();
                 while (mng.incoming() > 0) {
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
         Recv o = new Recv(args);
         o.run();
     }
}