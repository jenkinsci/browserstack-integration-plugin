package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.constants.Constants;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
public abstract class AbstractBrowserStackReportForBuild extends AbstractTestResultAction {
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

    public String getHealingAgentUrl() {
        return Constants.BROWSERSTACK_AI_HEALING_URL;
    }

    public String getTfaAgentUrl() {
        return Constants.BROWSERSTACK_AI_TFA_URL;
    }

    public String getSmartSelectionAgentUrl() {
        return Constants.BROWSERSTACK_AI_SMART_SELECTION_URL;
    }

    public String getNlAutomationAgentUrl() {
        return Constants.BROWSERSTACK_AI_NL_AUTOMATION_URL;
    }

    public void setBuild(Run<?, ?> build) {
        this.build = build;
    }
}
