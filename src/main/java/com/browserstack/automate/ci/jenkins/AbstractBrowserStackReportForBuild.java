package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.constants.Constants;
import hudson.model.Action;
import hudson.model.Run;

public abstract class AbstractBrowserStackReportForBuild implements Action {
    private Run<?, ?> build;

    protected final String displayName = Constants.BROWSERSTACK_REPORT_DISPLAY_NAME;
    protected final String iconFileName = Constants.BROWSERSTACK_LOGO;
    protected final String reportUrl = Constants.BROWSERSTACK_REPORT_URL;

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
