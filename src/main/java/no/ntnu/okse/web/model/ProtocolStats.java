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

/**
 * Created by Fredrik Borgen Tørnvall on 25/03/15.
 */
public class ProtocolStats {

    private final String protocolServer;
    private final int totalMessagesSent;
    private final int totalMessagesReceived;
    private final int totalRequests;
    private final int totalBadRequests;
    private final int totalErrors;

    /**
     * Constructes a model for containing protocol stats to be serialized to JSON
     *
     * @param protocolServer : The protocol server
     * @param totalMessagesSent : Total amount of messages sent
     * @param totalMessagesReceived : Total amount of messages received
     * @param totalRequests : Total amount of requests
     * @param totalBadRequests : Total amount of bad requests
     * @param totalErrors : Total amount of errors
     */
    public ProtocolStats(String protocolServer, int totalMessagesSent, int totalMessagesReceived, int totalRequests, int totalBadRequests, int totalErrors) {
        this.protocolServer = protocolServer;
        this.totalMessagesSent = totalMessagesSent;
        this.totalMessagesReceived = totalMessagesReceived;
        this.totalRequests = totalRequests;
        this.totalBadRequests = totalBadRequests;
        this.totalErrors = totalErrors;
    }

    public String getProtocolServer() {
        return protocolServer;
    }

    public int getTotalMessagesSent() {
        return totalMessagesSent;
    }

    public int getTotalMessagesReceived() {
        return totalMessagesReceived;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getTotalBadRequests() {
        return totalBadRequests;
    }

    public int getTotalErrors() {
        return totalErrors;
    }
}
