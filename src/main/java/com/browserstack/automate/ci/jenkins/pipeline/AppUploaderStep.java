package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.uploader.AppUploaderHelper;
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

public class AppUploaderStep extends Step {

    public String appPath;

    @DataBoundConstructor
    public AppUploaderStep(String appPath) throws Exception {
        this.appPath = appPath;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new AppUploadStepExecution(context, appPath);
    }

    @Extension
    public static final class StepDescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "browserstackAppUploader";
        }

        @Override
        public String getDisplayName() {
            return "BrowserStack App Uploader";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        public FormValidation doCheckAppPath(@QueryParameter String value) {
            return AppUploaderHelper.validateAppPath(value);
        }

    }

}
