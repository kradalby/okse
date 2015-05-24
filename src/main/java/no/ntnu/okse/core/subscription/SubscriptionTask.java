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

/**
 * Created by Aleksander Skraastad (myth) on 4/18/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public class SubscriptionTask {

    // The different Task types
    public static enum Type {
        NEW_SUBSCRIBER,
        NEW_PUBLISHER,
        UPDATE_SUBSCRIBER,
        UPDATE_PUBLISHER,
        DELETE_SUBSCRIBER,
        DELETE_PUBLISHER,
        SHUTDOWN
    }

    // Needed fields
    private Type type;
    private Runnable job;

    // Public constructor
    public SubscriptionTask(Type type, Runnable job) {
        this.type = type;
        this.job = job;
    }

    // Public getter for Type
    public Type getType() {
        return this.type;
    }

    // Public run-delegation method
    public void run() {
        this.job.run();
    }
}
