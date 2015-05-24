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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.log4j.Logger;
import org.springframework.security.crypto.codec.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@JsonIgnoreProperties({"timeout"})
public class Subscriber {

    private final String host;
    private final Integer port;
    private HashMap<String, String> attributes;
    private final String topic;
    private Long timeout;
    private final String originProtocol;
    private final String subscriberID;
    private static Logger log;
    private HashSet<String> filters;

    /**
     * Constructs a Subscriber object from the required fields
     *
     * @param host Host of the subscriber
     * @param port Port of the subscriber
     * @param topic The topic subscribed on
     * @param originProtocol Origin protocol of the subscriber
     */
    public Subscriber(String host, Integer port, String topic, String originProtocol) {
        this.timeout = null;
        this.host = host;
        this.attributes = new HashMap<>();
        this.topic = topic;
        this.originProtocol = originProtocol;
        this.filters = new HashSet<>();
        if (checkPort(port)) {
            this.port = port;
        } else throw new IllegalArgumentException("Port must be in range 1-65535");

        this.subscriberID = generateSubscriberID();

        log = Logger.getLogger(Subscriber.class.getName());

    }

    /**
     * Private method that generates an MD5 subscriber ID
     *
     * @return A string containing the generated subscriber ID
     */
    private String generateSubscriberID() {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(Long.toString(System.nanoTime()).getBytes());
            byte[] hash = m.digest();
            String topicID = new String(Hex.encode(hash));

            return topicID;
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not generate a subscriber ID (MD5 algorithm not found)");
        }

        return null;
    }

    /**
     * Check to see if the port is in valid range
     *
     * @param port The port to be verified
     * @return True if it is valid, false otherwise
     */
    private boolean checkPort(Integer port) {
        return (port > 0 && port < 65536);
    }

    /**
     * Retrieves the hostname of this subscriber
     *
     * @return The host address of this subscriber
     */
    public String getHost() {
        return host;
    }

    /**
     * Retrieves the port of this subscriber
     *
     * @return The port of this subscriber
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Retrieves the full raw topic string of this subscriber
     *
     * @return The topic this subscriber is subscribing to
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets an attribute on this subscriber object
     *
     * @param key   The attribute key
     * @param value The value of the key
     */
    public void setAttribute(String key, String value) {
        if (attributes.containsKey(key)) {
            attributes.replace(key, value);
        } else {
            attributes.put(key, value);
        }
    }

    /**
     * Retrieves the value correlated with the specified key
     *
     * @param key The key to be queried
     * @return The value of the key if it exists, null otherwise
     */
    public String getAttribute(String key) {
        if (attributes.containsKey(key)) return attributes.get(key);
        return null;
    }

    /**
     * Retrieve the timeout for this subscriber
     *
     * @return The timout if it should expire, null otherwise
     */
    public Long getTimeout() {
        return timeout;
    }

    /**
     * Retrive the subscriber ID for this subscriber
     *
     * @return The subscriber ID
     */
    public String getSubscriberID() {
        return subscriberID;
    }

    /**
     * Sets the timeout for this subcriber (can be null if it should not expire)
     *
     * @param timeout The time of expiry as seconds since unix epoch, null if infinite
     */
    public void setTimeout(Long timeout) {
        if (timeout < System.currentTimeMillis())
            throw new IllegalArgumentException("The timeout cannot be in the past.");
        this.timeout = timeout;
    }

    /**
     * Check to see if this subscriber should expire
     *
     * @return True if timeout is null, false otherwise
     */
    public boolean shouldExpire() {
        if (timeout != null) return true;
        return false;
    }

    /**
     * Add a message content filter to the subscriber
     *
     * @param filterString The filterString to be added
     */
    public void addFilter(String filterString) {
        if (!filters.contains(filterString)) filters.add(filterString);
        else log.warn("Attempt to add a filter that already exists.");
    }

    /**
     * Removes a message content filter from the subscriber
     *
     * @param filterString The filterString to be removed
     */
    public void removeFilter(String filterString) {
        if (filters.contains(filterString)) filters.remove(filterString);
        else log.warn("Attempt to remove a filter that did not exist.");
    }

    /**
     * Retrieve the set of message content filters on this subscriber object.
     *
     * @return A shallow clone of the internal filter set.
     */
    public HashSet<String> getFilterSet() {
        return (HashSet<String>) filters.clone();
    }

    /**
     * Check to see if this subscriber has expired
     *
     * @return True if this subscriber should expire and has expired
     */
    public boolean hasExpired() {
        return shouldExpire() && timeout < System.currentTimeMillis();
    }

    /**
     * Retrieve the originating protocol this subscriber used to subscribe
     *
     * @return A string representing the originating protocol
     */
    public String getOriginProtocol() {
        return this.originProtocol;
    }

    /**
     * Returns a textual representation of this subscriber object
     *
     * @return string representation of subscriber
     */
    @Override
    public String toString() {
        return "Subscriber [" + originProtocol + "] " + host + ":" + port + " on topic: " + topic;
    }
}
