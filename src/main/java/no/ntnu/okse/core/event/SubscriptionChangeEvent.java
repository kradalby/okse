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

package no.ntnu.okse.core.event;

import no.ntnu.okse.core.subscription.Subscriber;

/**
 * Created by Aleksander Skraastad (myth) on 4/5/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class SubscriptionChangeEvent extends Event {

    // The different types of subscription changes that might occur
    public static enum Type {
        SUBSCRIBE,
        UNSUBSCRIBE,
        RENEW,
        PAUSE,
        RESUME
    }

    private Type eventType;

    /**
     * Constructs a SubscriptionChangeEvent of a certain Type, with associated Subscriber object.
     * <p>
     *
     * @param eventType : The type of subscription event this is
     * @param data      : The subscriber object in question.
     */
    public SubscriptionChangeEvent(Type eventType, Subscriber data) {
        super(data);
        this.eventType = eventType;
    }

    /**
     * Fetch the related Subscriber object.
     *
     * @return The Subscriber object associated with this event.
     */
    @Override
    public Subscriber getData() {
        return (Subscriber) data;
    }

    /**
     * Fetch the event type of this event.
     *
     * @return A SubscriptionChangeEvent
     */
    @Override
    public Type getType() {
        return this.eventType;
    }
}
