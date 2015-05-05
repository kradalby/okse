/**
 * Created by hakloev on 01/05/15.
 */
(function ($) {

    var pluginName = 'okseDebug';

    $.okseDebug = function(options) {

        var defaults = {
            debugFlag: true
            },
            plugin = this,
            options = options || {};

        plugin.init = function() {
            var settings = $.extend({}, defaults, options);
            $.data(document, pluginName, settings);
        }

        plugin.init();
    }

    $.okseDebug.logPrint = function(text) {
        if ($.data(document, pluginName).debugFlag) {
            console.log(text)
        }
    }

    $.okseDebug.errorPrint = function(text) {
        if ($.data(document, pluginName).debugFlag) {
            console.error(text)
        }
    }

})(jQuery);