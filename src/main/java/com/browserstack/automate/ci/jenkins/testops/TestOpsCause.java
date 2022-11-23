package com.browserstack.automate.ci.jenkins.testops;

import hudson.model.Cause;
import net.sf.json.JSONObject;

import javax.annotation.Nonnull;

import static com.browserstack.automate.ci.common.BrowserStackEnvVars.BROWSERSTACK_RERUN;
import static com.browserstack.automate.ci.common.BrowserStackEnvVars.BROWSERSTACK_RERUN_TESTS;

/**
 * Indicates that a build was started because of one or more TestOps params.
 */
public class TestOpsCause extends Cause {
    private JSONObject params;
    private String tests;
    private String reRun;

    public TestOpsCause(@Nonnull JSONObject params) {
        this.tests = params.getString(BROWSERSTACK_RERUN_TESTS);
        this.reRun = params.getString(BROWSERSTACK_RERUN);
        this.params = params;
    }

    @Nonnull
    public JSONObject getParams() {
        return params;
    }

    public String getTests() {
        return tests;
    }

    public String getReRun() {
        return reRun;
    }

    /** {@inheritDoc} */
    @Override
    public String getShortDescription() {
        return "TestOps Params: " + params.toString();
    }
}
