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

console.log("loaded")
$(document).ready(function(){
    $("#load").click(function(){
        var load = $.ajax({
            url: "http://localhost:8080/topics",
            dataType: "json",
            error: function(error) {
                console.log("error in ajax")
            },
            success: function(data) {
                $('#col3').html(JSON.stringify(data))
                console.log("success" + JSON.stringify(data))
            },
            type: "GET"
        })
    });

    var test = setInterval( function()
    {
        var load = $.ajax({
            url: "http://localhost:8080/topics",
            dataType: "json",
            error: function(error) {
                console.log("error in ajax")
            },
            success: function(data) {
                $('#col3').html(JSON.stringify(data))
                console.log("success" + JSON.stringify(data))
            },
            type: "GET"
        })


    }, 5000);
});

