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

import no.ntnu.okse.core.topic.Topic;

import java.util.HashMap;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class Subscriber {

    private String address;
    private Integer port;
    private HashMap<String, String> attributes;
    private String topic;
    private Long timeout;
    private String originProtocol;

    public Subscriber(String address, Integer port, String topic, String originProtocol) {
        init(address, port, topic, originProtocol);
    }

    private void init(String address, Integer port, String topic, String originProtocol) {
        this.timeout = null;
        this.address = address;
        this.port = port;
        this.attributes = new HashMap<>();
        this.topic = topic;
        this.originProtocol = originProtocol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Error: Port outside allowed range (1-65535)");
        else this.port = port;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setAttribute(String key, String value) {
        if (attributes.containsKey(key)) {
            attributes.replace(key, value);
        } else {
            attributes.put(key, value);
        }
    }

    public String getAttribute(String key) {
        if (attributes.containsKey(key)) return attributes.get(key);
        return null;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        if (timeout < System.currentTimeMillis()) throw new IllegalArgumentException("The timeout cannot be in the past.");
        this.timeout = timeout;
    }

    public boolean shouldExpire() {
        if (timeout == null) return true;
        return false;
    }

    public String getOriginProtocol() {
        return this.originProtocol;
    }

    @Override
    public boolean equals(Object o) {
        Subscriber other;
        if (!(o instanceof Subscriber)) {
            return false;
        } else {
            other = (Subscriber) o;
        }

        return (this.address.equals(other.getAddress()) &&
                this.port.equals(other.getPort()) &&
                this.topic.equals(other.getTopic()));
    }

    public String toString() {
        return "[" + originProtocol + "] " + address + ":" + port + " on topic: " + topic;
    }
}
