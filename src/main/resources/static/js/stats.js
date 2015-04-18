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
     Creates, fills and returns a tr element
     */
    var fillTable = function(data) {
        var HTML = '';
        $.each(data.protocols, function (i, protocolstats) {
            if ($('#' + protocolstats.protocolServerType).length === 0) {
                HTML +=
                    '<div class="panel-heading" id="" + protocolstats.protocolServerType><strong>' + protocolstats.protocolServerType + '</strong></div>' +
                        '<ul class="list-group">' +
                        '<li class="list-group-item"> <strong>Requests handled:</strong> ' + protocolstats.totalRequests + '</li>' +
                        '<li class="list-group-item"> <strong>Messages sent:</strong> ' + protocolstats.totalMessages + '</li>' +
                        '</ul>'
            }
        });
        return HTML
    }

    return {
        error: function(xhr, statusText, thrownError) {
            console.error("[Error] in ajax for Stats with error: " + xhr.statusText)
        },
        refresh: function(data) {
            $('#stats-total-messages').html('<strong>Messages sent: </strong>' + data.totalMessages)
            $('#stats-total-requests').html('<strong>Requests handled: </strong>' + data.totalRequests)

            $('#stats-total-badrequests').html('<strong>Bad requests: </strong>' + data.totalBadRequests)
            $('#stats-total-error').html('<strong>Total errors: </strong>' + data.totalErrors)

            $('#totalram').html('<strong>Total RAM: </strong>' + data.ramTotal + ' MB')
            $('#freeram').html('<strong>Free RAM: </strong>' + data.ramFree + ' MB')
            $('#ramuse').html('<strong>Used RAM: </strong>' + data.ramUse + ' MB')
            $('#cpucores').html('<strong>CPU cores: </strong>' + data.cpuUse)
            $('#protocolList').html(fillTable(data))
        }
    }

})(jQuery)
