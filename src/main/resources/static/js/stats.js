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

/**
 * Created by Håkon Ødegård Løvdal (hakloev) on 02/03/15.
 */

var Stats = (function($) {

    /*
     Creates, fills and returns a <tr>-element. The <tr>-element is generated based on the protocols
     list from the OKSE-RestAPI. This function does not manipulate the DOM by checking if an element exists.
     It overwrites everything.
     */
    var createTableForAllProtocols = function(protocols) {
        var trHTML = ""
        $.each(protocols, function(i, protocol) {
            trHTML +=
                '<tr>' +
                    '<td>' + protocol.protocolServer + '</td>' +
                    '<td>' + protocol.totalMessagesSent + '</td>' +
                    '<td>' + protocol.totalMessagesReceived + '</td>' +
                    '<td>' + protocol.totalRequests + '</td>' +
                    '<td>' + protocol.totalBadRequests + '</td>' +
                    '<td>' + protocol.totalErrors + '</td>' +
                '</tr>'
        });
        return trHTML
    }



    var refreshCoreServiceStatistics = function(statistics) {
        $('#messagesSent').html(statistics.totalMessagesSent)
        $('#messagesReceived').html(statistics.totalMessagesReceived)
        $('#totalRequests').html(statistics.totalRequests)
        $('#badRequests').html(statistics.totalBadRequests)
        $('#totalErrors').html(statistics.totalErrors)
        Main.refreshElementByClassWithText('.totalSubscribers', statistics.subscribers)
        Main.refreshElementByClassWithText('.totalPublishers', statistics.publishers)
        Main.refreshElementByClassWithText('.totalTopics', statistics.topics)
    }

    return {
        refresh: function(data) {
            var table = createTableForAllProtocols(data.protocolServerStatistics)
            $('#protocol-table').html(table)

            refreshCoreServiceStatistics(data.coreServiceStatistics)
        }
    }

})(jQuery);
