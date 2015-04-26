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
 * Created by Fredrik Tørnvall and Håkon Ødegård Løvdal on 26/02/15.
 */
var Main = (function($) {

    // Private variable for error messages
    var statusErrorMap = {
        '400' : "Server understood the request, but request content was invalid.",
        '401' : "Unauthorized access.",
        '403' : "Forbidden resource can't be accessed.",
        '500' : "Internal server error.",
        '503' : "Service unavailable."
    };

    //  Private variable for holding the interval used to update the panes
    var clickInterval

    // The base url, that appends to all ajax-requests
    var BASE_URL = "/api/"

    /*
        Sets some default settings for every AJAX request, like the request url and data type.
    */
    var setupAjax = function() {
        $.ajaxSetup({
            error: error,
            dataType: 'json'
        });
        console.log("[Debug][Main] Successfully set up AJAX")
    }

    /*
        Global, generic AJAX-function for all AJAX-requests across the complete page.
        Takes in five arguments, defining the request. All urls are appended to BASE_URL
        All settings can be overridden by applying other inputs in the settings object.
        Sets up the AJAX with the correct CSRF-token (must do this to be able to do POST-requests)
     */
    var ajax = function(settings) {
        var token = $("meta[name='_csrf']").attr("content");
        var header = $("meta[name='_csrf_header']").attr("content");
        $.ajax({
            url: BASE_URL + settings.url,
            type: settings.type,
            dataType: settings.dataType,
            beforeSend: function(xhr) {
                xhr.url = settings.url
                xhr.setRequestHeader(header, token)
            },
            success: settings.success,
            error: settings.error

        })
    }

    /*
        Global function that sets the click interval for the log-tab after the user wants to activate it again.
     */
    var setIntervalForLogTab = function() {
        clearInterval(clickInterval)
        ajax({url: Logs.url(), type: 'GET', success: Logs.refresh})
        clickInterval = setInterval(function () {
            ajax({
                url: Logs.url(),
                type: 'GET',
                success: Logs.refresh
            });
        }, $('#settings-update-interval').val() * 1000);
    }

    // TODO: For now, it only updates when in main-pane. Needs to change in stats pane later on
    // Updates all the subscriber counters on the page (both the main-pane and the stats-pane
    var updateSubscribers = function(subscribers) {
        $('.total-subscribers').each(function() {
            if ($(this).val() != subscribers) {
                $(this).text(subscribers)
            }
        });
    }

    /*
        Global error function that shows the Ajax callback and request url.
     */
    var error = function(xhr, status, error)    {
        if (xhr.status != 200) {
            var errorMessage = statusErrorMap[xhr.status]
            if (!errorMessage) { errorMessage = "Unknown error" }
            console.error("[Error][" + xhr.url + "] in Ajax with the following callback {" +
                "status: " + xhr.status +  " " +
                "errorMessage: " + errorMessage+ " " +
                "readyState: " + xhr.readyState + " " +
                "responseText: " + xhr.responseText + "}")
        }
    }

    var refresh = function(response) {
        updateSubscribers(response.subscribers)
    }

    return {
        ajax: ajax,
        error: error,
        setIntervalForLogTab: setIntervalForLogTab,
        clearIntervalForTab: function() {
            clearInterval(clickInterval)
        },
        displayMessage: function(message) {
            $('#messages').append(
                '<div class="alert alert-danger">' +
                    '<a class="close" data-dismiss="alert">&times;</a>' +
                    '<strong>Error: </strong>' + message +
                '</div>');
        },
        init: function() {
            setupAjax()

            $(".nav-tabs").on("click", "a", function(e){
                clearInterval(clickInterval)
                var clickedElement = $(this).attr("href").substring(1)
                var updateInterval = $('#settings-update-interval').val() * 1000

                var ajaxSettings = {
                    url: clickedElement,
                    type: 'GET'
                };

                switch (clickedElement) {
                    case "main":
                        ajaxSettings.success = refresh
                        break;
                    case "topics":
                        ajaxSettings.url = clickedElement + '/get/all'
                        ajaxSettings.success = Topics.refresh
                        break;
                    case "stats":
                        ajaxSettings.success = Stats.refresh
                        break;
                    case "log":
                        ajaxSettings.url = Logs.url()
                        ajaxSettings.success = Logs.refresh
                        break;
                    case "config":
                        ajaxSettings.success = Config.refresh
                        break;
                    default:
                        console.error("[Error][Main] Unknown nav-tab clicked, this should not happen!")
                }

                ajax(ajaxSettings)
                clickInterval = setInterval( function() {
                   ajax(ajaxSettings)
                }, updateInterval);

            });

            if ($('#main').length) {
                clickInterval = setInterval( function() {
                    ajax({
                        url: 'main',
                        type: 'GET',
                        success: refresh
                    })}, 2000);
                Logs.init()
            }
        }
    }

})(jQuery);

$(document).ready(function(){
    Main.init()
    Topics.init()
    Config.init()
});

