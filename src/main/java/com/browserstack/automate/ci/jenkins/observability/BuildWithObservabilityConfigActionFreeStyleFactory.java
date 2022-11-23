package com.browserstack.automate.ci.jenkins.observability;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.TransientActionFactory;

/**
 * Attaches the {@link BuildWithObservabilityConfigAction} action to all {@link FreeStyleProject} instances.
 */
@Extension
public class BuildWithObservabilityConfigActionFreeStyleFactory extends TransientActionFactory<FreeStyleProject> {
    /** {@inheritDoc} */
    @Override
    public Class<FreeStyleProject> type() {
        return FreeStyleProject.class;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull FreeStyleProject job) {
        return Collections.singleton(new BuildWithObservabilityConfigAction(job));
    }
}
