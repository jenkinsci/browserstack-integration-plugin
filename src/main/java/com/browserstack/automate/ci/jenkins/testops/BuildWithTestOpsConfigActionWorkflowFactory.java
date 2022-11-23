package com.browserstack.automate.ci.jenkins.testops;

import hudson.Extension;
import hudson.model.Action;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import jenkins.model.TransientActionFactory;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * Attaches the {@link BuildWithTestOpsConfigAction} action to all {@link WorkflowJob} instances.
 */
@Extension(optional = true)
public class BuildWithTestOpsConfigActionWorkflowFactory extends TransientActionFactory<WorkflowJob> {
    @Override
    public Class<WorkflowJob> type() {
        return WorkflowJob.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull WorkflowJob job) {
        return Collections.singleton(new BuildWithTestOpsConfigAction(job));
    }
}
