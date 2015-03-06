/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package no.ntnu.okse.web.model;

/**
 * Created by Fredrik on 26/02/15.
 */

public class Topic {
    private final long id;
    private final String content;
    private final String dialect;
    private final String subIp;
    private final String subPort;
    private final String subProtocol;


    public Topic(long id, String content, String dialect, String subIp, String subPort, String subProtocol) {
        this.id = id;
        this.content = content;
        this.dialect = dialect;
        this.subIp = subIp;
        this.subPort = subPort;
        this.subProtocol = subProtocol;

    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getDialect(){ return dialect; }

    public String getSubProtocol() { return subProtocol; }

    public String getSubIp() { return subIp; }

    public String getSubPort() { return subPort; }


}


