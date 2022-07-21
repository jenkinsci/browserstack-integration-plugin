package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;

public class BrowserStackCypressReportPublisher extends Recorder implements SimpleBuildStep {

    @DataBoundConstructor
    public BrowserStackCypressReportPublisher() {
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();
        final PluginsTracker tracker = new PluginsTracker();
        final boolean pipelineStatus = false;

        log(logger, "Generating BrowserStack Cypress Test Report");

        final EnvVars parentEnvs = build.getEnvironment(listener);
        String browserStackBuildName = parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_BUILD_NAME);
        browserStackBuildName = Optional.ofNullable(browserStackBuildName).orElse(parentEnvs.get(Constants.JENKINS_BUILD_TAG));
        final String projectFolder = parentEnvs.get("BROWSERSTACK_CYPRESS_PROJECT_ROOT") != null ? parentEnvs.get("BROWSERSTACK_CYPRESS_PROJECT_ROOT") : parentEnvs.get("WORKSPACE");
        ProjectType product = ProjectType.AUTOMATE;

        tracker.reportGenerationInitialized(browserStackBuildName, product.name(), pipelineStatus);
        log(logger, "BrowserStack Cypress Project identified as : " + product.name());

        final BrowserStackCypressReportForBuild bstackReportAction =
                new BrowserStackCypressReportForBuild(build, product, browserStackBuildName, logger, tracker, pipelineStatus);
        final boolean reportResult = bstackReportAction.generateBrowserStackCypressReport(workspace, projectFolder);
        build.addAction(bstackReportAction);

        String reportStatus = reportResult ? Constants.ReportStatus.SUCCESS : Constants.ReportStatus.FAILED;
        log(logger, "BrowserStack Cypress Report Status: " + reportStatus);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
            return Constants.BROWSERSTACK_CYPRESS_REPORT_DISPLAY_NAME;
        }
    }
}
