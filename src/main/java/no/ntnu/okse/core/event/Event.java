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
 * Created by Aleksander Skraastad (myth) on 3/3/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public abstract class Event {

    private String operation;
    protected Object data;
    private String dataType;

    /**
     * Constructs an Event containing an operation, some data and a dataType.
     * <p/>
     * @param operation: A string representing the operation to be performed.
     * @param data: An object containing the data payload.
     * @param dataType: A string representing the type of data.
     */
    public Event(String operation, Object data, String dataType) {
        this.operation = operation;
        this.data = data;
        this.dataType = dataType;
    }

    /**
     * What operation is to be performed from this event.
     * <p/>
     * @return: A string representing the operation to be performed.
     */
    public String getOperation() {
        return operation;
    }

    /**
     * An abstract method to retrieve the data payload.
     * <p/>
     * @return: An object containing the data payload.
     */
    public abstract Object getData();

    /**
     * What data type is the data object
     * @return: A string representing the class instance of the data object
     */
    public String getDataType() {
        return dataType;
    }
}
