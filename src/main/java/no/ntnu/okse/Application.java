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

package no.ntnu.okse;

import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.db.DB;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import no.ntnu.okse.web.Server;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;


/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 25/02/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class Application {

    private static Logger log;
    public static CoreService cs;
    public static Server webserver;

    /**
     * Main method for the OKSE Message Broker
     * Used to initate the complete application (CoreService and WebServer)
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        log = Logger.getLogger(Application.class.getName());

        File dbFile = new File("okse.db");

        if (!dbFile.exists()) {
            DB.initDB();
            log.info("okse.db initiated");
        } else {
            log.info("okse.db exists");
        }

        // Initialize system threads
        webserver = new Server();
        cs = new CoreService();

        // Add ProtocolServers to CoreService
        cs.addProtocolServer(WSNotificationServer.getInstance());


        // Start the admin console
        webserver.run();

        // Start the CoreService
        cs.start();

        try {
            cs.join();
        } catch (InterruptedException e) {
            log.trace(e.getStackTrace());
        }
    }
}