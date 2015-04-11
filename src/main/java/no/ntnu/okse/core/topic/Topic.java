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

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class Topic {

    private String name;
    private String type;
    private Topic parent;
    private HashSet<Topic> children;

    public Topic() {
        init(null, null);
    }

    public Topic(String name, String type) {
        init(name, type);
    }

    /**
     * Private initialization method.
     * @param name Either the name of the topic or null if no name to be set on init.
     * @param type Either the type of the topic or null of no type to be set on init.
     */
    private void init(String name, String type) {
        if (name == null) this.name = "UNNAMED";
        else this.name = name;
        if (type == null) this.type = "UNKNOWN";
        else this.type = type;

        parent = null;
        children = new HashSet<>();
    }

    /**
     * Returns the name of this topic.
     * @return A string containing the name of this topic node.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of this topic in ignorecase (lowercase) mode.
     * @return A lowercase string representation of the name of this topic.
     */
    public String getNameIgnoreCase() {
        return name.toLowerCase();
    }

    /**
     * Sets a new name of this topic.
     * @param name A string containing the new name of the topic node.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the textual type representation of this topic.
     * @return A string containing the type of this topic.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this topic.
     * @param type A string containing a textual representation of the topic type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the parent topic node of this topic instance.
     * @return The parent Topic node of this topic instance.
     */
    public Topic getParent() {
        return parent;
    }

    /**
     * Sets a new parent topic node for this topic instance, and updates the children for the new parent.
     * @param newParent A Topic instance to be the new parent of this topic node, or null if it is to be converted to a root node.
     */
    public void setParent(Topic newParent) {
        // Do we have a parent? e.g not null
        if (newParent != null) {
            // Are we switching to a new parent? If so, remove ourselves from the children set of the old parent.
            if (this.parent != newParent) {
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
     * @param topic The topic to be added as a child node.
     */
    public void addChild(@NotNull Topic topic) {
        // We reuse the logic from the setParent method
        topic.setParent(this);
    }

    /**
     * Removes a topic from the list of children.
     * @param topic The topic to be removed.
     */
    public void removeChild(@NotNull Topic topic) {
        // We reuse the logic from the setParent method
        topic.setParent(null);
    }

    /**
     * Get a HashSet of the children of this node.
     * @return A shallow copy of the children set for this node, to prevent alterations to set set itself outside setters.
     */
    public HashSet<Topic> getChildren() {
        return (HashSet<Topic>) this.children.clone();
    }

    /**
     * Checks to see wether this topic is the root node in the hierarchy.
     * @return true if this is the root node, false otherwise.
     */
    public boolean isRoot() {
        if (this.parent == null) return true;
        return false;
    }

    /**
     * Checks to see wether this topic is a leaf node in the hierarchy
     * @return true if this is a leaf node, false otherwise.
     */
    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    @Override
    public String toString() {
        return "Topic{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", parent=" + parent +
                ", children=" + children +
                '}';
    }
}
