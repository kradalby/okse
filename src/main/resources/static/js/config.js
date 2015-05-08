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
 * Created by Fredrik Tørnvall (freboto) and Håkon Ødegård Løvdal (hakloev) on 02/03/15.
 */


var Config = (function($) {

    var createTableForAllRelays = function(relays) {
        var trHTML = ""
        $.map(relays, function(value, key) {
            trHTML +=
                '<tr>' +
                    '<td>' + value.split('subscriptionManager')[0] + '</td>' +
                    '<td>' + '<a class="btn btn-xs btn-block btn-danger delete-relay" id="' + key + '">Delete</a></td>' +
                '</tr>'
        });
        return trHTML
    }

    /*
     Creates, fills and returns a <tr>-element. The <tr>-element is generated based on the mappings
     list from the OKSE-RestAPI. It also adds all the buttons needed for deleting mappings. It uses the origin topic for
     this purpose. This function does not manipulate the DOM by checking if an element exists. It overwrites everything.
     */
    var createTableForAllMappings = function(mappings) {
        var trHTML = ""
        $.map(mappings, function(value, key) {
            $.each(value, function(i, toTopic) {
                trHTML +=
                    '<tr>' +
                        '<td>' + key + '</td>' +
                        '<td>' + toTopic + '</td>' +
                        '<td>' + '<a class="btn btn-xs btn-block btn-danger delete-mapping" id="topic-' + i + '-' + key + '">Delete</a></td>' +
                    '</tr>'
            });
        });
        return trHTML
    }

    var unBindButtons = function() {
        $('.delete-mapping').off('click')
    }

    var bindButtons = function() {

        $('.delete-mapping').on('click', function(e) {
            e.preventDefault();

            if (confirm("Are you sure you want to delete this mapping?")) {
                // Disable the topic and buttons temporarily
                var fromTopic = $(e.target).attr('id')
                fromTopic = fromTopic.split('-').pop()
                $.okseDebug.logPrint("Trying to remove mapping for topic: " + fromTopic)
                $(e.target).closest('tr').addClass('deleted')
                $(e.target).addClass("disabled")

                Main.ajax({
                    url: 'config/mapping/delete/single?topic=' + fromTopic,
                    type: 'DELETE',
                    success: function(data) {
                        $.okseDebug.logPrint("[Debug][Config] Callback from server; mapping deleted")
                    },
                    error: function(xhr, status, error) {
                        $.okseDebug.logPrint("[Debug][Config] Unable to remove mapping for topic: " + fromTopic)
                        $(e.target).closest('tr').removeClass('deleted')
                        $(e.target).removeClass("disabled")

                        var data = JSON.parse(xhr.responseText)
                        Main.error(xhr, status, error)
                        Main.displayMessage(data.message)
                    }
                });
            }
        });

        $('.delete-relay').on('click', function(e) {
            e.preventDefault();

            if (confirm("Are you sure you want to delete this relay?")) {
                // Disable the topic and buttons temporarily
                var relay = $(e.target).attr('id')
                $.okseDebug.logPrint("Trying to remove relay: " + relay)
                $(e.target).closest('tr').addClass('deleted')
                $(e.target).addClass("disabled")

                Main.ajax({
                    url: 'config/relay/delete/single?relayID=' + relay,
                    type: 'DELETE',
                    success: function(data) {
                        $.okseDebug.logPrint("[Debug][Config] Callback from server; relay deleted")
                    },
                    error: function(xhr, status, error) {
                        $.okseDebug.logPrint("[Debug][Config] Unable to remove relay: " + relay)
                        $(e.target).closest('tr').removeClass('deleted')
                        $(e.target).removeClass("disabled")

                        var data = JSON.parse(xhr.responseText)
                        Main.error(xhr, status, error)
                        Main.displayMessage(data.message)
                    }
                });
            }
        });

    }

    return {
        refresh: function(response) {
            unBindButtons()

            var countMappings = Object.keys(response.mappings).length
            var countRelays = Object.keys(response.relays).length

            if ( ! countMappings == 0 ) {
                $('#mappings-table').html(createTableForAllMappings(response.mappings))
            } else {
                $('#mappings-table').html('<tr class="danger"><td colspan="3"><h4 class="text-center">No mappings returned from TopicService</h4></td></tr>')
            }

            if ( ! countRelays == 0) {
                $('#relays-table').html(createTableForAllRelays(response.relays))
            } else {
                $('#relays-table').html('<tr class="danger"><td colspan="2"><h4 class="text-center">No relays returned from ConfigController</h4></td></tr>')
            }

            bindButtons()

        },
        init: function() {
            $('#add-relay').on('click', function(e) {
                e.preventDefault()

                var url = ('config/relay/add?from=' + $('#relay-from').val())

                var relayTopic = $('#relay-topic').val()
                if (relayTopic.length != 0) {
                    url += ('&topic=' + encodeURIComponent(relayTopic))
                }

                Main.ajax({
                    url: url,
                    type: 'POST',
                    success: function(data) {
                        $.okseDebug.logPrint("[Debug][Config] Callback from server; added relay")
                    },
                    error: function(xhr, status, error) {
                        var data = JSON.parse(xhr.responseText)
                        Main.error(xhr, status, error)
                        Main.displayMessage(data.message)

                    }
                })
            })

            $('#add-mapping').on('click', function(e) {
                e.preventDefault()

                Main.ajax({
                    url: 'config/mapping/add?fromTopic=' + encodeURIComponent($('#from-topic').val()) + '&toTopic=' + encodeURIComponent($('#to-topic').val()),
                    type: 'POST',
                    success: function(data) {
                        $.okseDebug.logPrint("[Debug][Config] Callback from server; added mapping")
                    },
                    error: function(xhr, status, error) {
                        var data = JSON.parse(xhr.responseText)
                        Main.error(xhr, status, error)
                        Main.displayMessage(data.message)
                    }
                });
            });

            $('#delete-all-relays').on('click', function(e){
                e.preventDefault()

                if (confirm("Are you sure you want to delete all relays?")) {

                    Main.ajax({
                        url: 'config/relay/delete/all',
                        type: 'DELETE',
                        success: function(data) {
                            $.okseDebug.logPrint("[Debug][Config] Callback from server; deleted all relays")
                        },
                        error: function(xhr, status, error) {
                            var data = JSON.parse(xhr.responseText)
                            Main.error(xhr, status, error)
                            Main.displayMessage(data.message)
                        }
                    })

                }
            });

            $('#delete-all-mappings').on('click', function(e) {
                e.preventDefault()

                if (confirm("Are you sure you want to delete all mappings?")) {

                    Main.ajax({
                        url: 'config/mapping/delete/all',
                        type: 'DELETE',
                        success: function (data) {
                            $.okseDebug.logPrint("[Debug][Config] Callback from server; deleted all mappings")
                        },
                        error: function (xhr, status, error) {
                            var data = JSON.parse(xhr.responseText)
                            Main.error(xhr, status, error)
                            Main.displayMessage(data.message)
                        }
                    });
                }
            });

            $('#AMQP-topic-config').on('click', function(e) {
                e.preventDefault()

                Main.ajax({
                    url: 'config/mapping/queue/change',
                    type: 'POST',
                    success: function(data) {
                        $.okseDebug.logPrint("[Debug][Config] Callback from server; AMQP useQueue value changed")
                        $.okseDebug.logPrint("[Debug][Config] AMPQ useQueue is now" + data.value)
                        $(e.target).prop('checked', data.value)
                    },
                    error: function(xhr, status, error) {
                        var data = JSON.parse(xhr.responseText)
                        Main.error(xhr, status, error)
                        Main.displayMessage(data.message)
                    }
                })
            });
        }
    }


})(jQuery)