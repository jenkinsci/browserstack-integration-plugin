package com.browserstack.automate.ci.common.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    public static final String CAD_BASE_URL = "https://api-observability.browserstack.com//api";
    public static  final String BROWSERSTACK_CONFIG_DETAILS_ENDPOINT = "/v1/builds/buildReport";

    public static final String BROWSERSTACK_TEST_REPORT_DISPLAY_NAME = "BrowserStack Build Test Reports";
    public static final String INTEGRATIONS_TOOL_KEY = "jenkins";
    public static  final String REPORT_FORMAT = "richHtml";

    // Product
    public static final String AUTOMATE = "automate";
    public static final String APP_AUTOMATE = "app-automate";

    //GRR REGION vs API URL mapping for Automate
    public static Map<String, String> GRR_AUTO_REGION_VS_APIURL = new HashMap<String, String>();
    public static Map<String, String> GRR_APPAUTO_REGION_VS_APIURL = new HashMap<String, String>();
    static {
        GRR_AUTO_REGION_VS_APIURL.put("eu","https://api-eu-only.browserstack.com/automate");
        GRR_AUTO_REGION_VS_APIURL.put("us","https://api-us-only.browserstack.com/automate");
        GRR_APPAUTO_REGION_VS_APIURL.put("eu","https://api-eu-only.browserstack.com/app-automate");
        GRR_APPAUTO_REGION_VS_APIURL.put("us","https://api-us-only.browserstack.com/app-automate");
    }

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

    public static final class QualityDashboardAPI {
        public static final String URL_BASE = "https://quality-engineering-insights.browserstack.com/api/v1/jenkins";

        public static final String LOG_MESSAGE = URL_BASE + "/log-message";
        public static final String IS_INIT_SETUP_REQUIRED = URL_BASE + "/init-setup-required";

        public static final String HISTORY_FOR_DAYS = URL_BASE + "/history-for-days";

        public static final String SAVE_PIPELINES = URL_BASE + "/save-pipelines";

        public static final String SAVE_PIPELINE_RESULTS = URL_BASE + "/save-pipeline-results";

        public static final String ITEM_CRUD = URL_BASE + "/item";
        public static final String IS_QD_ENABLED = URL_BASE + "/qd-enabled";
        public static final String IS_PIPELINE_ENABLED = URL_BASE + "/pipeline-enabled";
        public static final String GET_RESULT_DIRECTORY = URL_BASE + "/get-result-directory";

        public static final String UPLOAD_RESULT_ZIP = URL_BASE + "/upload-result";
        public static final String STORE_PIPELINE_RESULTS = URL_BASE + "/save-results";

        public static final String PROJECTS_PAGE_SIZE = URL_BASE + "/projects-page-size";
        public static final String RESULTS_PAGE_SIZE = URL_BASE + "/results-page-size";
    }
}
