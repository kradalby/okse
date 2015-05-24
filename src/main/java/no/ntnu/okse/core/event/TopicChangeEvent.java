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

import no.ntnu.okse.core.topic.Topic;

/**
 * Created by Aleksander Skraastad (myth) on 4/11/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class TopicChangeEvent extends Event {

    public enum Type {
        NEW,
        UPDATE,
        DELETE
    }

    Type type;

    /**
     * Constructs an Event containing an operation, some data and a dataType.
     * <p/>
     *
     * @param data : An object containing the data payload.
     */
    public TopicChangeEvent(Type eventType, Object data) {
        super(data);
        this.type = eventType;
    }

    /**
     * A method to retrieve the data payload.
     * <p/>
     *
     * @return: An object containing the data payload casted to proper type.
     */
    @Override
    public Topic getData() {
        return (Topic) this.data;
    }

    /**
     * A method that returns the type of topic change event that occured.
     *
     * @return Type enum implemented in subclass
     */
    @Override
    public Type getType() {
        return this.type;
    }
}
