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

import org.apache.log4j.Logger;

/**
 * Created by Aleksander Skraastad (myth) on 4/18/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */
public abstract class AbstractCoreService {

    // Instance-specific fields
    protected boolean _running;
    protected Logger log;

    /**
     * Protected constructor that takes in the className string from the subclass (for logger initializing)
     *
     * @param className
     */
    protected AbstractCoreService(String className) {
        _running = false;
        log = Logger.getLogger(className);
    }

    /**
     * Initializing method
     */
    protected abstract void init();

    /**
     * Startup method that sets up the service
     */
    public abstract void boot();

    /**
     * This method must contain the operations needed for the subclass to register itself as a listener
     * to the different objects it wants to listen to. This method will be called after all Core Services have
     * been booted.
     */
    public abstract void registerListenerSupport();

    /**
     * Main run method that will be called when the subclass' serverThread is started
     */
    public abstract void run();

    /**
     * Graceful shutdown method
     */
    public abstract void stop();

}
