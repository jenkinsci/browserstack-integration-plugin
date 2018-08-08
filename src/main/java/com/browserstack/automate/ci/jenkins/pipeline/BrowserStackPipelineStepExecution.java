package com.browserstack.automate.ci.jenkins.pipeline;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import com.browserstack.automate.ci.common.BrowserStackBuildWrapperOperations;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildAction;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.browserstack.automate.ci.jenkins.local.BrowserStackLocalUtils;
import com.browserstack.automate.ci.jenkins.local.JenkinsBrowserStackLocal;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class BrowserStackPipelineStepExecution extends SynchronousNonBlockingStepExecution<Void> {
  private static final long serialVersionUID = -8810137779949881645L;
  private String credentialsId;
  private StepContext context;
  private BodyExecution body;
  private LocalConfig localConfig;
  private JenkinsBrowserStackLocal browserStackLocal;

  protected BrowserStackPipelineStepExecution(StepContext context, String credentialsId,
      LocalConfig localConfig) {
    super(context);
    this.context = context;
    this.credentialsId = credentialsId;
    this.localConfig = localConfig;
  }

  @Override
  protected Void run() throws Exception {
    Run run = context.get(Run.class);
    TaskListener taskListener = context.get(TaskListener.class);
    Launcher launcher = context.get(Launcher.class);
    PrintStream logger = taskListener.getLogger();

    BrowserStackCredentials credentials =
        BrowserStackCredentials.getCredentials(run.getParent(), credentialsId);

    if (credentials == null) {
      logError(logger, "Credentials id is invalid. Aborting!!!");
      return null;
    }

    BrowserStackBuildAction action = run.getAction(BrowserStackBuildAction.class);
    if (action == null) {
      action = new BrowserStackBuildAction(credentials);
      run.addAction(action);
    }

    String accessKey = credentials.getDecryptedAccesskey();

    if (accessKey != null && this.localConfig != null) {
      try {
        startBrowserStackLocal(run.getFullDisplayName(), taskListener.getLogger(), accessKey,
            launcher);
      } catch (Exception e) {
        taskListener.fatalError(e.getMessage());
        throw new IOException(e.getCause());
      }
    }

    BrowserStackBuildWrapperOperations buildWrapperOperations =
        new BrowserStackBuildWrapperOperations(credentials, false, taskListener.getLogger(),
            localConfig, browserStackLocal);

    HashMap<String, String> overrides = new HashMap<String, String>();
    buildWrapperOperations.buildEnvVars(overrides);
    body = getContext()
        .newBodyInvoker().withContext(credentials).withContext(EnvironmentExpander
            .merge(getContext().get(EnvironmentExpander.class), new ExpanderImpl(overrides)))
        .withCallback(new Callback(browserStackLocal)).start();
    return null;
  }

  public void startBrowserStackLocal(String buildTag, PrintStream logger, String accessKey,
      Launcher launcher) throws Exception {
    browserStackLocal = new JenkinsBrowserStackLocal(accessKey, localConfig, buildTag);
    log(logger, "Local: Starting BrowserStack Local...");
    browserStackLocal.start(launcher);
    log(logger, "Local: Started");
  }

  @Override
  public void stop(Throwable cause) throws Exception {
    if (body != null) {
      body.cancel(cause);
    }
  }

  private static final class Callback extends BodyExecutionCallback.TailCall {
    private static final long serialVersionUID = -2490551580518219245L;

    private JenkinsBrowserStackLocal browserStackLocal;

    public Callback(JenkinsBrowserStackLocal browserStackLocal) {
      super();
      this.browserStackLocal = browserStackLocal;
    }

    @Override
    protected void finished(StepContext context) throws Exception {
      Launcher launcher = context.get(Launcher.class);
      TaskListener listener = context.get(TaskListener.class);
      PrintStream logger = listener.getLogger();
      BrowserStackLocalUtils.stopBrowserStackLocal(browserStackLocal, launcher, logger);
    }
  }
}
