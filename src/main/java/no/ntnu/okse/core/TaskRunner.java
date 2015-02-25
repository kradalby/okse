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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Aleksander Skraastad (myth) on 2/25/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class TaskRunner {

    private ArrayList<Thread> taskPool;
    ThreadProducer threadFactory;

    /**
     * Constructs an instance of TaskRunner
     * <p/>
     */
    public TaskRunner() {
        taskPool = new ArrayList<>();
        threadFactory = new ThreadProducer();
    }

    /**
     * Runs a task implementing the Runnable interface and adds it to the
     * task thread pool.
     * <p/>
     * @param r: An instance of an object implementing the Runnable interface
     */
    public void run(Runnable r) {
        Thread t = threadFactory.newThread(r);
        taskPool.add(t);
        t.start();
    }

    /**
     * Returns an ArrayList containing all threads that are still alive
     * in the task thread pool.
     * <p/>
     * @return ArrayList of Threads that are alive
     */
    public ArrayList<Thread> getActiveThreads() {
        ArrayList<Thread> active = new ArrayList<>();
        taskPool.stream()
                .filter(t -> t.getState() != Thread.State.TERMINATED)
                .forEach(t -> active.add(t));

        return active;
    }

    /**
     * Iterates over the task thread pool and removes threads that are
     * no longer alive, and have either terminated normally or thrown
     * an exception.
     */
    public void cleanCompletedThreads() {
        Iterator<Thread> threads = taskPool.iterator();

        while (threads.hasNext()) {
            Thread t = threads.next();

            if (t.getState() == Thread.State.TERMINATED) threads.remove();
        }
    }
}
