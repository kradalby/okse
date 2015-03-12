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
 * Created by Fredrik on 26/02/15.
 */


var Main = (function($) {

    var clickInterval;

    var ajax = function(location, error, success) {
        $.ajax({
            url: "http://localhost:8080/api/" + location.substring(1),
            dataType: "json",
            error: error,
            success: success,
            type: "GET"
        })
    };

    var error = function() {
        console.log("Error in main ajax")
    }

    var refresh = function(data) {
        console.log(JSON.stringify(data))
    }

    return {
        init: function() {
            $(".nav-tabs").on("click", "a", function(e){
                clearInterval(clickInterval)
                var clickedElement = $(this).attr("href")
                var updateInterval = $('#settings-update-interval').val() * 1000

                if (clickedElement === "#main")
                {
                    ajax(clickedElement, error, refresh)
                    clickInterval = setInterval( function() {
                        ajax(clickedElement, error, refresh)
                    }, updateInterval);
                } else if(clickedElement === "#topics")
                {
                    ajax(clickedElement, Topics.error, Topics.refresh)
                    clickInterval = setInterval( function() {
                        ajax(clickedElement, Topics.error, Topics.refresh)
                    }, updateInterval);
                } else if (clickedElement === "#stats")
                {
                    ajax(clickedElement, Stats.error, Stats.refresh)
                    clickInterval = setInterval( function() {
                        ajax(clickedElement, Stats.error, Stats.refresh)
                    }, updateInterval);
                } else if (clickedElement === "#config")
                {
                    ajax(clickedElement, Config.error, Config.refresh)
                    clickInterval = setInterval( function() {
                        ajax(clickedElement, Config.error, Config.refresh)
                    }, updateInterval);
                } else
                {
                    console.log("Unknown tab, should not happen!")
                }
            });

            if ($('#main').length) {
                clickInterval = setInterval( function() {
                    ajax("#main", error, refresh)
                }, 2000);
            }
        }
    }

})(jQuery)

$(document).ready(function(){
    Main.init()
});

