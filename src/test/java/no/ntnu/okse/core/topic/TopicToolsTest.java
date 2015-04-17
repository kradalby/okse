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

import java.util.HashSet;

import static org.testng.Assert.*;

public class TopicToolsTest {

    Topic one,two,three,four,five,six,seven,eight,nine,ten;
    HashSet<Topic> leafNodes, childSetOne, childSetTwo, all;

    @BeforeMethod
    public void setUp() throws Exception {

        all = new HashSet<>();
        leafNodes = new HashSet<>();
        childSetOne = new HashSet<>();
        childSetTwo = new HashSet<>();

        one = new Topic("One", "Default");
        two = new Topic("Two", "Default");
        three = new Topic("Three", "Default");
        four = new Topic("Four", "Default");
        five = new Topic("Five", "Default");
        six = new Topic("One", "Default");
        seven = new Topic("Two", "Default");
        eight = new Topic("Three", "Default");
        nine = new Topic("Four", "Default");
        ten = new Topic("Five", "Default");

        all.add(one);
        all.add(two);
        all.add(three);
        all.add(four);
        all.add(five);
        all.add(six);
        all.add(seven);
        all.add(eight);
        all.add(nine);
        all.add(ten);

        one.addChild(three);
        one.addChild(four);

        two.addChild(five);

        three.addChild(six);

        four.addChild(seven);

        five.addChild(eight);
        five.addChild(ten);

        eight.addChild(nine);

        childSetOne.add(three);
        childSetOne.add(four);
        childSetOne.add(six);
        childSetOne.add(seven);

        childSetTwo.add(five);
        childSetTwo.add(eight);
        childSetTwo.add(nine);
        childSetTwo.add(ten);

        leafNodes.add(six);
        leafNodes.add(seven);
        leafNodes.add(nine);
        leafNodes.add(ten);
    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetAllTopicNodesFromRootNodeSet() throws Exception {

        HashSet<Topic> rootNodeSet = new HashSet<>();
        rootNodeSet.add(one);
        rootNodeSet.add(two);

        HashSet<Topic> allNodesFromRootNodes = TopicTools.getAllTopicNodesFromRootNodeSet(rootNodeSet);

        for (Topic t : all) {
            assertTrue(allNodesFromRootNodes.contains(t));
        }
    }

    @Test
    public void testGetAllTopicNodesFromNodeSet() throws Exception {

        HashSet<Topic> nodeSet = new HashSet<>();
        nodeSet.add(five);
        nodeSet.add(six);

        HashSet<Topic> nodesNonRootRequirement = TopicTools.getAllTopicNodesFromNodeSet(nodeSet);

        assertTrue(nodesNonRootRequirement.contains(five));
        assertTrue(nodesNonRootRequirement.contains(six));
        assertTrue(nodesNonRootRequirement.contains(eight));
        assertTrue(nodesNonRootRequirement.contains(nine));
        assertTrue(nodesNonRootRequirement.contains(ten));
    }

    @Test
    public void testGetAllChildrenFromNode() throws Exception {
        HashSet<Topic> results = TopicTools.getAllChildrenFromNode(one);
        for (Topic t : childSetOne) {
            assertTrue(results.contains(t));
        }

        results = TopicTools.getAllChildrenFromNode(two);

        for (Topic t : childSetTwo) {
            assertTrue(results.contains(t));
        }
    }

    @Test
    public void testGetAllLeafNodesFromNode() throws Exception {
        HashSet<Topic> results = TopicTools.getAllLeafNodesFromNode(one);

        for (Topic t : results) {
            assertTrue(leafNodes.contains(t));
        }

        results = TopicTools.getAllLeafNodesFromNode(two);

        for (Topic t : results) {
            assertTrue(leafNodes.contains(t));
        }
    }
}