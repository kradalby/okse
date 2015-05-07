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

import static org.testng.Assert.*;

public class PublisherTest {
    
    Publisher p;

    @BeforeMethod
    public void setUp() {
        p = new Publisher("test/sub", "123.123.123.123", 8080, "Test");
    }

    @AfterMethod
    public void tearDown() {
        p = null;
    }

    @Test
    public void testGetOriginProtocol() throws Exception {
        assertEquals(p.getOriginProtocol(), "Test");
    }

    @Test
    public void testGetHost() throws Exception {
        assertEquals(p.getHost(), "123.123.123.123");
    }

    @Test
    public void testGetTopic() throws Exception {
        assertEquals(p.getTopic(), "test/sub");
    }

    @Test
    public void testGetPort() throws Exception {
        assertTrue(p.getPort() == 8080);
    }

    @Test
    public void testSetTimeout() throws Exception {
        Long timeout = System.currentTimeMillis();
        try {
            p.setTimeout(timeout - 20000L);
            fail("Should not be allowed to set timeout in the past");
        } catch (IllegalArgumentException e) {

        }
        timeout = System.currentTimeMillis();
        p.setTimeout(timeout + 20000L);
        assertTrue(p.getTimeout().equals(timeout + 20000L));
    }

    @Test
    public void testGetTimeout() throws Exception {
        Long timeout = System.currentTimeMillis() + 20000L;
        assertNull(p.getTimeout());
        p.setTimeout(timeout);
        assertTrue(p.getTimeout().equals(timeout));
    }

    @Test
    public void testSetAttribute() throws Exception {
        assertNull(p.getAttribute("flag"));
        p.setAttribute("flag", "value");
        assertEquals(p.getAttribute("flag"), "value");
        assertNull(p.getAttribute("FLAG"));

    }

    @Test
    public void testGetAttribute() throws Exception {
        assertNull(p.getAttribute("flag"));
        p.setAttribute("flag", "value");
        assertEquals(p.getAttribute("flag"), "value");
        assertNull(p.getAttribute("null"));
    }

    @Test
    public void testShouldExpire() throws Exception {
        assertFalse(p.shouldExpire());
        p.setTimeout(System.currentTimeMillis() + 20000L);
        assertTrue(p.shouldExpire());
    }

    @Test
    public void testHasExpired() throws Exception {
        assertFalse(p.hasExpired());
        p.setTimeout(System.currentTimeMillis() + 100L);
        assertFalse(p.hasExpired());
        Thread.sleep(150);
        assertTrue(p.hasExpired());
    }

    @Test
    public void testGetHostAndPort() throws Exception {
        assertEquals(p.getHostAndPort(), "123.123.123.123:8080");
    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(p.toString());
        assertTrue(p.toString() instanceof String);
    }
}