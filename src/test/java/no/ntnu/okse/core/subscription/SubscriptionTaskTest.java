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

public class SubscriptionTaskTest {

    boolean jobComplete;
    SubscriptionTask task;
    Runnable job;

    @BeforeMethod
    public void setUp() throws Exception {
        jobComplete = false;
        job = () -> jobComplete = true;
        task = new SubscriptionTask(SubscriptionTask.Type.NEW_SUBSCRIBER, job);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        jobComplete = false;
        job = null;
        task = null;
    }

    @Test
    public void testGetType() throws Exception {
        assertNotNull(task.getType());
        assertEquals(task.getType(), SubscriptionTask.Type.NEW_SUBSCRIBER);
        task = new SubscriptionTask(SubscriptionTask.Type.DELETE_SUBSCRIBER, job);
        assertEquals(task.getType(), SubscriptionTask.Type.DELETE_SUBSCRIBER);
    }

    @Test
    public void testRun() throws Exception {
        task.run();
        assertTrue(jobComplete);
    }
}