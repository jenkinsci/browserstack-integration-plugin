package com.browserstack.automate.ci.jenkins.observability;

import hudson.Extension;
import hudson.model.Action;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.TransientActionFactory;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * Attaches the {@link BuildWithObservabilityConfigAction} action to all {@link WorkflowJob} instances.
 */
@Extension(optional = true)
public class BuildWithObservabilityConfigActionWorkflowFactory extends TransientActionFactory<WorkflowJob> {
    @Override
    public Class<WorkflowJob> type() {
        return WorkflowJob.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull WorkflowJob job) {
        return Collections.singleton(new BuildWithObservabilityConfigAction(job));
    }
}
