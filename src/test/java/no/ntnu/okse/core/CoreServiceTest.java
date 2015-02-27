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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.LinkedBlockingQueue;

import static org.testng.Assert.*;

public class CoreServiceTest {

    CoreService cs;

    @BeforeMethod
    public void setUp() throws Exception {
        cs = new CoreService();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        cs.stopThread();
    }

    @Test
    public void testGetEventQueue() throws Exception {
        assertTrue(cs.getEventQueue() instanceof LinkedBlockingQueue,
                "getEventQueue should return an instance of LinkedBlockingQueue");
    }

    @Test
    public void testGetTaskRunner() throws Exception {
        assertTrue(cs.getTaskRunner() instanceof TaskRunner,
                "getTaskRunner should return an instance of TaskRunner");
    }

    @Test
    public void testRun() throws Exception {
        assertTrue(cs.getState() == Thread.State.NEW,
                "State of CoreService thread should be NEW before start() has been called but it was " +
                cs.getState()
        );
        cs.start();
        Thread.sleep(100);
        assertTrue(cs.getState() == Thread.State.WAITING,
                "State of CoreService thread should be WAITING after entering the run loop, but it was" +
                cs.getState()
        );
    }

    @Test
    public void testStopThread() throws Exception {
        cs.start();
        Thread.sleep(100);
        cs.stopThread();
        cs.interrupt();
        Thread.sleep(100);
        assertTrue(cs.getState() == Thread.State.TERMINATED,
                "The state of CoreService thread should be TERMINATED after stopThread has been called, but it was " +
                        cs.getState()
        );
    }
}