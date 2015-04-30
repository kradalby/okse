/**
 * Created by Håkon Ødegård Løvdal on 30/04/15.
 */

var Subscribers = (function($) {

    /*
     Creates, fills and returns a <tr>-element. The <tr>-element is generated based on the subscribers
     list from the OKSE-RestAPI. It also adds all the buttons needed for deleting subscribers. It uses the id for
     this purpose. This function does not manipulate the DOM by checking if an element exists. It overwrites everything.
     */
    /*
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
    */

    /*
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
     */


    return {
        init: function() {

        },
        refresh: function(data) {

        }
    }

})(jQuery);

