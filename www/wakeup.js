var exec = require("cordova/exec");

/**
 * This is a global variable called wakeup exposed by cordova
 */
var Wakeup = function () {
};

Wakeup.prototype.enable = function (success, error, options) {
    function internalSuccess(pluginresult) {
        if (typeof pluginresult !== "undefined") {
            if (pluginresult.type == "wakeup") {
                var wakeupId = pluginresult.id;

                success(function () {
                    executionFinished(wakeupId);
                });
            }
        }
    }

    function executionFinished(id) {
        exec(internalSuccess, error, "WakeupPlugin", "executionFinished", {id: id});
    }

    exec(internalSuccess, error, "WakeupPlugin", "enable", options);
};

Wakeup.prototype.disable = function (success, error) {
    exec(success, error, "WakeupPlugin", "disable", {});
};

module.exports = new Wakeup();
