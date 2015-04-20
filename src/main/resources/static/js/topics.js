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
        Creates, fills and returns a <tr>-element. The <tr>-element is generated based on the subscribers
        list from the OKSE-RestAPI. It also adds all the buttons needed for deleting subscribers. It uses the id for
        this purpose. This function does not manipulate the DOM by checking if an element exists. It overwrites everything.
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
        Iterates all the subscribers of this topic and overwrites the table with the new information.
     */
    var updatePanel = function(data, panel) {
        //$(panel).html(fillTable(data));
    }

    /*
        Sets up an basic template for a panel
     */
    var createPanelAndTableTemplate = function(topicID, topicName) {
        var panel = $(
            '<div class="panel panel-primary">' +
                '<div class="panel-heading">' +
                    '<h3 class="panel-title collapsed" data-toggle="collapse" data-target="#' + topicID + '">' +
                    '<a href="#' + topicID + '">' + topicName + '</a></h3>' +
                '</div>' +
                '<div id="' + topicID +'" class="panel-collapse collapse">' +
                    '<div class="table-reponsive">' +
                        '<table class="table table-striped">' +
                            '<thead><tr><th>Protocol</th><th>IP</th><th>Port</th><th>Actions</th></tr></thead><tbody></tbody>' +
                        '</table>' +
                    '</div>' +
                '</div>' +
            '</div>')
        return panel
    }

    /*
        Creates a panel and table and updates it with the new information
     */
    var createPanel = function(topic) {
        var panel = createPanelAndTableTemplate(topic.topicID, topic.fullTopicString)
        //$(panel).find('tbody').html(fillTable(topic))
        $('#topics-column').append(panel);
    }

    /*
        Unbinds the buttons that change on every AJAX-response.
        Removes the 'click'-listener
     */
    var unBindButtons = function() {
        $('.delete-topic').off('click');
        $('.delete-subscriber').off('click');
    }

    /*
        Binds the buttons after
     */
    var bindButtons = function() {
        // need to unbind all buttons between binding and add an id to topic a elements
        $('.delete-topic').on('click', function(e) {
            e.preventDefault();

            Main.ajax(("topics/delete/" + this.id), function() {
                console.log("[Debug][Topics] Unable to remove topic")
            }, function() {
                $(e.target).closest('.panel').remove();
                console.log("[Debug][Topics] Removing complete topic")
            }, "POST")


        });
        $('.delete-subscriber').on('click', function(e) {
            e.preventDefault();

            Main.ajax(("topics/delete/subscriber/" + this.id), function() {
                console.log("[Debug][Topics] Unable to remove subscriber");
            }, function() {
                $(e.target).parent().parent().remove();
                console.log("[Debug][Topics] Removing single subscriber");
            }, "POST")
        });
    }

    return {
        init: function() {
            $('#delete-all-topics').on('click', function(e) {
                e.preventDefault()

                Main.ajax("topics/delete/all", function() {
                    console.log("[Debug][Topics] Unable to remove all topics");
                }, function() {
                    $('#topics-column').html('');
                    console.log("[Debug][Topics] Removing all topics");
                }, "POST")

            });
        },
        error: function(xhr, status, error) {
          console.error("[Error][Topics] in Ajax with the following callback [status: " + xhr.status +  " readyState: " + xhr.readyState + " responseText: " + xhr.responseText + "]")
        },
        refresh: function(response) {
            unBindButtons();

            $.each(response, function(i, topic) {
                var topicID = topic.topicID
                if ($('#' + topicID).length === 0)  // If the topic doesn't already exist
                    createPanel(topic)
                else  // If the topic exist
                    updatePanel(topic, $('#' + topicID).find('tbody'))

            })



            bindButtons();
        }
    }

})(jQuery);


