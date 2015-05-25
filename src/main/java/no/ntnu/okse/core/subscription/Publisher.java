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

import java.util.HashMap;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class Publisher {

    private final String originProtocol;
    private final String topic;
    private final String host;
    private final Integer port;
    private HashMap<String, String> attributes;
    private Long timeout;

    /**
     * Create a Publisher object using the required input arguments
     *
     * @param rawTopicString The full raw topic string this publisher is registered to
     * @param host           The host of the publisher
     * @param port           The port used by the publisher
     * @param originProtocol The originating protocol the publisher registered from
     */
    public Publisher(String rawTopicString, String host, Integer port, String originProtocol) {
        this.topic = rawTopicString;
        this.host = host;
        this.port = port;
        this.originProtocol = originProtocol;
        this.attributes = new HashMap<>();
        this.timeout = null;
    }

    /**
     * Fetch what protocol this publisher used to register
     *
     * @return The originating protocol
     */
    public String getOriginProtocol() {
        return originProtocol;
    }

    /**
     * Retrieve the hostname or ip this publisher used to register
     *
     * @return The hostname of or ip of the publisher
     */
    public String getHost() {
        return host;
    }

    /**
     * Retrieves the raw topic string this publisher has registered to
     *
     * @return The raw topic string this publisher is registered to
     */
    public String getTopic() {
        return this.topic;
    }

    /**
     * Retrieve the port this publisher used to register
     *
     * @return The port of the publisher
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Set a new timeout for this publisher
     *
     * @param timeout The new timeout represented as seconds since unix epoch
     * @throws IllegalArgumentException If the timeout is in the past
     */
    public void setTimeout(Long timeout) throws IllegalArgumentException {
        if (timeout == null) this.timeout = timeout;
        else if (timeout < System.currentTimeMillis())
            throw new IllegalArgumentException("Timeout cannot be in the past");
        this.timeout = timeout;
    }

    /**
     * Retrieve the current timeout of this publisher
     *
     * @return The unix epoch time when this publisher should expire
     */
    public Long getTimeout() {
        return timeout;
    }

    /**
     * Set an attribute on this publisher object
     *
     * @param key   The attribute key
     * @param value The attribute value
     */
    public void setAttribute(String key, String value) {
        if (attributes.containsKey(key)) {
            attributes.replace(key, value);
        } else {
            attributes.put(key, value);
        }
    }

    /**
     * Retrieve an attribute from this publisher object
     *
     * @param key The attribute key to be queried
     * @return The value of the attribute key, <code>null</code> if it does not exist
     */
    public String getAttribute(String key) {
        if (attributes.containsKey(key)) return attributes.get(key);
        return null;
    }

    /**
     * Checks to see if this publisher should expire
     *
     * @return True if this publisher object should expire, false otherwise
     */
    public boolean shouldExpire() {
        return timeout != null;
    }

    /**
     * Checks to see if this publisher has expired
     *
     * @return True if it should expire and has expired, false otherwise
     */
    public boolean hasExpired() {
        if (shouldExpire()) {
            return timeout < System.currentTimeMillis();
        }
        return false;
    }

    /**
     * Fetches a concatenation of host and port as a string
     *
     * @return The host and port of the subscriber as a string
     */
    public String getHostAndPort() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        return "Publisher [" + originProtocol + "] " + host + ":" + port + " on topic: " + topic;
    }

}
