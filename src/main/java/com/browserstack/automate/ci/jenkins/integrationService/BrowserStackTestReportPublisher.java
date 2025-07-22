package com.browserstack.automate.ci.jenkins.integrationService;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildAction;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.FilePath;
import hudson.Launcher;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;

public class BrowserStackTestReportPublisher extends Recorder implements SimpleBuildStep {
  private static final Logger LOGGER = Logger.getLogger(BrowserStackTestReportPublisher.class.getName());
  private Map<String, String> customEnvVars;

  @DataBoundConstructor
  public BrowserStackTestReportPublisher(@CheckForNull String product) {
    this.customEnvVars =  new ConcurrentHashMap<>();
  }

  @Override
  public void perform(Run<?, ?> build, @NonNull FilePath workspace, @NonNull Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
    final PrintStream logger = listener.getLogger();
    log(logger, "Adding BrowserStack Report");

    EnvVars parentEnvs = build.getEnvironment(listener);
    parentEnvs.putAll(getCustomEnvVars());

    String browserStackBuildName = Optional.ofNullable(parentEnvs.get(BrowserStackEnvVars.BROWSERSTACK_BUILD_NAME))
            .orElse(parentEnvs.get(Constants.JENKINS_BUILD_TAG));

    BrowserStackBuildAction buildAction = build.getAction(BrowserStackBuildAction.class);
    if (buildAction == null || buildAction.getBrowserStackCredentials() == null) {
      logError(logger, "No BrowserStackBuildAction or credentials found");
      return;
    }

    BrowserStackCredentials credentials = buildAction.getBrowserStackCredentials();

    LOGGER.info("Adding BrowserStack Report Action");


    Date buildTimestamp = new Date(build.getStartTimeInMillis());

    // Format the timestamp (e.g., YYYY-MM-DD HH:MM:SS)
    long unixTimestamp = buildTimestamp.getTime() / 1000;

    String buildCreatedAt = String.valueOf(unixTimestamp);

    build.addAction(new BrowserStackTestReportAction(build, credentials, browserStackBuildName,buildCreatedAt));

  }


  public Map<String, String> getCustomEnvVars() {
    return customEnvVars;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Symbol(Constants.BROWSERSTACK_REPORT_PIPELINE_FUNCTION)
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // indicates that this builder can be used with all kinds of project types
      return true;
    }
    @Override
    public String getDisplayName() {
      return Constants.BROWSERSTACK_CAD_REPORT_DISPLAY_NAME;
    }

  }
}
