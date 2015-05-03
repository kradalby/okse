/**
 * Created by Håkon Ødegård Løvdal on 30/04/15.
 */

var Subscribers = (function($) {

    // Private variable holding the subscribers array returned upon the ajax-request.
    var subscribers;

    var _PAGESIZE = 25; // Variable holding the size of each paginator page.
    var _CURRENTPAGE = 1; // Variable holding the last page showed in the paginator.

    /*
        This function sets up the pagination with the correct values.
        It also fills the new paginator with the correct table.
     */
    var setupPagination = function() {
        $('#pagination-selector').twbsPagination({
            totalPages: numberOfPages(),
            startPage: _CURRENTPAGE,
            visiblePages: 10,
            onPageClick: function(e, page) {
                _CURRENTPAGE = page
                var decrementedPage = (page - 1),
                    fromIndex = (_PAGESIZE * decrementedPage),
                    toIndex = (_PAGESIZE * decrementedPage) + _PAGESIZE
                $.okseDebug.logPrint("[Debug][Subscriber] Clicked page: " + page + " and trying to populate it with [" + fromIndex + "," + toIndex + "]")
                fillTable(fromIndex, toIndex)
            }
        })
        if (subscribers.length > 0) {
            var decrementedPage = (_CURRENTPAGE - 1),
                fromIndex = (_PAGESIZE * decrementedPage),
                toIndex = (_PAGESIZE * decrementedPage) + _PAGESIZE
            fillTable(fromIndex, toIndex)
        }
    }

    /*
        This function calculates the pages needed for the correct subscribers list.
     */
    var numberOfPages = function() {
        return Math.ceil(subscribers.length / _PAGESIZE);
    }

    /*
        This function does all checks required to find out the correct paginator to append (or remove)
        from the DOM. See each check to see what they do.
     */
    var checkIfPaginationIsNeeded = function() {
        var pageData = $('#pagination-selector').data(); // This variable holds the data-object for the paginator-element.

        // Do we need a paginator to populate the table?
        if (numberOfPages() < 2) {
            $.okseDebug.logPrint("[Debug][Subscriber] There is no need for the paginator; filling table without it")
            if ( typeof pageData.twbsPagination != 'undefined') {
                $.okseDebug.logPrint("[Debug][Subscriber] Paginator already exists, but we don't need it, so we destroy it")
                $('#pagination-selector').twbsPagination('destroy')
            }
            fillTable()
            return;
        }


        // If the data-object for twbsPagination is undefined, we create a new one; else we create one
        // based on the needed pages.
        if ( typeof pageData.twbsPagination == 'undefined' ) {
            $.okseDebug.logPrint("[Debug][Subscriber] Creating a new paginator!")
            _CURRENTPAGE = 1; // Need to reset the _CURRENTPAGE
            setupPagination()
        } else {
            // If _CURRENTPAGE is greater than needed pages, we decrement it to the needed numbers
            if (_CURRENTPAGE > numberOfPages()) {
                $.okseDebug.logPrint("[Debug][Subscriber] Decrementing the current page to needed number since _CURRENTPAGE > numberOfPages()")
                _CURRENTPAGE = numberOfPages()
            }
            $.okseDebug.logPrint("[Debug][Subscriber] Paginator were defined but destroyed to initiate with new values: {" +
                "startPage:" + _CURRENTPAGE + " totalPages: " + numberOfPages() + "}");
            $('#pagination-selector').twbsPagination('destroy')
            setupPagination()
        }
    }

    /*
        This function fills the #subscribers-table with the given input list
     */
    var fillTable = function(from, to) {
        unBindButtons()
        if (subscribers.length == 0) {
            $('#subscribers-table').html('<tr class="danger"><td colspan="6"><h4 class="text-center">No subscribers returned from SubscriptionsService</h4></td></tr>')
            return;
        }
        if (from === undefined || to === undefined) {
            $.okseDebug.logPrint("[Debug][Subscriber] Filling table with the complete list")
            $('#subscribers-table').html(createTableForSubscribers(subscribers))
        } else {
            $.okseDebug.logPrint("[Debug][Subscriber] Filling table with [" + from + "," + to + "]")
            var subscribersToPopulate = subscribers.slice(from, to)
            $('#subscribers-table').html(createTableForSubscribers(subscribersToPopulate))
        }
        bindButtons()
    }

    // Create a string of all filters to fill the table cell with
    var createFilterSetString = function(filterSet) {
        if ( ! filterSet.length == 0) {
            var returnString = ""
            $.each(filterSet, function(i, filter) {
               returnString += filter + "\n"
            });
            return returnString;
        } else {
            return "";
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
                '<td>' + ((subscriber.topic == null) ? '*' : subscriber.topic) + '</td>' +
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
            $('#delete-all-subscribers').on('click', function(e) {
                e.preventDefault()

                if (confirm("Are you sure you want to delete all subscribers?")) {

                    Main.ajax({
                        url: 'subscriber/delete/all',
                        type: 'DELETE',
                        success: function(data) {
                            $.okseDebug.logPrint("[Debug][Topics] Callback from server; deleted all subscribers")
                            // Disable all subscribers and buttons
                            if (data.deleted == true) {
                                $('#subscribers-table').addClass('deleted')
                                $('#subscribers-table').find('a').each(function() {
                                    $(this).addClass('disabled')
                                });
                            }
                        },
                        error: function(xhr, status, error) {
                            Main.displayMessage('Unable to remove all subscribers!')
                            Main.error(xhr, status, error)
                        }
                    });
                }
            });
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
            // Remove 'deleted class' if it exists
            Main.refreshElementByClassWithText('.totalSubscribers', subscribers.length)
            if ($('#subscribers-table').hasClass('deleted')) { $('#subscribers-table').removeClass('deleted'); }
        }
    }

})(jQuery);

