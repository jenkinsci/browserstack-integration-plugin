package com.browserstack.automate.ci.jenkins;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.Jenkins;

public abstract class AbstractBrowserStackReportForBuild implements Action {
    private Run<?, ?> build;

    protected String displayName = "Test Report";
    protected String iconFileName = Jenkins.RESOURCE_PATH + "/plugin/browserstack-integration/images/logo.png";
    protected String reportUrl = "testReportBrowserStack";

    @Override
    public String getIconFileName() {
        return iconFileName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getUrlName() {
        return reportUrl;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public void setBuild(Run<?, ?> build) {
        this.build = build;
    }
}
