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

package no.ntnu.okse.core;

import no.ntnu.okse.protocol.ProtocolServer;
import no.ntnu.okse.protocol.wsn.WSNotificationServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.testng.Assert.*;

public class CoreServiceTest {

    CoreService cs;
    ProtocolServer testProtocolServer, testProtocolServer2;
    WSNotificationServer wsnserver;

    @BeforeMethod
    public void setUp() throws Exception {
        cs = new CoreService();
        wsnserver = WSNotificationServer.getInstance();
        testProtocolServer = new ProtocolServer() {
            @Override
            public int getTotalRequests() {
                return 0;
            }

            @Override
            public int getTotalMessages() {
                return 0;
            }

            @Override
            public void boot() {

            }

            @Override
            public void stopServer() {

            }

            @Override
            public String getProtocolServerType() {
                return null;
            }
        };

        testProtocolServer2 = new ProtocolServer() {
            @Override
            public int getTotalRequests() {
                return 0;
            }

            @Override
            public int getTotalMessages() {
                return 0;
            }

            @Override
            public void boot() {
                System.out.println("ProtocolServer initialized.");
            }

            @Override
            public void stopServer() {

            }

            @Override
            public String getProtocolServerType() {
                return null;
            }
        };
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // STUB
    }

    @Test
    public void testGetEventQueue() throws Exception {
        assertTrue(cs.getEventQueue() instanceof LinkedBlockingQueue);
    }

    @Test
    public void testGetExecutor() throws Exception {
        assertTrue(cs.getExecutor() instanceof ThreadPoolExecutor);
    }

    @Test
    public void testAddProtocolServer() throws Exception {
        ProtocolServer testProtocolServer = new ProtocolServer() {
            @Override
            public int getTotalRequests() {
                return 0;
            }

            @Override
            public int getTotalMessages() {
                return 0;
            }

            @Override
            public void boot() {
                System.out.println("ProtocolServer initialized.");
            }

            @Override
            public void stopServer() { }

            @Override
            public String getProtocolServerType() {
                return null;
            }
        };

        // Test that adding a new ProtocolServer works.
        cs.addProtocolServer(testProtocolServer);
        assertEquals(cs.getAllProtocolServers().size(), 1,
                "The amount of registered ProtocolServers should be 1 at this point.");

        // Adding the same ProtocolServer should not have any effect
        cs.addProtocolServer(testProtocolServer);
        assertEquals(cs.getAllProtocolServers().size(), 1,
                "The amount of registered ProtocolServers should still be 1 at this point.");

        // Remove it again.
        cs.removeProtocolServer(testProtocolServer);
    }

    @Test
    public void testRemoveProtocolServer() throws Exception {

        // Add a PS
        cs.addProtocolServer(testProtocolServer);
        assertEquals(cs.getAllProtocolServers().size(), 1,
                "The amount of registered ProtocolServers should be 1 at this point.");

        // Try removing a PS not in the list
        cs.removeProtocolServer(testProtocolServer2);
        assertEquals(cs.getAllProtocolServers().size(), 1,
                "The amount of PS should still be 1 at this point.");

        // Try removing a PS in the list
        cs.removeProtocolServer(testProtocolServer);
        assertEquals(cs.getAllProtocolServers().size(), 0,
                "The amount of registered ProtocolServers should be 0 at this point.");

    }

    @Test
    public void testRemoveAllProtocolServers() throws Exception {
        cs.addProtocolServer(testProtocolServer);
        cs.addProtocolServer(testProtocolServer2);
        cs.addProtocolServer(wsnserver);
        cs.removeAllProtocolServers();
        assertEquals(cs.getAllProtocolServers().size(), 0,
                "The amount of protocol servers should be 0 at this point.");
    }

    @Test
    public void testGetAllProtocolServers() throws Exception {
        cs.addProtocolServer(testProtocolServer);
        cs.addProtocolServer(testProtocolServer2);
        cs.addProtocolServer(wsnserver);
        assertEquals(cs.getAllProtocolServers().size(), 3,
                "The amount of registered protocolservers should be 3 at this point.");
        cs.removeAllProtocolServers();
    }

    @Test
    public void testGetProtocolServer() throws Exception {
        cs.addProtocolServer(wsnserver);
        assertEquals(cs.getProtocolServer("WSNotification"), wsnserver,
                "The returned protocol server should be the WSNotification Instance.");
        assertEquals(cs.getProtocolServer(WSNotificationServer.class), wsnserver,
                "The returned protocol server should be the WSNotification Instance.");
        assertEquals(cs.getProtocolServer("HerpaDerp"), null,
                "The returned protocol server should be null.");
        assertEquals(cs.getProtocolServer(CoreService.class), null,
                "The returned protocol server should be null.");
    }
}