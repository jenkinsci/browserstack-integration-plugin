package com.browserstack.automate.ci.jenkins;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TextForBuild extends AbstractTextForBuild {
    /*
     * Holds info about the Selenium Test
     */
    private String buildName;
    private String buildNumber;
    private String testType;

    TextForBuild(final String testType) {
        super();
        this.testType = testType;
        setIconFileName(Jenkins.RESOURCE_PATH + "/plugin/browserstack-integration/images/logo.png");
        setDisplayName("Test Report");
        setReportUrl(displayName);
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

}
