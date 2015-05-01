/**
 * Created by Håkon Ødegård Løvdal on 30/04/15.
 */

var Subscribers = (function($) {

    // Private variable holding the subscribers array returned upon the ajax-request
    var subscribers;

    var _PAGESIZE = 5;
    var _CURRENTPAGE = 1;
    var _TOTALPAGES = 10;

    var setupPagination = function() {
        $('#pagination-selector').twbsPagination({
            totalPages: numberOfPages(),
            startPage: _CURRENTPAGE,
            visiblePages: 5,
            onPageClick: function(e, page) {
                _CURRENTPAGE = page
                _TOTALPAGES = numberOfPages();
                var decrementedPage = page - 1
                var fromIndex = (_PAGESIZE * decrementedPage)
                var toIndex = (_PAGESIZE * decrementedPage) + _PAGESIZE
                var subscribersToPopulate = subscribers.slice(fromIndex, toIndex)
                $.okseDebug.logPrint("[Debug][Subscriber] Clicked page: " + page + " and trying to populate it with [" + fromIndex + "," + toIndex + "]")
                fillTable(subscribersToPopulate)
            }
        })
        if (subscribers.length > 0) {
            $.okseDebug.logPrint("[Debug][Subscriber] Append the correct table to the table")
            var decrementedPage = _CURRENTPAGE - 1
            var fromIndex = (_PAGESIZE * decrementedPage)
            var toIndex = (_PAGESIZE * decrementedPage) + _PAGESIZE
            var subscribersToPopulate = subscribers.slice(fromIndex, toIndex)
            fillTable(subscribersToPopulate)
        }
    }

    var numberOfPages = function() {
        return Math.ceil(subscribers.length / _PAGESIZE);
    }

    var checkIfPaginationIsNeeded = function() {
        var pageData = $('#pagination-selector').data();

        // Need to populate without paginator
        if (numberOfPages() < 2) {
            $.okseDebug.logPrint("[Debug][Subscriber] No need for paginator, filling table without paginator")
            if ( typeof pageData.twbsPagination != 'undefined') {
                $.okseDebug.logPrint("[Debug][Subscriber] Paginator exists, but we don't need it, so we destroy it")
                $('#pagination-selector').twbsPagination('destroy')
            }
            fillTable(subscribers)
            return;
        }


        if ( typeof pageData.twbsPagination == 'undefined' ) {
            $.okseDebug.logPrint("[Debug][Subscriber] Creating new paginator!")
            setupPagination()
        } else {
            // If _CURRENTPAGE is greater than needed pages, we decrement it to the needed numbers
            if (_CURRENTPAGE > numberOfPages()) {
                $.okseDebug.logPrint("[Debug][Subscriber] Decrementing the current page to needed number")
                _CURRENTPAGE = numberOfPages()
            }
            $.okseDebug.logPrint("[Debug][Subscriber] Paginator were defined and destroyed, initiating new with values: {" +
            "startPage:" + _CURRENTPAGE + " totalPages: " + numberOfPages() + "}");
            $('#pagination-selector').twbsPagination('destroy')
            setupPagination()
        }
    }

    var fillTable = function(subscribers) {
        unBindButtons()
        $('#subscribers-table').html(createTableForSubscribers(subscribers))
        bindButtons()
    }

    var createFilterSetString = function(filterSet) {
        if (filterSet.length == 0) {
            return "All"
        } else {
            var returnString = ""
            $.each(filterSet, function(i, filter) {
               returnString += filter + "\n"
            });
            return returnString;
        }
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
                '<td>' + subscriber.topic + '</td>' +
                '<td>' + subscriber.originProtocol + '</td>' + // TODO: Add support for no protocol here when available
                '<td>' + subscriber.host + '</td>' +
                '<td>' + subscriber.port + '</td>' +
                '<td>' + createFilterSetString(subscriber.filterSet) + '</td>' +
                '<td><a class="btn btn-xs btn-block btn-danger delete-subscriber">Delete</a></td>' +
                '</tr>';
        });
        return trHTML
    }

    var unBindButtons = function() {
        $('.delete-subscriber').off('click')
    }

    var bindButtons = function() {
         $('.delete-subscriber').on('click', function(e) {
            e.preventDefault()

             if (confirm("Are you sure you want to delete this subscriber?")) {

                 var subscriberID = $(e.target).closest('tr').attr('id')
                 $(e.target).closest("tr").addClass("deleted")
                 $(e.target).addClass("disabled")

                 Main.ajax({
                    url: 'subscriber/delete/' + subscriberID,
                    type: 'DELETE',
                    success: function(subscriber) {
                        $.okseDebug.logPrint("[Debug][Subscriber] Callback from server; subscriber deleted")
                    },
                    error: function(xhr, status, error) {
                        $.okseDebug.logPrint("[Debug][Subscriber] Unable to remove subscriber with id: " + e.target.id)
                        $(e.target).closest("tr").removeClass("deleted")
                        $(e.target).removeClass("disabled")
                        Main.error(xhr, status, error)
                    }
                });
            }
         });
    }

    return {
        init: function() {
            /*
             * Add a listener to clear interval
             * */
            $("#subscribers-button-refresh").on("click", function(e) {
                e.preventDefault()
                if (!$(this).hasClass("active")) {
                    $(this).addClass("active")
                    $(this).text("Stop refresh")
                    Main.setIntervalForTab({
                        url: 'subscriber/get/all',
                        type: 'GET',
                        success: Subscribers.refresh
                    })
                } else {
                    $(this).removeClass("active")
                    $(this).text("Start refresh")
                    Main.clearIntervalForTab()
                }
            })
        },
        refresh: function(data) {
            subscribers = data;
            checkIfPaginationIsNeeded()
        }
    }

})(jQuery);

