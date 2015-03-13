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

package no.ntnu.okse.protocol;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.Log;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.xml.sax.SAXException;

/**
 * Created by Aleksander Skraastad (myth) on 3/12/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNotificationServer {

    private static final String configurationFile = "/config/wsnserver.xml";

    private static boolean _invoked = false;
    private static boolean _running = false;

    private static Logger log;
    private static WSNotificationServer _singleton;

    private Server _server;
    private final ArrayList<Connector> _connectors = new ArrayList();
    private HttpClient _client;
    private Thread _serverThread;

    private WSNotificationServer() {
        this.init(null);
        this._invoked = true;
    }

    private WSNotificationServer(Integer port) {
        this.init(port);
    }

    public static WSNotificationServer getInstance() {
        if (WSNotificationServer._invoked) return _singleton;
        else {
            _singleton = new WSNotificationServer();
            WSNotificationServer._invoked = true;

            return _singleton;
        }
    }

    public static WSNotificationServer getInstance(Integer port) {
        if (WSNotificationServer._invoked) return _singleton;
        else {
            _singleton = new WSNotificationServer(port);
            WSNotificationServer._invoked = true;
            return _singleton;
        }
    }

    private void init(Integer port) {

        log = Logger.getLogger(WSNotificationServer.class.getName());

        // TODO: Initialize other needed variables

        _client = null;
        Resource configResource = null;
        try {
            configResource = Resource.newSystemResource(configurationFile);
            XmlConfiguration config = new XmlConfiguration(configResource.getInputStream());
            this._server = (Server)config.configure();
            log.debug("XMLConfig complete, server instanciated.");

        } catch (Exception e) {
            log.error("Unable to start WSNotificationServer: " + e.getMessage());
        }
    }

    public void boot() {
        log.info("Booting WSNServer.");
        if (!_running) {
            try {
                this._client = new HttpClient();
                this._client.setFollowRedirects(false);
                this._client.start();
                log.info("Started WSNServer HTTPClient");

                this._connectors.stream().forEach(c -> this._server.addConnector(c));

                this._serverThread = new Thread(() -> {
                    try {
                        WSNotificationServer.this._server.start();
                        WSNotificationServer.this._server.join();

                    } catch (Exception serverError) {
                        log.trace(serverError.getStackTrace());
                    }
                });
                this._serverThread.start();
                WSNotificationServer._running = true;
                log.info("WSNServer Thread started successfully.");
            } catch (Exception e) {
                log.trace(e.getStackTrace());
            }
        }
    }

}
