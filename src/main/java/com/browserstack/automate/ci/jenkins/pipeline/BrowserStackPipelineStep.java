package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.BrowserStackBuildWrapperOperations;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.browserstack.automate.ci.jenkins.observability.ObservabilityConfig;
import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.Set;

public class BrowserStackPipelineStep extends Step {

    public String credentialsId;
    public LocalConfig localConfig;
    public ObservabilityConfig observabilityConfig;

    @DataBoundConstructor
    public BrowserStackPipelineStep(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setLocalConfig(LocalConfig localConfig) {
        this.localConfig = localConfig;
    }

    @DataBoundSetter
    public void setObservabilityConfig(ObservabilityConfig observabilityConfig) {
        this.observabilityConfig = observabilityConfig;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new BrowserStackPipelineStepExecution(context, credentialsId, localConfig, observabilityConfig);
    }

    @Extension
    public static final class StepDescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class, Launcher.class);
        }

        @Override
        public String getFunctionName() {
            return "browserstack";
        }

        @Override
        public String getDisplayName() {
            return "BrowserStack";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item context) {
            return BrowserStackBuildWrapperOperations.doFillCredentialsIdItems(context);
        }

        public FormValidation doCheckLocalPath(@AncestorInPath final AbstractProject project,
                                               @QueryParameter final String localPath) {
            return BrowserStackBuildWrapperOperations.doCheckLocalPath(project, localPath);
        }
    }


}
