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

import java.util.HashMap;

/**
 * Created by Fredrik on 06/03/15.
 */
public class Subscriber {
    private final String ip;
    private final String port;
    private final String protocol;
    private final HashMap<String, String> flags;


    public Subscriber(String ip, String port, String protocol, HashMap<String, String> flags){
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;
        this.flags = flags;
    }

    public String getProtocol() { return protocol; }

    public String getIp() { return ip; }

    public String getPort() { return port; }

    public HashMap getFlags(){ return flags; }



}
