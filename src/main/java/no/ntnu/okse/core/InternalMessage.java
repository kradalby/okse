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

package no.ntnu.okse.core;

import java.util.HashMap;

/**
 * Created by Aleksander Skraastad (myth) on 3/2/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class InternalMessage {
    private final int ID;
    private final String message;
    private final String topic;
    private final boolean retain;
    private boolean delivered;
    private HashMap<String, String> flags;

    public InternalMessage(int ID, String topic, String message, boolean retain) {
        this.ID = ID;
        this.message = message;
        this.topic = topic;
        this.retain = retain;
        this.delivered = false;
        this.flags = new HashMap<>();
    }

    /**
     * Check if the message is flagged as delivered
     * @return Deliverystatus
     */
    public boolean isDelivered() {
        return delivered;
    }

    /**
     * Flags message as delivered
     */
    public void setDelivered() {
        delivered = true;
    }

    /**
     * Add a custom flag for this message
     * @param key: The flag name
     * @param value: The flag value
     */
    public void setFlag(String key, String value) {
        flags.put(key, value);
    }

    /**
     * Retrieve the value of a certain custom flag
     * @param key: The name of the flag
     * @return The value of the "key" flag
     */
    public String getFlag(String key) {
        return this.flags.get(key);
    }
}
