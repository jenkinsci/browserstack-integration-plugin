document.addEventListener("DOMContentLoaded", () => {
    const { sessionId } = document.querySelector(".browserstack-analytics-data-holder").dataset;

    var startTime = (new Date).valueOf();
    function onLoadIFrame() {
        testAction.iframeLoadTime((new Date).valueOf() - startTime);
    }

    var iframeElem = document.getElementById('browserstack-iframe-' + sessionId);
    if (iframeElem.addEventListener) {
        iframeElem.addEventListener('load', onLoadIFrame, true);
    } else if (iframeElem.attachEvent) {
        iframeElem.attachEvent('onload', onLoadIFrame);
    }
});
