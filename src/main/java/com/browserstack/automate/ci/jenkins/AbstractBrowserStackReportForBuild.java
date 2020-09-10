package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.constants.Constants;
import hudson.model.Action;
import hudson.model.Run;

public abstract class AbstractBrowserStackReportForBuild implements Action {
    private Run<?, ?> build;

    @Override
    public String getIconFileName() {
        return Constants.BROWSERSTACK_LOGO;
    }

    @Override
    public String getDisplayName() {
        return Constants.BROWSERSTACK_REPORT_DISPLAY_NAME;
    }

    @Override
    public String getUrlName() {
        return Constants.BROWSERSTACK_REPORT_URL;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public void setBuild(Run<?, ?> build) {
        this.build = build;
    }
}
