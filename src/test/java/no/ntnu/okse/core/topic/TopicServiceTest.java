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

package no.ntnu.okse.core.topic;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;

import static org.testng.Assert.*;

public class TopicServiceTest {

    TopicService ts = TopicService.getInstance();
    Topic rootOne, rootTwo, one, two, three;
    HashSet<Topic> roots, leafs, all;

    @BeforeMethod
    public void setUp() throws Exception {
        ts.boot();

        roots = new HashSet<>();
        leafs = new HashSet<>();
        all = new HashSet<>();

        rootOne = new Topic("RootOne", "Root");
        rootTwo = new Topic("RootTwo", "Root");
        one = new Topic("One", "Default");
        two = new Topic("Two", "Default");
        three = new Topic("Three", "Default");

        rootOne.addChild(one);
        one.addChild(two);
        rootTwo.addChild(three);

        roots.add(rootOne);
        roots.add(rootTwo);

        leafs.add(two);
        leafs.add(three);

        all.add(rootOne);
        all.add(rootTwo);
        all.add(one);
        all.add(two);
        all.add(three);

        ts.addTopicLocal(rootOne);
        ts.addTopicLocal(rootTwo);
        ts.addTopicLocal(one);
        ts.addTopicLocal(two);
        ts.addTopicLocal(three);

    }

    @AfterMethod
    public void tearDown() throws Exception {
        ts.stop();
    }

    @Test
    public void testGetInstance() throws Exception {
        assertTrue(ts.getInstance() instanceof TopicService);
    }

    @Test
    public void testAllGetRootTopics() throws Exception {
        HashSet<Topic> rootTopics = ts.getAllRootTopics();
        roots.forEach(t -> assertTrue(rootTopics.contains(t)));
    }

    @Test
    public void testGetAllTopics() throws Exception {
        HashSet<Topic> allTopics = ts.getAllTopics();
        all.forEach(t -> assertTrue(allTopics.contains(t)));
    }

    @Test
    public void testGetTopic() throws Exception {
        assertEquals(three, ts.getTopic(three.getFullTopicString()));
    }

    @Test
    public void testGetAllLeafTopics() throws Exception {
        HashSet<Topic> leafTopics = ts.getAllLeafTopics();
        leafs.forEach(t -> assertTrue(leafTopics.contains(t)));
    }

    @Test
    public void testTopicExists() throws Exception {
        assertTrue(ts.topicExists(one));
        assertTrue(ts.topicExists(rootTwo));
    }

    @Test
    public void testTopicExists1() throws Exception {
        assertTrue(ts.topicExists(one.getFullTopicString()));
        assertTrue(ts.topicExists(two.getFullTopicString()));
    }

    @Test
    public void testGenerateTopicNodesFromRawTopicString() throws Exception {
        HashSet<Topic> collector = ts.generateTopicNodesFromRawTopicString("no/ffi/test");
        assertEquals(collector.size(), 3);
        HashMap<String, Topic> partNames = new HashMap<>();
        partNames.put("no", null);
        partNames.put("ffi", null);
        partNames.put("test", null);
        collector.forEach(t -> assertTrue(partNames.containsKey(t.getName())));
        collector.forEach(t -> partNames.put(t.getName(), t));
        assertEquals(partNames.get("test").getParent(), partNames.get("ffi"));
        assertEquals(partNames.get("ffi").getParent(), partNames.get("no"));
        collector.forEach(t -> ts.addTopicLocal(t));
        collector = ts.generateTopicNodesFromRawTopicString("no/ffi/test/sub");
        partNames.put("sub", null);
        collector.forEach(t -> {
            if (t.getName().equals("sub")) {
                partNames.put("sub", t);
            }
        });
        assertEquals(partNames.get("sub").getParent(), partNames.get("test"));
        assertEquals(partNames.get("test").getParent(), partNames.get("ffi"));
        assertEquals(partNames.get("ffi").getParent(), partNames.get("no"));
    }
}