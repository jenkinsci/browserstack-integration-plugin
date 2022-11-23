package com.browserstack.automate.ci.jenkins.observability;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.browserstack.automate.ci.common.BrowserStackEnvVars.BROWSERSTACK_RERUN;
import static com.browserstack.automate.ci.common.BrowserStackEnvVars.BROWSERSTACK_RERUN_TESTS;

@Extension
public class ObservabilityEnvironmentContributor extends EnvironmentContributor {

    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {

        ObservabilityCause cause = (ObservabilityCause) r.getCause(ObservabilityCause.class);
        if (cause != null) {
            envs.put(BROWSERSTACK_RERUN_TESTS, cause.getTests());
            envs.put(BROWSERSTACK_RERUN, cause.getReRun());
        }

    }
}
