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

    // Private method for setting up the AJAX with the correct CSRF-token (must do this to be able to do POST-requests)
    var setupAjax = function() {
        var token = $("input[name='_csrf']").val();
        var header = "X-CSRF-TOKEN";
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(header, token);
            }
        });
    }

    // Global, generic AJAX-function for all AJAX-requests across the complete page
    var ajax = function(url, error, success, httpMethod, dataType) {
        $.ajax({
            url: "/api/" + url,
            dataType: dataType,
            error: error,
            success: success,
            type: httpMethod
        })
    };

    // TODO: For now, it only updates when in main-pane. Needs to change in topic pane later on
    // Updates all the subscriber counters on the page (both the main-pane and the topics-pane
    var updateSubscribers = function(subscribers) {
        $('.total-subscribers').each(function() {
            if ($(this).val() != subscribers) {
                $(this).text(subscribers)
            }
        });
    }

    var error = function(xhr, statusText, thrownError) {
        console.log("[Error] in Ajax for main with status: " + xhr.statusText)
    }

    var refresh = function(response) {
        updateSubscribers(response.subscribers)
        console.log(JSON.stringify(response))
    }

    return {
        ajax: ajax,
        init: function() {
            setupAjax()
            $(".nav-tabs").on("click", "a", function(e){
                clearInterval(clickInterval)
                var clickedElement = $(this).attr("href")
                var updateInterval = $('#settings-update-interval').val() * 1000

                if (clickedElement === "#main")
                {
                    ajax(clickedElement.substring(1), error, refresh, "GET", "json")
                    clickInterval = setInterval( function() {
                        ajax(clickedElement.substring(1), error, refresh, "GET", "json")
                    }, updateInterval);
                } else if(clickedElement === "#topics")
                {
                    ajax(clickedElement.substring(1), Topics.error, Topics.refresh, "GET", "json")
                    clickInterval = setInterval( function() {
                        ajax(clickedElement.substring(1), Topics.error, Topics.refresh, "GET", "json")
                    }, updateInterval);
                } else if (clickedElement === "#stats")
                {
                    ajax(clickedElement.substring(1), Stats.error, Stats.refresh, "GET", "json")
                    clickInterval = setInterval( function() {
                        ajax(clickedElement.substring(1), Stats.error, Stats.refresh, "GET", "json")
                    }, updateInterval);
                } else if (clickedElement === "#config")
                {
                    ajax(clickedElement.substring(1), Config.error, Config.refresh, "GET")
                    clickInterval = setInterval( function() {
                        ajax(clickedElement.substring(1), Config.error, Config.refresh, "GET")
                    }, updateInterval);
                } else
                {
                    console.log("Unknown tab, should not happen!")
                }
            });

            if ($('#main').length) {
                clickInterval = setInterval( function() {
                    ajax("main", error, refresh, "GET", "json")
                }, 2000);
            }
        }
    }

})(jQuery)

$(document).ready(function(){
    Main.init()
    Topics.init()
    Config.init()
});

