package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.enums.ProjectType;
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
import java.io.IOException;
import java.io.PrintStream;

public class BrowserStackReportPublisher extends Recorder implements SimpleBuildStep {

    @DataBoundConstructor
    public BrowserStackReportPublisher() {
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();
        logger.println("Generating BrowserStack Test Report");

        final EnvVars parentEnvs = build.getEnvironment(listener);
        final String browserStackBuildName = parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_BUILD_NAME);
        final String browserStackAppID = parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_APP_ID);

        ProjectType product = ProjectType.AUTOMATE;
        if (browserStackAppID != null && !browserStackAppID.isEmpty()) {
            product = ProjectType.APP_AUTOMATE;
        }

        final BrowserStackReportForBuild bstackReportAction =
                new BrowserStackReportForBuild(build, product, browserStackBuildName, logger);
        final boolean reportResult = bstackReportAction.generateBrowserStackReport();
        build.addAction(bstackReportAction);

        logger.println("BrowserStack Report Status: " + (reportResult ? "Generated" : "Failed"));
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckPublishText(@AncestorInPath Job<?, ?> project, @QueryParameter String value) throws IOException, ServletException {
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
