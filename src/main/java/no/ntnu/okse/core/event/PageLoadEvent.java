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

import java.util.IllegalFormatCodePointException;

/**
 * Created by Aleksander Skraastad (myth) on 3/3/15.
 * <p>
 * okse is licenced under the MIT licence.
 * </p>
 */
public class PageLoadEvent extends Event {

    /**
     * Mockup Subclass of Event
     * <p>
     * @param operation: String representing the operation type of the event.
     * @param data: An object structure containing the payload.
     * @param dataType: String representing the datatype of the payload
     * </p>
     */
    public PageLoadEvent(String operation, Object data, String dataType) throws IllegalArgumentException {
        super(operation, data, dataType);

        if (!(data instanceof String)) {
            throw new IllegalArgumentException("Data object must be of type String.");
        }
    }

    /**
     * Returns the proper cast of the object payload
     * <p>
     * @return: A string representation of the data payload.
     * </p>
     */
    @Override
    public String getData() {
        return (String) this.data;
    }
}
