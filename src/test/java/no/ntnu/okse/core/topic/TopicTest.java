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

public class TopicTest {

    Topic noNameNoTypeTopic;
    Topic namedAndTypedTopic;
    Topic childOne;
    Topic childTwo;
    Topic childThree;
    Topic rootOne;
    Topic rootTwo;

    @BeforeMethod
    public void setUp() throws Exception {
        noNameNoTypeTopic = new Topic();
        namedAndTypedTopic = new Topic("SomeName", "SomeType");
        childOne = new Topic("ChildOne", "Topic");
        childTwo = new Topic("ChildTwo", "Topic");
        childThree = new Topic("ChildThree", "Topic");
        rootOne = new Topic("RootOne", "Topic");
        rootTwo = new Topic("RootTwo", "Topic");
        rootOne.addChild(childOne);
        rootOne.addChild(childTwo);
        rootTwo.addChild(childThree);
    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetTopicID() throws Exception {
        Topic one = new Topic();
        HashSet<String> ids = new HashSet<>();
        assertNotNull(one.getTopicID());
        // Hex regex
        assertTrue(one.getTopicID().matches("-?[0-9a-fA-F]+"));
        // Do a mass test and check for colliding ID's
        for (int i = 0; i < 1337; i++) {
            one = new Topic();
            ids.add(one.getTopicID());
        }
        assertEquals(ids.size(), 1337);

    }

    @Test
    public void testGetName() throws Exception {
        assertEquals(noNameNoTypeTopic.getName(), "UNNAMED");
        assertEquals(namedAndTypedTopic.getName(), "SomeName");
    }

    @Test
    public void testGetNameIgnoreCase() throws Exception {
        assertEquals(noNameNoTypeTopic.getNameIgnoreCase(), "unnamed");
        assertEquals(namedAndTypedTopic.getNameIgnoreCase(), "somename");
    }

    @Test
    public void testSetName() throws Exception {
        noNameNoTypeTopic.setName("SomeOtherName");
        assertEquals(noNameNoTypeTopic.getName(), "SomeOtherName");
        assertEquals(noNameNoTypeTopic.getNameIgnoreCase(), "someothername");
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(noNameNoTypeTopic.getType(), "UNKNOWN");
    }

    @Test
    public void testSetType() throws Exception {
        noNameNoTypeTopic.setType("SomeTopic");
        assertEquals(noNameNoTypeTopic.getType(), "SomeTopic");
    }

    @Test
    public void testGetParent() throws Exception {
        assertEquals(childOne.getParent(), rootOne);
        assertEquals(childTwo.getParent(), rootOne);
        assertEquals(childThree.getParent(), rootTwo);
    }

    @Test
    public void testSetParent() throws Exception {
        assertEquals(childThree.getParent(), rootTwo);
        assertEquals(rootOne.getChildren().size(), 2);
        childThree.setParent(rootOne);
        assertEquals(childThree.getParent(), rootOne);
        assertEquals(rootOne.getChildren().size(), 3);
        assertEquals(rootTwo.getChildren().size(), 0);
        childThree.setParent(null);
        assertFalse(rootOne.getChildren().contains(childThree));
        assertEquals(childThree.getParent(), null);
    }

    @Test
    public void testAddChild() throws Exception {
        Topic childFour = new Topic();
        Topic childFive = new Topic();
        assertEquals(childFour.getParent(), null);
        assertEquals(childFive.getParent(), null);
        int origChildCount = rootTwo.getChildren().size();
        rootTwo.addChild(childFour);
        rootTwo.addChild(childFive);
        assertEquals(rootTwo.getChildren().size(), origChildCount + 2);
        assertTrue(rootTwo.getChildren().contains(childFour));
        assertTrue(rootTwo.getChildren().contains(childFive));
        assertEquals(childFour.getParent(), rootTwo);
        assertEquals(childFive.getParent(), rootTwo);
    }

    @Test
    public void testRemoveChild() throws Exception {
        Topic childSix = new Topic();
        rootTwo.addChild(childSix);
        assertTrue(rootTwo.getChildren().contains(childSix));
        assertEquals(childSix.getParent(), rootTwo);
        rootTwo.removeChild(childSix);

    }

    @Test
    public void testGetChildren() throws Exception {
        HashSet<Topic> localChildren = rootTwo.getChildren();

        assertFalse(rootTwo.getChildren() == localChildren);

        rootTwo.getChildren().stream().forEach(c -> assertTrue(localChildren.contains(c)));

        Topic childSeven = new Topic();
        Topic childEight = new Topic();

        localChildren.add(childSeven);
        localChildren.add(childEight);

        rootTwo.addChild(childSeven);
        rootTwo.addChild(childEight);

        for (Topic t: rootTwo.getChildren()) {
            assertTrue(localChildren.contains(t));
        }
    }

    @Test
    public void testClearChildren() throws Exception {
        HashSet<Topic> localChildren = rootTwo.getChildren();
        localChildren.stream().forEach(c -> assertEquals(c.getParent(), rootTwo));
        rootTwo.clearChildren();
        assertEquals(rootTwo.getChildren().size(), 0);
        localChildren.stream().forEach(c -> assertEquals(c.getParent(), null));
    }

    @Test
    public void testIsRoot() throws Exception {
        assertTrue(rootOne.isRoot());
        assertTrue(rootTwo.isRoot());
        Topic t = new Topic();
        rootOne.setParent(t);
        assertFalse(rootOne.isRoot());
        assertTrue(t.isRoot());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertTrue(childOne.isLeaf());
        assertTrue(childTwo.isLeaf());
        assertTrue(childThree.isLeaf());
        Topic t = new Topic();
        childThree.addChild(t);
        assertFalse(childThree.isLeaf());
        assertFalse(childThree.isRoot());
        assertTrue(t.isLeaf());
    }

    @Test
    public void testGetFullTopicString() throws Exception {
        Topic childTen = new Topic();
        Topic childEleven = new Topic();
        childTen.setName("ChildTen");
        childEleven.setName("ChildEleven");
        childTen.addChild(childEleven);
        childThree.addChild(childTen);

        String fullTopicForChildEleven = "RootTwo/ChildThree/ChildTen/ChildEleven";
        String fullTopicForChildTen = "RootTwo/ChildThree/ChildTen";
        String fullTopicForChildThree = "RootTwo/ChildThree";

        assertEquals(childEleven.getFullTopicString(), fullTopicForChildEleven);
        assertEquals(childTen.getFullTopicString(), fullTopicForChildTen);
        assertEquals(childThree.getFullTopicString(), fullTopicForChildThree);
    }

    @Test
    public void testGetFullTopicStringIgnoreCase() throws Exception {
        Topic childTen = new Topic();
        Topic childEleven = new Topic();
        childTen.setName("ChildTen");
        childEleven.setName("ChildEleven");
        childTen.addChild(childEleven);
        childThree.addChild(childTen);

        String fullTopicForChildEleven = "roottwo/childthree/childten/childeleven";
        String fullTopicForChildTen = "roottwo/childthree/childten";
        String fullTopicForChildThree = "roottwo/childthree";

        assertEquals(childEleven.getFullTopicStringIgnoreCase(), fullTopicForChildEleven);
        assertEquals(childTen.getFullTopicStringIgnoreCase(), fullTopicForChildTen);
        assertEquals(childThree.getFullTopicStringIgnoreCase(), fullTopicForChildThree);
    }

    @Test
    public void testIsAncestorOf() throws Exception {
        Topic parent = new Topic();
        Topic child = new Topic();
        Topic grandchild = new Topic();
        grandchild.setParent(child);
        child.setParent(parent);

        assertTrue(parent.isAncestorOf(child));
        assertTrue(parent.isAncestorOf(grandchild));
        assertFalse(parent.isAncestorOf(parent));
    }

    @Test
    public void testIsDescendantOf() throws Exception {
        Topic parent = new Topic();
        Topic child = new Topic();
        Topic grandchild = new Topic();
        grandchild.setParent(child);
        child.setParent(parent);

        assertTrue(grandchild.isDescendantOf(child));
        assertTrue(grandchild.isDescendantOf(parent));
        assertFalse(grandchild.isDescendantOf(grandchild));
    }

    @Test
    public void testToString() throws Exception {
        Topic parent = new Topic("parent", "TEST");
        Topic child = new Topic("child", "TEST");
        child.setParent(parent);
        assertEquals(child.toString(), "Topic{parent/child}");
        assertEquals(parent.toString(), "Topic{parent}");
}
}