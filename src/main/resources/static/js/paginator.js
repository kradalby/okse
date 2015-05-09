/**
 * Created by Håkon Ødegård Løvdal on 05/05/15.
 */

/*
    This module holds logic for the paginator used by topic.js and subscribers.js
 */
var Paginator = (function($) {

    var _PAGESIZE = 25

    /*
     This function sets up the pagination with the correct values.
     It also fills the new paginator with the correct table.
     */
    var setupPagination = function(args) {
        $('#' + args.element).twbsPagination({
            totalPages: args.pagesNeeded,
            startPage: args.currentPage,
            visiblePages: 10,
            onPageClick: function(e, page) {
                args.setCurrentPage(page)
                var decrementedPage = (page - 1),
                    fromIndex = (_PAGESIZE * decrementedPage),
                    toIndex = (_PAGESIZE * decrementedPage) + _PAGESIZE
                $.okseDebug.logPrint("[Debug][Paginator] Clicked page: " + page + " and trying to populate it with [" + fromIndex + "," + toIndex + "]")
                args.fillTableCallback(fromIndex, toIndex)
            }
        })

         if (args.listLength > 0) {
            var decrementedPage = (args.currentPage - 1),
                fromIndex = (_PAGESIZE * decrementedPage),
                toIndex = (_PAGESIZE * decrementedPage) + _PAGESIZE
            args.fillTableCallback(fromIndex, toIndex)
         }
    }

    var existenceCheck = function(paginatorElement) {
        var pageData = $('#' + paginatorElement).data(); // This variable holds the data-object for the paginator-element.

        if ( typeof pageData.twbsPagination != 'undefined') {
            $.okseDebug.logPrint("[Debug][Paginator] Paginator exists, so it's destroyed")
            $('#' + paginatorElement).twbsPagination('destroy')
        }
    }

    /*
     This function does all checks required to find out the correct paginator to append (or remove)
     from the DOM. See each check to see what they do.
     */
    var requiredCheck = function(args) {
        var pageData = $('#' + args.element).data(); // This variable holds the data-object for the paginator-element.

        if (args.pagesNeeded < 2) {
            existenceCheck(args.element)
            args.fillTableCallback()
            return;
        }

        // If the data-object for twbsPagination is undefined, we create a new one; else we create one
        // based on the needed pages.
        if ( typeof pageData.twbsPagination == 'undefined' ) {
            $.okseDebug.logPrint("[Debug][Paginator] Creating a new paginator")
            args.currentPage = 1; // Need to reset the currentPage
            setupPagination(args)
        } else {
            // If currentPage is greater than needed pages, we decrement it to the needed numbers
            if (args.currentPage > args.pagesNeeded) {
                $.okseDebug.logPrint("[Debug][Paginator] Decrementing the current page to needed number since currentPage > pagesNeeded")
                args.currentPage = args.pagesNeeded
            }

            $.okseDebug.logPrint("[Debug][Paginator] Paginator were already defined but destroyed to initiate with new values: {" +
                "startPage:" + args.currentPage + " totalPages: " + args.pagesNeeded + "}");

            $('#' + args.element).twbsPagination('destroy')

            setupPagination(args)
        }
    }

    return {
        tools: {
            checkIfPaginationIsNeeded: requiredCheck
        },
        settings: {
            pageSize: _PAGESIZE
        }
    }

})(jQuery);