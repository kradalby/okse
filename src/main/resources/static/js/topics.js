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
 * Created by Fredrik on 27/02/15.
 */

var Topics = (function($) {

    /*
        Iterates all the subscribers of this topic and overwrites the table with the new information
     */
    var updatePanel = function(subscribers, panel) {
        var trHTML = '';
        $.each(subscribers, function (i, subscriber) {
            trHTML += '<tr><td>' + subscriber.protocol + '</td><td>' + subscriber.ip + '</td><td>' + subscriber.port + '</td></tr>';
        });
        $(panel).html(trHTML);
    }


    /*
        Creates a panel and table and updates it with the new information
     */
    var createPanel = function(data) {
        var trHTML = '';
        $.each(response.subscribers, function (i, subscriber) {
            trHTML +=
                '<tr><td>' + subscriber.protocol + '</td><td>' + subscriber.ip + '</td><td>' + subscriber.port + '</td></tr>';
        });
        $('#topics-column').append(trHTML);
    }

    return {
        // Ajax error function, should preferably update the site with information about this.
        error: function() {
          console.log("Error in Ajax for Topics")
        },
        // Ajax success function (updates all the information
        refresh: function(response) {
            var topicName = response.topicName.toLowerCase()
            if ($('#' + topicName).length === 0) { // If the topic doesn't already exist
                createPanel(response)
            } else { // If the topic exist
                updatePanel(response.subscribers, $('#' + topicName).find('tbody'))
            }
        }
    }

})(jQuery);

