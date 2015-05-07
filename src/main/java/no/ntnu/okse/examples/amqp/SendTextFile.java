package no.ntnu.okse.examples.amqp;

/**
 * Created by Trond Walleraunet on 20.04.2015.
 */

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

import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
35  * Example/test of the java Messenger/Message API.
36  * Based closely qpid src/proton/examples/messenger/py/send.py
37  * @author mberkowitz@sf.org
38  * @since 8/4/2013
39  */
public class Send {

    private static Logger tracer = Logger.getLogger("proton.example");
    private String address = "amqp://127.0.0.1/test";
    private String subject = "bang";
    private Messenger mng;
    private Message msg;

    private static void usage() {
        System.err.println("Usage: send [-a ADDRESS] [-s SUBJECT] MSG+");
        System.exit(2);
    }

    private String[] readfile() {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader("d:\\test.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            List<String> lines = new ArrayList<String>();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();
            return lines.toArray(new String[lines.size()]);

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOException");
            e.printStackTrace();
        }
        return null;
    }

    private Send(String args[]) {
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            if (arg.startsWith("-")) {
                if ("-a".equals(arg)) {
                    address = args[i++];
                } else if ("-s".equals(arg)) {
                    subject = args[i++];
                } else {
                    System.err.println("unknown option " + arg);
                    usage();
                }
            } else {
                --i;
                break;
            }
        }
    }

    private void run() {
        try {
            mng = Messenger.Factory.create();
            mng.start();
            msg = Message.Factory.create();
            msg.setAddress(address);
            if (subject != null) msg.setSubject(subject);
            for (String body : readfile()) {
                msg.setBody(new AmqpValue(body));
                mng.put(msg);
            }
            mng.send();
            mng.stop();
        } catch (Exception e) {
            tracer.log(Level.SEVERE, "proton error", e);
        }
    }

    public static void main(String args[]) {
        Send o = new Send(args);
        o.run();
    }
}
