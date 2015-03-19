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
        Creates, fills and returns a tr element
     */
    var fillTable = function(data) {
        var trHTML = '';
        $.each(data.subscribers, function (i, subscriber) {
            if ($('#' + subscriber.ip).length === 0) {
                trHTML += '<tr>' +
                '<td>' + subscriber.protocol + '</td>' +
                '<td>' + subscriber.ip + '</td>' +
                '<td>' + subscriber.port + '</td>' +
                '<td><a id="' + subscriber.ip + '" class="btn btn-xs btn-block btn-warning delete-subscriber">Delete</a></td>' +
                '</tr>';
            }
        });
        trHTML += '<tr><td colspan="4"><a id="' + data.id + '" class="btn btn-block btn-danger delete-topic">Delete all</a></td></tr>';
        return trHTML
    }
    /*
        Iterates all the subscribers of this topic and overwrites the table with the new information
     */
    var updatePanel = function(data, panel) {
        $(panel).html(fillTable(data));
    }

    /*
        Sets up an basic template for a panel
     */
    var createPanelAndTableTemplate = function(topicName) {
        var panel = $('<div class="panel panel-primary">' +
        '<div class="panel-heading">' +
            '<h3 class="panel-title collapsed" data-toggle="collapse" data-target="#' + topicName.toLowerCase() + '">' +
                '<a href="#' + topicName.toLowerCase() + '">' + topicName +
        '</a></h3></div>' +
        '<div id="' + topicName.toLowerCase() +'" class="panel-collapse collapse">' +
            '<div class="table-reponsive"><table class="table table-striped">' +
                '<thead><tr><th>Protocol</th><th>IP</th><th>Port</th><th>Actions</th></tr></thead><tbody></tbody>' +
        '</table></div></div></div>')
        return panel
    }

    /*
        Creates a panel and table and updates it with the new information
     */
    var createPanel = function(data) {
        var panel = createPanelAndTableTemplate(data.topicName)
        $(panel).find('tbody').html(fillTable(data))
        $('#topics-column').append(panel);
    }

    var unBindButtons = function() {
        $('.delete-topic').off('click');
        $('.delete-subscriber').off('click');
    }

    var bindButtons = function() {
        // need to unbind all buttons between binding and add an id to topic a elements
        $('.delete-topic').on('click', function(e) {
            e.preventDefault();

            Main.ajax(("topics/delete/" + this.id), function() {
                console.log("Unable to remove topic")
            }, function() {
                $(e.target).closest('.panel').remove();
                console.log("Removing complete topic")
            }, "POST")


        });
        $('.delete-subscriber').on('click', function(e) {
            e.preventDefault();

            Main.ajax(("topics/delete/subscriber/" + this.id), function() {
                console.log("Unable to remove subscriber");
            }, function() {
                $(e.target).parent().parent().remove();
                console.log("Removing single subscriber");
            }, "POST")
        });
    }

    return {
        init: function() {
            $('#delete-all-topics').on('click', function(e) {
                e.preventDefault()

                Main.ajax("topics/delete/all", function() {
                    console.log("Unable to remove all topics");
                }, function() {
                    $('#topics-column').html('');
                    console.log("Removing all topics");
                }, "POST")

            });
        },
        // Ajax error function, should preferably update the site with information about this.
        error: function() {
          console.log("Error in Ajax for Topics")
        },
        // Ajax success function (updates all the information)
        refresh: function(response) {
            unBindButtons();

            var topicName = response.topicName.toLowerCase()

            if ($('#' + topicName).length === 0)  // If the topic doesn't already exist
                createPanel(response)
            else  // If the topic exist
                updatePanel(response, $('#' + topicName).find('tbody'))

            bindButtons();
        }

    }

})(jQuery);


