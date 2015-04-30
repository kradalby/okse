package no.ntnu.okse.web.model;

import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.subscription.Subscriber;

import java.util.HashSet;

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 20/04/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class Topics {

    private Topic topic;
    private HashSet<Subscriber> subscribers;

    public Topics(Topic t, HashSet<Subscriber> s) {
        this.topic = t;
        this.subscribers = s;
    }

    public Topic getTopic() {
        return topic;
    }

    public HashSet<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(HashSet<Subscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public void setTopic(Topic topic) {

        this.topic = topic;
    }
}
