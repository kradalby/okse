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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.log4j.Logger;
import org.springframework.security.crypto.codec.Hex;

import javax.validation.constraints.NotNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
@JsonIgnoreProperties({"parent", "children", "type"})
public class Topic {

    private String name;
    private final String topicID;
    private String type;
    private Topic parent;
    private HashSet<Topic> children;
    private static Logger log;

    public Topic() {
        if (name == null) this.name = "UNNAMED";
        if (type == null) this.type = "UNKNOWN";

        topicID = generateTopicID();

        parent = null;
        children = new HashSet<>();
    }

    public Topic(String name, String type) {
        log = Logger.getLogger(Topic.class.getName());
        if (name == null) this.name = "UNNAMED";
        else this.name = name;
        if (type == null) this.type = "UNKNOWN";
        else this.type = type;

        topicID = generateTopicID();

        parent = null;
        children = new HashSet<>();
    }

    /**
     * Private method that generates an MD5 topic ID
     *
     * @return A string containing the generated topicID
     */
    private String generateTopicID() {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(Long.toString(System.nanoTime()).getBytes());
            byte[] hash = m.digest();
            String topicID = new String(Hex.encode(hash));

            return topicID;
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not generate a topic ID (MD5 algorithm not found)");
        }

        return null;
    }

    /**
     * Returns the id of this topic
     *
     * @return A string containing the id of this topic node
     */
    public String getTopicID() {
        return topicID;
    }

    /**
     * Returns the name of this topic.
     *
     * @return A string containing the name of this topic node.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this topic
     *
     * @param name A string representing the name of this topic
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this topic in ignorecase (lowercase) mode.
     *
     * @return A lowercase string representation of the name of this topic.
     */
    public String getNameIgnoreCase() {
        return name.toLowerCase();
    }

    /**
     * Returns the textual type representation of this topic.
     *
     * @return A string containing the type of this topic.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this topic.
     *
     * @param type A string containing a textual representation of the topic type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the parent topic node of this topic instance.
     *
     * @return The parent Topic node of this topic instance.
     */
    public Topic getParent() {
        return parent;
    }

    /**
     * Sets a new parent topic node for this topic instance, and updates the children for the new parent.
     *
     * @param newParent A Topic instance to be the new parent of this topic node, or null if it is to be converted to a root node.
     */
    public void setParent(Topic newParent) {
        // Do we have a parent? e.g not null
        if (newParent != null) {
            // Are we switching to a new parent? If so, remove ourselves from the children set of the old parent.
            if (this.parent != newParent && this.parent != null) {
                this.parent.children.remove(this);
            }
            // Add ourselves to the children set of the new parent
            if (!newParent.children.contains(this)) newParent.children.add(this);
            // We are removing the parent of this node, converting it to a root node.
        } else {
            // Remove ourselves from the children set of the existing parent
            this.parent.children.remove(this);
        }
        // Set the new parent
        this.parent = newParent;
    }

    /**
     * Adds a topic as a child of this topic node.
     *
     * @param topic The topic to be added as a child node.
     */
    public void addChild(@NotNull Topic topic) {
        // We reuse the logic from the setParent method
        topic.setParent(this);
    }

    /**
     * Removes a topic from the list of children.
     *
     * @param topic The topic to be removed.
     */
    public void removeChild(@NotNull Topic topic) {
        // We reuse the logic from the setParent method
        topic.setParent(null);
    }

    /**
     * Get a HashSet of the children of this node.
     *
     * @return A shallow copy of the children set for this node, to prevent alterations to set set itself outside setters.
     */
    public HashSet<Topic> getChildren() {
        return (HashSet<Topic>) this.children.clone();
    }

    /**
     * Removes all children from this node, by disconnecting their parent relation to this Topic node.
     */
    public void clearChildren() {
        Iterator<Topic> iterator = this.children.iterator();
        while (iterator.hasNext()) {
            Topic t = iterator.next();
            t.setParent(null);
        }
    }

    /**
     * Checks to see wether this topic is the root node in the hierarchy.
     *
     * @return true if this is the root node, false otherwise.
     */
    public boolean isRoot() {
        if (this.parent == null) return true;
        return false;
    }

    /**
     * Checks to see wether this topic is a leaf node in the hierarchy.
     *
     * @return true if this is a leaf node, false otherwise.
     */
    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    /**
     * Traverses up the tree quasi-recursively to generate a complete topic string.
     *
     * @return A string containing the full topic path of this node.
     */
    public String getFullTopicString() {
        String localTopicName = this.getName();
        if (!this.isRoot()) {
            return this.parent.getFullTopicString() + "/" + localTopicName;
        } else {
            return this.name;
        }
    }

    /**
     * Traverses up the tree quasi-recursively to generate a complete topic string.
     *
     * @return A string containing the full topic path of this node.
     */
    public String getFullTopicStringIgnoreCase() {
        if (!this.isRoot()) {
            return this.parent.getFullTopicStringIgnoreCase() + "/" + this.getNameIgnoreCase();
        } else {
            return this.getNameIgnoreCase();
        }
    }

    /**
     * Checks if this topic is the ancestor of another topic
     *
     * @param other The topic node we wish to explore if is a decendant of this topic node
     * @return True if this topic is an ancestor of the argument, false otherwise
     */
    public boolean isAncestorOf(Topic other) {

        // If the other is a root node, it is impossible that this object is an ancestor of it
        if (other.isRoot()) return false;

            // If the other's parent is this object, we hace a match
        else if (other.getParent() == this) return true;

            // Recursively ascend up the family tree
        else return this.isAncestorOf(other.getParent());
    }

    /**
     * Checks if this topic is a decendant of the argument topic. This method reuses the isAncestorOf method,
     * by swapping the arguments.
     *
     * @param other The topic we wish to check if we have decended from
     * @return True if we have decended from the argument topic, false otherwise
     */
    public boolean isDescendantOf(Topic other) {
        return other.isAncestorOf(this);
    }

    @Override
    public String toString() {
        return "Topic{" + this.getFullTopicString() + "}";
    }

}
