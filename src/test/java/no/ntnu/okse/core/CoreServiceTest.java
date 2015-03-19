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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.testng.Assert.*;

public class CoreServiceTest {

    CoreService cs;

    @BeforeMethod
    public void setUp() throws Exception {
        cs = new CoreService();
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
        ProtocolServer testProtocolServer = () -> System.out.println("ProtocolServer initialized.");

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
        ProtocolServer testProtocolServer = () -> System.out.println("ProtocolServer initialized.");
        ProtocolServer testProtocolServer2 = () -> System.out.println("ProtocolServer initialized.");

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
}