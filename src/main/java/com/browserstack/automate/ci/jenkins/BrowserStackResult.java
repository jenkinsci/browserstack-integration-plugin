package com.browserstack.automate.ci.jenkins;

import java.util.List;
import java.util.Map;

import hudson.model.Run;
import org.json.JSONObject;

import com.browserstack.automate.ci.common.constants.Constants;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

public class BrowserStackResult extends TestResult {
    // owner of this build
    protected Run<?, ?> build;
    private String buildName;
    private String browserStackBuildBrowserUrl;
    private final transient List<JSONObject> result;
    private final Map<String, String> resultAggregation;
    private final String errorConst = Constants.SessionStatus.ERROR;
    private final String failedConst = Constants.SessionStatus.FAILED;

    public BrowserStackResult(String buildName, String browserStackBuildBrowserUrl, List<JSONObject> resultList, Map<String, String> resultAggregation) {
        this.buildName = buildName;
        this.browserStackBuildBrowserUrl = browserStackBuildBrowserUrl;
        this.result = resultList;
        this.resultAggregation = resultAggregation;
    }
    
    @Override
    public TestResult findCorrespondingResult(String id) {
        if (id.equals(getId())) {
            return this;
        }
        return null;
    }

    @Override
    public String getDisplayName() {
        return Constants.BROWSERSTACK_REPORT_DISPLAY_NAME;
    }

    @Override
    public Run<?, ?> getRun() {
        return build;
    }

    @Override
    public TestObject getParent() {
        return null;
    }

    public List<JSONObject> getResult() {
        return result;
    }
    
    public Map<String, String> getResultAggregation() {
        return resultAggregation;
    }

    public String getErrorConst() {
        return errorConst;
    }

    public String getFailedConst() {
        return failedConst;
    }

    public void setRun(Run<?, ?> build) {
        this.build = build;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBrowserStackBuildBrowserUrl() {
        return browserStackBuildBrowserUrl;
    }
}
