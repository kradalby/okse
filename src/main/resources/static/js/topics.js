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
 * Created by Fredrik Tørnvall and Håkon Ødegård Løvdal on 27/02/15.
 */

var Topics = (function($) {

    /*
     Creates, fills and returns a <tr>-element. The <tr>-element is generated based on the topics
     list from the OKSE-RestAPI. It also adds all the buttons needed for deleting topics. It uses the id for
     this purpose. This function does not manipulate the DOM by checking if an element exists. It overwrites everything.
     */
    var createTableForAllTopics = function(topics) {
        var trHTML = ""
        $.each(topics, function(i, topicInfo) {
            var topic = topicInfo.topic
            var subscribers = topicInfo.subscribers
            trHTML +=
                '<tr id="' + topic.topicID +'">' +
                    '<td>' + topic.fullTopicString + '</td>' +
                    '<td>' + '<span class="badge">' + subscribers + '</span></td>' +
                    '<td>' + topic.root + '</td>' +
                    '<td>' + topic.leaf + '</td>' +
                    '<td>' + '<a class="btn btn-xs btn-block btn-danger delete-topic">Delete</a></td>' +
                '</tr>'
        });
        return trHTML
    }

    /*
        Unbinds the buttons that change on every AJAX-response.
        Removes the 'click'-listener
     */
    var unBindButtons = function() { $('.delete-topic').off('click'); }

    /*
        Binds the buttons after the AJAX-request success function has completed
     */
    var bindButtons = function() {

        $('.delete-topic').on('click', function(e) {
            e.preventDefault();

            if (confirm("Are you sure you want to delete this topic? This will remove all subscribers and child topics.")) {
                // Disable the topic and buttons temporarily
                var topicID = $(e.target).closest("tr").attr("id")
                $(e.target).closest('tr').addClass('deleted')
                $(e.target).addClass("disabled")

                Main.ajax({
                    url: 'topic/delete/' + topicID,
                    type: 'DELETE',
                    success: function(data) {
                        $.okseDebug.logPrint("[Debug][Topics] Callback from server; topic and subscribers deleted")
                        /*
                        // Disabling all children topics also
                        $.each(data.children, function(i, topic) {
                            $('#' + topic.topicID).addClass('deleted')
                            $('#' + topic.topicID).find('a').each(function() {
                                $(this).addClass('disabled')
                            });
                        });
                        */
                    },
                    error: function(xhr, status, error) {
                        $.okseDebug.logPrint("[Debug][Topics] Unable to remove topic with id: " + e.target.id)
                        $(e.target).closest('tr').removeClass('deleted')
                        $(e.target).removeClass("disabled")
                        Main.displayMessage('Unable to remove topic!')
                        Main.error(xhr, status, error)
                    }
                });
            }
        });
    }

    return {
        init: function() {
            $('#delete-all-topics').on('click', function(e) {
                e.preventDefault()

                if (confirm("Are you sure you want to delete all the topics? This will also remove all subscriptions.")) {

                    Main.ajax({
                        url: 'topic/delete/all',
                        type: 'DELETE',
                        success: function(data) {
                            $.okseDebug.logPrint("[Debug][Topics] Callback from server; deleted all topics")
                            // Disable all topics and buttons
                            if (data.deleted == true) {
                                $('#topics-table').addClass('deleted')
                                $('#topics-table').find('a').each(function() {
                                    $(this).addClass('disabled')
                                });
                            }
                        },
                        error: function(xhr, status, error) {
                            Main.displayMessage('Unable to remove all topics!')
                            Main.error(xhr, status, error)
                        }
                    });
                }
            });
        },
        refresh: function(response) {
            unBindButtons();

            // This is done because unlike when we get all subscribers, which returnes an array,
            // this returns an object, and therefore we must count the keys
            var count = Object.keys(response).length

            Main.refreshElementByClassWithText('.totalTopics', count)

            // Remove 'deleted class' if it exists
            if ($('#topics-table').hasClass('deleted')) { $('#topics-table').removeClass('deleted'); }

            if (!(count == 0)) {
                var table = createTableForAllTopics(response)
                $('#topics-table').html(table)
            } else {
                $('#topics-table').html('<tr class="danger"><td colspan="6"><h4 class="text-center">No topics returned from TopicService</h4></td></tr>')
            }

            bindButtons();
        }
    }

})(jQuery);


