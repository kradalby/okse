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

package no.ntnu.okse.web.model;

import java.lang.management.*;


/**
 * Created by Fredrik on 06/03/15.
 */
public class Stats {
    private final long ramTotal;
    private final long ramFee;
    private final long ramUse;
    private final double cpuAvailable;
    private final int totalRequests;
    private final int totalMessages;

    public Stats(long ramFree, long ramUse, long ramTotal, double cpuAvailable, int totalRequests, int totalMessages){

        this.ramFee = ramFree;
        this.ramUse = ramUse;
        this.ramTotal = ramTotal;
        this.cpuAvailable = cpuAvailable;
        this.totalRequests = totalRequests;
        this.totalMessages = totalMessages;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public long getRamUse() {
        return ramUse;
    }

    public double getCpuUse() {
        return cpuAvailable;
    }


    public long getRamTotal(){
      return ramTotal;
    }

    public double getRamFree(){ return ramFee; }




}
