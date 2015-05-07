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

package no.ntnu.okse.core.messaging;

import no.ntnu.okse.Application;
import no.ntnu.okse.core.topic.Topic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.*;

public class MessageServiceTest {

    MessageService m;

    @BeforeMethod
    public void setUp() throws Exception {
        m = MessageService.getInstance();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        m = null;
    }

    @Test
    public void testIsCachingMessages() throws Exception {
        assertEquals(Application.CACHE_MESSAGES, m.isCachingMessages());
    }

    @Test
    public void testGenerateMessageForAGivenTopicSet() throws Exception {
        HashSet<Topic> topics = new HashSet<>();
        topics.add(new Topic("test", "TEST"));
        topics.add(new Topic("test2", "TEST"));
        Message msg = new Message("message", "origTopic", null, "Test");
        List<Message> generated = m.generateMessageForAGivenTopicSet(msg, topics);

        generated.forEach(genMsg -> {
            assertFalse(genMsg.getTopic().equals("origTopic"));
            assertNotSame(genMsg, msg);
            assertTrue(genMsg.getTopic().equals("test") || genMsg.getTopic().equals("test2"));
        });
        assertEquals(generated.size(), 2);
    }
}