/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package no.ntnu.okse.web.model;

import java.util.ArrayList;

/**
 * Created by Fredrik on 26/02/15.
 */

public class Topic {
    private final long id;
    private final String topicName;
    private final ArrayList<Subscriber> subscribers;


    public Topic(long id, String topicName,  ArrayList subscribers) {
        this.id = id;
        this.topicName = topicName;
        this.subscribers = subscribers;

    }

    public long getId() {
        return id;
    }

    public String getTopicName() {
        return topicName;
    }

    public ArrayList<Subscriber> getSubscribers() { return subscribers; }

}


