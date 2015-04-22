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

    // Global variable for holding the interval used to update the panes
    var clickInterval;

    /*
        Private method for setting up the AJAX with the correct CSRF-token
        (must do this to be able to do POST-requests)
    */
    var setupAjax = function() {
        var token = $("input[name='_csrf']").val();
        var header = "X-CSRF-TOKEN";
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(header, token);
            }
        });
        console.log("[Debug][Main] Successfully set up AJAX")
    }

    /*
        Global, generic AJAX-function for all AJAX-requests across the complete page.
        Takes in five arguments, defining the request. All urls are appended to '/api/'
     */
    var ajax = function(settings) {
        $.ajax({
            url: "/api/" + settings.url,
            type: settings.type,
            dataType: settings.dataType,
            beforeSend: function(xhr, settings) {
                xhr.url = settings.url
            },
            success: settings.success,
            error: settings.error

        })
    };

    var setIntervalForLogTab = function() {
        clickInterval = setInterval(function () {
            Main.ajax({
                url: Logs.url(),
                type: 'GET',
                dataType: 'json',
                success: Logs.success,
                error: error
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

    var error = function(xhr, status, error)    {
        console.error("[Error][" + xhr.url + "] in Ajax with the following callback [status: " + xhr.status +  " readyState: " + xhr.readyState + " responseText: " + xhr.responseText + "]")
    }

    var refresh = function(response) {
        updateSubscribers(response.subscribers)
        console.log("[Debug][Main]" + JSON.stringify(response))
    }

    return {
        ajax: ajax,
        error: error,
        init: function() {
            setupAjax()
            $(".nav-tabs").on("click", "a", function(e){
                clearInterval(clickInterval)
                var clickedElement = $(this).attr("href").substring(1)
                var updateInterval = $('#settings-update-interval').val() * 1000

                var ajaxSettings = {
                    url: clickedElement,
                    type: 'GET',
                    dataType: 'json',
                    error: error
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
                        dataType: 'json',
                        success: refresh,
                        error: error
                    })}, 2000);
            }
        },
        clearIntervalForTab: function() {
            clearInterval(clickInterval)
        },
        setIntervalForLogTab: setIntervalForLogTab
    }

})(jQuery);

$(document).ready(function(){
    Main.init()
    Config.init()
    Logs.init()
});

