package com.browserstack.automate.ci.common.analytics;

import com.brsanthu.googleanalytics.EventHit;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsRequest;
import com.brsanthu.googleanalytics.TimingHit;
import hudson.Plugin;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class Analytics {

    private static final String PLUGIN_NAME = "browserstack-integration";

    private static final String DEFAULT_CLIENT_ID = "unknown-client";

    private static final GoogleAnalytics ga = new GoogleAnalytics("UA-79358556-2");

    private static final Logger LOGGER = Logger.getLogger(Analytics.class.getName());

    private static String clientId;

    private static PluginWrapper pluginWrapper;

    private static VersionTracker versionTracker = new VersionTracker(Jenkins.getInstance().getRootDir());

    private static boolean isEnabled = true;

    static {
        trackInstall();
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void setEnabled(boolean isEnabled) {
        Analytics.isEnabled = isEnabled;
    }

    public static void trackInstall() {
        try {
            String version = getPluginWrapper().getVersion();

            if (versionTracker.init(version)) {
                postAsync(newEventHit("install", "install"));
            } else if (versionTracker.updateVersion(version)) {
                postAsync(newEventHit("install", "update"));
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to track install: " + e.getMessage());
        }
    }

    public static void trackBuildRun(boolean localEnabled, boolean localPathSet,
                                     boolean localOptionsSet, boolean isReportEnabled) {

        EventHit eventHit = newEventHit((localEnabled ? "with" : "without") + "Local", "buildRun");
        if (isReportEnabled) {
            eventHit.eventLabel("embedTrue");
        } else {
            eventHit.eventLabel("embedFalse");
        }

        if (localPathSet) {
            eventHit.customDimension(1, "withLocalPath");
        } else {
            eventHit.customDimension(2, "withoutLocalPath");
        }

        if (localOptionsSet) {
            eventHit.customDimension(3, "withLocalOptions");
        } else {
            eventHit.customDimension(4, "withoutLocalOptions");
        }

        postAsync(eventHit);
    }

    public static void trackIframeRequest() {
        postAsync(newEventHit("iframeRequested", "iframe"));
    }

    public static void trackIframeLoad(int loadTime) {
        postAsync(newTimingHit("iframeLoadTimeMs", "iframe", loadTime));
    }

    private static void postAsync(GoogleAnalyticsRequest request) {
        if (isEnabled) {
            ga.postAsync(request);
        }
    }

    private static EventHit newEventHit(String category, String action) {
        EventHit eventHit = new EventHit(category, action);
        attachGlobalProperties(eventHit);
        return eventHit;
    }

    private static TimingHit newTimingHit(String category, String variable, int time) {
        TimingHit timingHit = new TimingHit()
                .userTimingCategory(category)
                .userTimingVariableName(variable)
                .userTimingTime(time);
        attachGlobalProperties(timingHit);
        return timingHit;
    }

    private static boolean attachGlobalProperties(GoogleAnalyticsRequest gaRequest) {
        try {
            gaRequest.clientId(getClientId());
        } catch (IOException e) {
            LOGGER.warning("Using default clientId. Error: " + e.getMessage());
            gaRequest.clientId(DEFAULT_CLIENT_ID);
        }

        try {
            PluginWrapper pluginWrapper = getPluginWrapper();
            gaRequest.applicationName("jenkins-" + Jenkins.VERSION);
            gaRequest.applicationId(pluginWrapper.getShortName());
            gaRequest.applicationVersion(pluginWrapper.getVersion());
        } catch (IOException e) {
            LOGGER.warning("Failed to load plugin properties: " + e.getMessage());
            return false;
        }

        return true;
    }

    private static String getClientId() throws IOException {
        if (clientId != null) {
            return clientId;
        }

        clientId = versionTracker.getClientId();
        return clientId;
    }

    private static PluginWrapper getPluginWrapper() throws IOException {
        if (pluginWrapper != null) {
            return pluginWrapper;
        }

        Plugin plugin = Jenkins.getInstance().getPlugin(PLUGIN_NAME);
        if (plugin == null || plugin.getWrapper() == null) {
            throw new IOException("Plugin not found");
        }

        pluginWrapper = plugin.getWrapper();
        return pluginWrapper;
    }
}
