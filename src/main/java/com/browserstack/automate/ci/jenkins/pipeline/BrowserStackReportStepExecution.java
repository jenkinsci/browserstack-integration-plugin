package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.common.enums.ProjectType;
import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import com.browserstack.automate.ci.jenkins.BrowserStackReportForBuild;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.PrintStream;
import java.util.Optional;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;

public class BrowserStackReportStepExecution extends SynchronousNonBlockingStepExecution {

    private final ProjectType product;

    public BrowserStackReportStepExecution(StepContext context, final ProjectType product) {
        super(context);
        this.product = product;
    }

    @Override
    protected Void run() throws Exception {
        Run<?, ?> run = getContext().get(Run.class);
        TaskListener taskListener = getContext().get(TaskListener.class);
        final PrintStream logger = taskListener.getLogger();
        final EnvVars parentContextEnvVars = getContext().get(EnvVars.class);
        final EnvVars parentEnvs = run.getEnvironment(taskListener);

        String customProxy = parentContextEnvVars.get("https_proxy");
        customProxy = Optional.ofNullable(customProxy).orElse(parentContextEnvVars.get("http_proxy"));

        final PluginsTracker tracker = new PluginsTracker(customProxy);

        System.out.println("\ncustomProxy in report generation: " + customProxy);

        log(logger, "Generating BrowserStack Test Report via Pipeline for : " + product.name());

        String browserStackBuildName = parentContextEnvVars.get(BrowserStackEnvVars.BROWSERSTACK_BUILD_NAME);
        browserStackBuildName = Optional.ofNullable(browserStackBuildName).orElse(parentEnvs.get(Constants.JENKINS_BUILD_TAG));

        tracker.reportGenerationInitialized(browserStackBuildName, product.name(), true);

        final BrowserStackReportForBuild bstackReportAction =
                new BrowserStackReportForBuild(run, product, browserStackBuildName, logger, tracker, true, customProxy);
        final boolean reportResult = bstackReportAction.generateBrowserStackReport();
        run.addAction(bstackReportAction);

        String reportStatus = reportResult ? Constants.ReportStatus.SUCCESS : Constants.ReportStatus.FAILED;
        log(logger, "BrowserStack Report Status via Pipeline: " + reportStatus);

        tracker.reportGenerationCompleted(reportStatus, product.name(), true,
                browserStackBuildName, bstackReportAction.getBrowserStackBuildID());
        return null;
    }
}
