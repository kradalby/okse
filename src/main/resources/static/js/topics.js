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
     Creates, fills and returns a <tr>-element. The <tr>-element is generated based on the subscribers
     list from the OKSE-RestAPI. It also adds all the buttons needed for deleting subscribers. It uses the id for
     this purpose. This function does not manipulate the DOM by checking if an element exists. It overwrites everything.
     */
    var createTableForAllTopics = function(topics) {
        var trHTML = ""
        $.each(topics, function(i, topic) {
            trHTML +=
                '<tr id="' + topic.topicID +'">' +
                    '<td>' + topic.name + '</td>' +
                    '<td>' + topic.fullTopicString + '</td>' +
                    '<td>' + topic.root + '</td>' +
                    '<td>' + topic.leaf + '</td>' +
                    '<td>' + '<a class="btn btn-xs btn-block btn-warning show-subscribers" data-id="' + topic.topicID + '">Subscribers</a></td>' +
                    '<td>' + '<a class="btn btn-xs btn-block btn-danger delete-topic">Delete</a></td>' +
                '</tr>'
        });
        return trHTML
    }

    /*
     Creates, fills and returns a <tr>-element. The <tr>-element is generated based on the subscribers
     list from the OKSE-RestAPI. It also adds all the buttons needed for deleting subscribers. It uses the id for
     this purpose. This function does not manipulate the DOM by checking if an element exists. It overwrites everything.
     */
    var createTableForSubscribers = function(subscribers) {
        var trHTML = '';
        $.each(subscribers, function (i, subscriber) {
            trHTML +=
                '<tr id="'+ subscriber.subscriberID +' ">' +
                    '<td>' + subscriber.originProtocol + '</td>' +
                    '<td>' + subscriber.host + '</td>' +
                    '<td>' + subscriber.port + '</td>' +
                    '<td><a class="btn btn-xs btn-block btn-warning delete-subscriber">Delete</a></td>' +
                '</tr>';
        });
        return trHTML
    }

    /*
        Unbinds the buttons that change on every AJAX-response.
        Removes the 'click'-listener
     */
    var unBindButtons = function() {
        $('.delete-topic').off('click');
        $('.delete-subscriber').off('click');
        $('.show-subscriber').off('click');
    }

    /*
        Binds the buttons after
     */
    var bindButtons = function() {

        $('.delete-topic').on('click', function(e) {
            e.preventDefault();

            if (confirm("Are you sure you want to delete this topic? This will remove all subscribers and child topics.")) {

                var topicID = $(e.target).closest("tr").attr("id")
                $(e.target).closest('tr').addClass('deleted')
                $(e.target).addClass("disabled")

                Main.ajax({
                    url: 'topics/delete/' + topicID,
                    type: 'DELETE',
                    success: function(topic) {
                        console.log("[Debug][Topics] Callback from server; topic and subscribers deleted")
                    },
                    error: function(xhr, status, error) {
                        console.log("[Debug][Topics] Unable to remove topic with id: " + e.target.id)
                        $(e.target).closest('tr').removeClass('deleted')
                        $(e.target).removeClass("disabled")
                        Main.displayMessage('Unable to remove topic!')
                        Main.error(xhr, status, error)
                    }
                });
            }
        });

        $('.delete-subscriber').on('click', function(e) {
            e.preventDefault()

            if (confirm("Are you sure you want to delete this subscriber?")) {

                var subscriberID = $(e.target).closest('tr').attr('id')
                $(e.target).closest("tr").addClass("deleted")
                $(e.target).addClass("disabled")

                Main.ajax({
                    url: 'topics/delete/subscriber/' + subscriberID,
                    type: 'DELETE',
                    success: function(subscriber) {
                        console.log("[Debug][Topics] Callback from server; subscriber deleted")
                    },
                    error: function(xhr, status, error) {
                        console.log("[Debug][Topics] Unable to remove subscriber with id: " + e.target.id)
                        $(e.target).closest("tr").removeClass("deleted")
                        $(e.target).removeClass("disabled")
                        Main.error(xhr, status, error)
                    }
                });
            }
        });

        $('.show-subscribers').on('click', function(e) {
            e.preventDefault()

            console.log("[Debug][Topics] Showing subscribers modal for topic with id: " + $(this).data('id'))
            Main.ajax({
                url: 'topics/get/' + $(this).data('id') + '/subscriber/all',
                type: 'GET',
                success: function(data) {
                    console.log("[Debug][Topics] Callback from server; showing subscribers modal")
                    if (!(data.length == 0)) {
                        var table = createTableForSubscribers(data)
                        $('#subscribers-table').html(table)
                    } else {
                        $('#subscribers-table').html('<tr class="danger"><td colspan="4"><h4 class="text-center">No subscribers returned from SubscriptionService</h4></td></tr>')
                    }
                    $('#subscribers-modal').modal('show')
                },
                error: function(xhr, status, error) {
                    Main.displayMessage('Unable to show subscribers!')
                    Main.error(xhr, status, error)
                }
            });

            return false;
        });
    }

    return {
        init: function() {
            $('#delete-all-topics').on('click', function() {
                Main.ajax({
                    url: 'topics/delete/all',
                    type: 'DELETE',
                    success: function(data) {
                        console.log("[Debug][Topics] Callback from server; deleted all topics");
                        if (data.deleted == true) {
                            $('#topics-table').addClass('deleted')
                        }
                    },
                    error: function(xhr, status, error) {
                        Main.displayMessage('Unable to remove all topics!')
                        Main.error(xhr, status, error)
                    }
                });
            });
        },
        refresh: function(response) {
            unBindButtons();

            if ($('#topics-table').hasClass('deleted')) {
                $('#topics-table').removeClass('deleted');
            }

            if (!(response.length == 0)) {
                var table = createTableForAllTopics(response)
                $('#topics-table').html(table)
            } else {
                $('#topics-table').html('<tr class="danger"><td colspan="6"><h4 class="text-center">No topics returned from TopicService</h4></td></tr>')
            }

            bindButtons();
        }
    }

})(jQuery);


