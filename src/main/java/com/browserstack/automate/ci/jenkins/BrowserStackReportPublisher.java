package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import com.browserstack.automate.ci.common.enums.ProjectType;

import java.io.IOException;
import java.io.PrintStream;

public class BrowserStackReportPublisher extends Recorder implements SimpleBuildStep {
    private PrintStream logger;

    @DataBoundConstructor
    public BrowserStackReportPublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        this.logger = listener.getLogger();
        this.logger.println("Generating BrowserStack Test Report");

        EnvVars parentEnvs = build.getEnvironment(listener);
        String browserStackBuildName = parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_BUILD_NAME);
        String browserStackAppID = parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_APP_ID);

        ProjectType product = ProjectType.AUTOMATE;
        if (browserStackAppID != null && !browserStackAppID.isEmpty()) {
            product = ProjectType.APP_AUTOMATE;
        }

        AbstractBrowserStackReportForBuild bstackReportAction =
            new BrowserStackReportForBuild(product, browserStackBuildName, this.logger);
        bstackReportAction.setBuild(build);
        Boolean reportResult = ((BrowserStackReportForBuild) bstackReportAction).generateBrowserStackReport();
        build.addAction(bstackReportAction);

        this.logger.println("BrowserStack Report Status: " + (reportResult ? "Generated" : "Failed"));
        return;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckPublishText(@AncestorInPath Job<?,?> project, @QueryParameter String value) throws IOException, ServletException {
            return FormValidation.ok();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "BrowserStack Report Publisher";
        }
    }
}
