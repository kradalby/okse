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

package no.ntnu.okse.core.subscription;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;

import static org.testng.Assert.*;

public class SubscriberTest {

    Subscriber s;

    @BeforeMethod
    public void setUp() throws Exception {
        s = new Subscriber("0.0.0.0", 1337, "test/sub", "Test");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        s = null;
    }

    @Test
    public void testGetHost() throws Exception {
        assertEquals(s.getHost(), "0.0.0.0");
    }

    @Test
    public void testGetPort() throws Exception {
        assertTrue(s.getPort() == 1337);
        try {
            s = new Subscriber("0.0.0.0", 0, "test/sub", "Test");
            fail("Ports outside range should not be allowed");
        } catch (IllegalArgumentException e) {

        }
        try {
            s = new Subscriber("0.0.0.0", 65536, "test/sub", "Test");
            fail("Ports outside range should not be allowed");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testGetTopic() throws Exception {
        assertEquals(s.getTopic(), "test/sub");
    }

    @Test
    public void testSetAttribute() throws Exception {
        s.setAttribute("flag", "value");
        assertEquals(s.getAttribute("flag"), "value");
        assertNull(s.getAttribute("FLAG"));
    }

    @Test
    public void testGetAttribute() throws Exception {
        assertNull(s.getAttribute("flag"));
        s.setAttribute("flag", "value");
        assertEquals(s.getAttribute("flag"), "value");
        assertNull(s.getAttribute("FLAG"));
    }

    @Test
    public void testGetTimeout() throws Exception {
        Long timeout = System.currentTimeMillis() + 20000L;
        assertNull(s.getTimeout());
        s.setTimeout(timeout);
        assertTrue(s.getTimeout().equals(timeout));
    }

    @Test
    public void testGetSubscriberID() throws Exception {
        HashSet<String> ids = new HashSet<>();
        assertNotNull(s.getSubscriberID());
        // Hex regex
        assertTrue(s.getSubscriberID().matches("-?[0-9a-fA-F]+"));
        // Do a mass test and check for colliding ID's
        for (int i = 0; i < 1337; i++) {
            setUp();
            ids.add(s.getSubscriberID());
            tearDown();
        }
        assertEquals(ids.size(), 1337);
    }

    @Test
    public void testSetTimeout() throws Exception {
        Long timeout = System.currentTimeMillis();
        try {
            s.setTimeout(timeout - 20000L);
            fail("Should not be allowed to set timeout in the past");
        } catch (IllegalArgumentException e) {

        }
        timeout = System.currentTimeMillis();
        s.setTimeout(timeout + 20000L);
        assertTrue(s.getTimeout().equals(timeout + 20000L));
    }

    @Test
    public void testShouldExpire() throws Exception {
        assertFalse(s.shouldExpire());
        s.setTimeout(System.currentTimeMillis() + 20000L);
        assertTrue(s.shouldExpire());
    }

    @Test
    public void testAddFilter() throws Exception {
        assertFalse(s.getFilterSet().contains("filterstring"));
        s.addFilter("filterstring");
        assertTrue(s.getFilterSet().contains("filterstring"));
    }

    @Test
    public void testRemoveFilter() throws Exception {
        s.addFilter("filterstring");
        assertTrue(s.getFilterSet().contains("filterstring"));
        s.removeFilter("filterstring");
        assertFalse(s.getFilterSet().contains("filterstring"));
    }

    @Test
    public void testGetFilterSet() throws Exception {
        assertNotNull(s.getFilterSet());
        assertTrue(s.getFilterSet() instanceof HashSet);
        assertTrue(s.getFilterSet().isEmpty());
    }

    @Test
    public void testHasExpired() throws Exception {
        assertFalse(s.hasExpired());
        s.setTimeout(System.currentTimeMillis() + 100L);
        assertFalse(s.hasExpired());
        Thread.sleep(150);
        assertTrue(s.hasExpired());
    }

    @Test
    public void testGetOriginProtocol() throws Exception {
        assertEquals(s.getOriginProtocol(), "Test");
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(s.toString());
        assertTrue(s.toString() instanceof String);
    }
}