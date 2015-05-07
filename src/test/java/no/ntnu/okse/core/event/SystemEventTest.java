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

package no.ntnu.okse.core.event;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SystemEventTest {

    Object o;
    SystemEvent e;

    @BeforeMethod
    public void setUp() throws Exception {
        o = new Object();
        e = new SystemEvent(SystemEvent.Type.SHUTDOWN, o);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        o = null;
        e = null;
    }

    @Test
    public void testGetData() throws Exception {
        assertNotNull(e.getData());
        assertSame(e.getData(), o);
    }

    @Test
    public void testGetType() throws Exception {
        assertNotNull(e.getType());
        assertEquals(e.getType(), SystemEvent.Type.SHUTDOWN);
        e = new SystemEvent(SystemEvent.Type.BOOT_PROTOCOL_SERVERS, o);
        assertEquals(e.getType(), SystemEvent.Type.BOOT_PROTOCOL_SERVERS);

    }
}