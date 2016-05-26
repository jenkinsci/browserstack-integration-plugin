package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.AutomateTestCase;
import hudson.model.AbstractBuild;
import hudson.model.InvisibleAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class AutomateBuildAction extends InvisibleAction {

    private final AbstractBuild<?, ?> build;
    private final List<AutomateTestCase> testCaseList;

    @DataBoundConstructor
    public AutomateBuildAction(AbstractBuild<?, ?> build, List<AutomateTestCase> testCaseList) {
        this.build = build;
        this.testCaseList = testCaseList;
    }

    public List<AutomateTestCase> getTestCaseList() {
        return testCaseList;
    }
}
