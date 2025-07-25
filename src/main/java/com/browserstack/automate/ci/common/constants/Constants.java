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

    public static final String CAD_BASE_URL = "https://api-observability.browserstack.com/ext";
    public static  final String BROWSERSTACK_CONFIG_DETAILS_ENDPOINT = "/v1/builds/buildReport";

    public static final String INTEGRATIONS_TOOL_KEY = "jenkins";

    public static final String BROWSERSTACK_TEST_REPORT_URL = "testReportBrowserStack";
    public static final String BROWSERSTACK_CAD_REPORT_DISPLAY_NAME = "BrowserStack Test Report and Insights";
    public static final String BROWSERSTACK_REPORT_FILENAME = "browserstack-report";
    public static final String BROWSERSTACK_REPORT_FOLDER = "browserstack-artifacts";

    public static final String BROWSERSTACK_REPORT_AUT_PIPELINE_FUNCTION = "browserStackReportAut";

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
        public static final String QEI_DEFAULT_URL = "https://quality-engineering-insights.browserstack.com";
        public static String host = QEI_DEFAULT_URL;
        // Cache configuration
        public static final long CACHE_DURATION_MS = 60 * 60 * 1000L; // 1 hour in milliseconds

        public static String getHost() {
            return host;
        }

        public static void setHost(String newHost) {
            host = newHost;
        }

        public static final String getURLBase() {
            return getHost() + "/api/v1/jenkins";
        }
    
        public static final String getLogMessageEndpoint() {
            return getURLBase() + "/log-message";
        }
    
        public static final String getIsInitSetupRequiredEndpoint() {
            return getURLBase() + "/init-setup-required";
        }
    
        public static final String getHistoryForDaysEndpoint() {
            return getURLBase() + "/history-for-days";
        }
    
        public static final String getSavePipelinesEndpoint() {
            return getURLBase() + "/save-pipelines";
        }
    
        public static final String getSavePipelineResultsEndpoint() {
            return getURLBase() + "/save-pipeline-results";
        }
    
        public static final String getItemCrudEndpoint() {
            return getURLBase() + "/item";
        }
    
        public static final String getIsQdEnabledEndpoint() {
            return getURLBase() + "/qd-enabled";
        }
    
        public static final String getIsPipelineEnabledEndpoint() {
            return getURLBase() + "/pipeline-enabled";
        }
    
        public static final String getResultDirectoryEndpoint() {
            return getURLBase() + "/get-result-directory";
        }
    
        public static final String getUploadResultZipEndpoint() {
            return getURLBase() + "/upload-result";
        }
    
        public static final String getStorePipelineResultsEndpoint() {
            return getURLBase() + "/save-results";
        }
    
        public static final String getProjectsPageSizeEndpoint() {
            return getURLBase() + "/projects-page-size";
        }
    
        public static final String getResultsPageSizeEndpoint() {
            return getURLBase() + "/results-page-size";
        }
    }
}
