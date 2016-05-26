package com.browserstack.automate.ci.jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

public class BrowserConfig {
    private String os;
    private String browser;

    public BrowserConfig() {
    }

    @DataBoundConstructor
    public BrowserConfig(String os, String browser) {
        this.os = os;
        this.browser = browser;
    }

    public String getOs() {
        return this.os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getBrowser() {
        return this.browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }
}