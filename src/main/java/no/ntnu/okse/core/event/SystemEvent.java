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

/**
 * Created by Aleksander Skraastad (myth) on 4/18/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class SystemEvent extends Event {

    public static enum Type {
        SHUTDOWN,
        SHUTDOWN_PROTOCOL_SERVERS,
        BOOT_PROTOCOL_SERVERS
    }

    Type type;

    /**
     * Constructs an Event containing an operation, some data and a dataType.
     * <p>
     *
     * @param type : Type of SystemEvent
     * @param data : An object containing the data payload.
     */
    public SystemEvent(Type type, Object data) {
        super(data);
        this.type = type;
    }

    /**
     * An abstract method to retrieve the data payload.
     * <p>
     *
     * @return: An object containing the data payload casted to proper type in subclass.
     */
    @Override
    public Object getData() {
        return data;
    }

    /**
     * An abstract method that should return a subclass enum type
     *
     * @return Type enum implemented in subclass
     */
    @Override
    public Object getType() {
        return type;
    }
}
