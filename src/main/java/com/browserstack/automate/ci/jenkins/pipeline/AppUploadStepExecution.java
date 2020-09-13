package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.logger.PluginLogger;
import com.browserstack.automate.ci.common.uploader.AppUploaderHelper;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;

import java.io.PrintStream;
import java.util.HashMap;

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

    EnvVars overrides = run.getEnvironment(taskListener);
    overrides.put(BrowserStackEnvVars.BROWSERSTACK_APP_ID, appId);
    HashMap<String, String> overridesMap = new HashMap<>();
    overridesMap.putAll(overrides);

    body = getContext().newBodyInvoker()
        .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
            new ExpanderImpl(overridesMap)))
        .withCallback(BodyExecutionCallback.wrap(getContext())).start();
    PluginLogger.log(logger, "Environment variable BROWSERSTACK_APP_ID set with value : " + appId);
    return null;
  }
}
