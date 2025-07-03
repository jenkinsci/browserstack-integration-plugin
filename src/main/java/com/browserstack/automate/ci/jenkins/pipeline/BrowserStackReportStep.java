package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Set;

public class BrowserStackReportStep extends Step {
    public ProjectType project;
    public String product;

    @DataBoundConstructor
    public BrowserStackReportStep(String product) {
        if (Constants.APP_AUTOMATE.equalsIgnoreCase(product)) {
            this.project = ProjectType.APP_AUTOMATE;
            this.product = Constants.APP_AUTOMATE;
        } else {
            this.project = ProjectType.AUTOMATE;
            this.product = Constants.AUTOMATE;
        }
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new BrowserStackReportStepExecution(stepContext, project);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return Constants.BROWSERSTACK_REPORT_AUT_PIPELINE_FUNCTION ; // deprecated Constants.BROWSERSTACK_REPORT_PIPELINE_FUNCTION;
        }

        @Override
        public String getDisplayName() {
            return Constants.BROWSERSTACK_REPORT_DISPLAY_NAME;
        }

        public ListBoxModel doFillProductItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Automate", Constants.AUTOMATE);
            items.add("App Automate", Constants.APP_AUTOMATE);
            return items;
        }

        public FormValidation doCheckProduct(@QueryParameter String product) {
            return FormValidation.ok();
        }
    }
}
