package com.browserstack.automate.ci.jenkins.pipeline;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.logger.PluginLogger;
import com.browserstack.automate.ci.common.uploader.AppUploader;
import com.browserstack.automate.ci.common.uploader.AppUploaderHelper;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildAction;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.browserstack.automate.exception.AppAutomateException;
import com.browserstack.automate.exception.InvalidFileExtensionException;
import hudson.model.Run;
import hudson.model.TaskListener;

public class AppUploadStepExecution extends SynchronousNonBlockingStepExecution<Void> {

  private StepContext context;
  private String appPath;
  private BodyExecution body;


  protected AppUploadStepExecution(StepContext context, String appPath) {
    super(context);
    this.context = context;
    this.appPath = appPath;
  }

  @Override
  protected Void run() throws Exception {
    Run run = context.get(Run.class);
    TaskListener taskListener = context.get(TaskListener.class);
    PrintStream logger = taskListener.getLogger();

    String appId = AppUploaderHelper.uploadApp(run, logger, this.appPath);

    if (StringUtils.isEmpty(appId)) {
      PluginLogger.log(logger, "ERROR : App Id empty. ABORTING!!!");
      return null;
    }

    HashMap<String, String> overrides = new HashMap<String, String>();
    overrides.put(BrowserStackEnvVars.BROWSERSTACK_APP_ID, appId);
    body = getContext().newBodyInvoker()
        .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
            new ExpanderImpl(overrides)))
        .withCallback(BodyExecutionCallback.wrap(getContext())).start();
    return null;
  }
}
