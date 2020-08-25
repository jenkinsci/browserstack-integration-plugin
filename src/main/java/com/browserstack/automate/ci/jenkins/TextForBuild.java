package com.browserstack.automate.ci.jenkins;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TextForBuild extends AbstractTextForBuild {
    /*
     * Holds info about the Selenium Test
     */
    private String operatingSystem;
    private String browserName;
    private String browserVersion;
    private String resolution;
    private String buildName;
    private String buildNumber;
    private String testType;
    private String iframeLink;

    TextForBuild(final String testType, final String operatingSystem, final String browserName,
                               final String browserVersion, final String resolution) {
        super();
        this.testType = testType;
        this.operatingSystem = operatingSystem;
        this.browserName = browserName;
        this.browserVersion = browserVersion;
        this.resolution = resolution;
        setIconFileName("some_icon");
        setDisplayName("LT(" + operatingSystem + " " + browserName + "-" + browserVersion + " " + resolution + ")");
        setTestUrl(displayName);
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
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

    public String getIframeLink() {
        return iframeLink;
    }

    public void setIframeLink(String iframeLink) {
        this.iframeLink = iframeLink;
    }

}
