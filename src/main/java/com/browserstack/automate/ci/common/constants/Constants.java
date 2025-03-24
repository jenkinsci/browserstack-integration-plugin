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

    public static class QualityDashboardAPI {
        public static final String QD_DEFAULT_URL = "https://quality-engineering-insights.browserstack.com";
        public static String host = QD_DEFAULT_URL;
        public static String URL_BASE;

        public static String getHost() {
            return host;
        }

        public static void setHost(String newHost) {
            host = newHost;
        }

        public static String getURLBase() {
            return getHost() + "/api/v1/jenkins";
        }
    
        public static String getLogMessageEndpoint() {
            return getURLBase() + "/log-message";
        }
    
        public static String getIsInitSetupRequiredEndpoint() {
            return getURLBase() + "/init-setup-required";
        }
    
        public static String getHistoryForDaysEndpoint() {
            return getURLBase() + "/history-for-days";
        }
    
        public static String getSavePipelinesEndpoint() {
            return getURLBase() + "/save-pipelines";
        }
    
        public static String getSavePipelineResultsEndpoint() {
            return getURLBase() + "/save-pipeline-results";
        }
    
        public static String getItemCrudEndpoint() {
            return getURLBase() + "/item";
        }
    
        public static String getIsQdEnabledEndpoint() {
            return getURLBase() + "/qd-enabled";
        }
    
        public static String getIsPipelineEnabledEndpoint() {
            return getURLBase() + "/pipeline-enabled";
        }
    
        public static String getResultDirectoryEndpoint() {
            return getURLBase() + "/get-result-directory";
        }
    
        public static String getUploadResultZipEndpoint() {
            return getURLBase() + "/upload-result";
        }
    
        public static String getStorePipelineResultsEndpoint() {
            return getURLBase() + "/save-results";
        }
    
        public static String getProjectsPageSizeEndpoint() {
            return getURLBase() + "/projects-page-size";
        }
    
        public static String getResultsPageSizeEndpoint() {
            return getURLBase() + "/results-page-size";
        }
    }
}
