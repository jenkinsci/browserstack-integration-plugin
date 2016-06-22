package com.browserstack.automate.ci.common.logger;

import java.io.PrintStream;

public class PluginLogger {
    private static final String PROPERTY_DEBUG = "browserstack.automate.debug";
    private static String TAG = "[BrowserStack]";

    public static boolean isDebugEnabled() {
        return System.getProperty(PROPERTY_DEBUG, "false").equals("true");
    }

    public static void log(PrintStream printStream, String message) {
        printStream.println(TAG + " " + message);
    }

    public static void logDebug(PrintStream printStream, String message) {
        if (isDebugEnabled()) printStream.println(TAG + ": " + message);
    }

    public static void setTag(String tag) {
        TAG = tag;
    }
}
