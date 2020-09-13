package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.enums.ProjectType;
import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Set;

public class BrowserStackReportStep extends Step {
    public final ProjectType product;

    @DataBoundConstructor
    public BrowserStackReportStep(String product) {
        if (product.toLowerCase() == "app-automate") {
            this.product = ProjectType.APP_AUTOMATE;
        } else {
            this.product = ProjectType.AUTOMATE;
        }
    }

    @DataBoundConstructor
    public BrowserStackReportStep() {
        this.product = ProjectType.AUTOMATE;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new BrowserStackReportStepExecution(stepContext, product);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "browserStackReportPublisher";
        }

        @Override
        public String getDisplayName() {
            return "BrowserStack Pipeline Report";
        }

        public FormValidation doCheckProduct(@QueryParameter String product) {
            return FormValidation.ok();
        }
    }
}
