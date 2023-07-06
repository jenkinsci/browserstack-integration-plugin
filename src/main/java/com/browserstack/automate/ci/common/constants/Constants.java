package com.browserstack.automate.ci.common.constants;

import jenkins.model.Jenkins;

public class Constants {
    public static final String BROWSERSTACK_REPORT_DISPLAY_NAME = "BrowserStack Test Report";
    public static final String BROWSERSTACK_CYPRESS_REPORT_DISPLAY_NAME = "BrowserStack Cypress Test Report";
    public static final String BROWSERSTACK_LOGO = String.format("%s/plugin/browserstack-integration/images/logo.png", Jenkins.RESOURCE_PATH);
    public static final String BROWSERSTACK_REPORT_URL = "testReportBrowserStack";
    public static final String BROWSERSTACK_CYPRESS_REPORT_URL = "testReportBrowserStackCypress";
    public static final String BROWSERSTACK_REPORT_PIPELINE_FUNCTION = "browserStackReportPublisher";
    public static final String BROWSERSTACK_REPORT_PATH_PATTERN = "**/browserstack-artifacts/*";
    public static final String JENKINS_CI_PLUGIN = "JenkinsCiPlugin";

    // Product
    public static final String AUTOMATE = "automate";
    public static final String APP_AUTOMATE = "app-automate";

    public static final String JENKINS_BUILD_TAG = "BUILD_TAG";

    // Session related info
    public static final class SessionInfo {
        public static final String NAME = "name";
        public static final String BROWSERSTACK_BUILD_NAME = "buildName";
        public static final String BROWSERSTACK_BUILD_URL = "buildUrl";
        public static final String BROWSERSTACK_BUILD_DURATION = "buildDuration";
        public static final String BROWSER = "browser";
        public static final String OS = "os";
        public static final String STATUS = "status";
        public static final String USER_MARKED = "userMarked";
        public static final String DURATION = "duration";
        public static final String CREATED_AT = "createdAt";
        public static final String CREATED_AT_READABLE = "createdAtReadable";
        public static final String URL = "url";
    }

    // Report
    public static final class ReportStatus {
        public static final String SUCCESS = "Success";
        public static final String FAILED = "Failed";
    }

    // Session Status (not exhaustive)
    public static final class SessionStatus {
        public static final String RUNNING = "running";
        public static final String ERROR = "error";
        public static final String FAILED = "failed";
        public static final String UNMARKED = "unmarked";
        public static final String PASSED = "passed";
    }
}
