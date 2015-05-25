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

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example/test of the java Messenger/Message API.
 * Based closely qpid src/proton/examples/messenger/py/send.py
 *
 * @author mberkowitz@sf.org
 * @since 8/4/2013
 */
public class AMQPSendMassive {

    private static Logger tracer = Logger.getLogger("proton.example");
    private String address = "amqp://127.0.0.1";
    private String subject;
    private int runs;
    private String body;
    private String[] bodies = new String[]{"Hello World!"};

    private static void usage() {
        System.err.println("Usage: send [-a ADDRESS] [-s SUBJECT] MSG+");
        System.exit(2);
    }

    private AMQPSendMassive(String args[]) {
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            if (arg.startsWith("-")) {
                if ("-a".equals(arg)) {
                    address = args[i++];
                } else if ("-s".equals(arg)) {
                    subject = args[i++];
                } else if ("-r".equals(arg)) {
                    runs = Integer.parseInt(args[i++]);
                } else if ("-b".equals(arg)) {
                    body = args[i++];
                } else {
                    System.err.println("unknown option " + arg);
                    usage();
                }
            } else {
                --i;
                break;
            }
        }

        if (i != args.length) {
            bodies = Arrays.copyOfRange(args, i, args.length);
        }
    }

    private void run() {
        try {
            Messenger mng = Messenger.Factory.create();
            mng.start();
            Message msg = Message.Factory.create();

            msg.setAddress(address);
            System.out.println(msg.getAddress());
            if (subject != null) msg.setSubject(subject);
            System.out.println(msg.getSubject());
            System.out.println(body);
            msg.setBody(new AmqpValue(body));
            System.out.println(msg.getBody());
            int c = 0;
            while (c < runs) {
                //Thread.sleep(50);
                mng.put(msg);
                mng.send();
                c++;
            }
            mng.stop();
        } catch (Exception e) {
            tracer.log(Level.SEVERE, "proton error", e);
        }
    }

    public static void main(String args[]) {
        AMQPSendMassive o = new AMQPSendMassive(args);
        o.run();
    }
}
