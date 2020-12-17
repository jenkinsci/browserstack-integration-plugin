package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.BrowserStackEnvVars;
import com.browserstack.automate.ci.common.logger.PluginLogger;
import com.browserstack.automate.ci.common.uploader.AppUploaderHelper;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Optional;

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
        EnvVars parentContextEnvVars = context.get(EnvVars.class);

        String customProxy = parentContextEnvVars.get("https_proxy");
        customProxy = Optional.ofNullable(customProxy).orElse(parentContextEnvVars.get("http_proxy"));

        System.out.println("App upload custom proxy: " + customProxy);
        String appId = AppUploaderHelper.uploadApp(run, logger, this.appPath, customProxy);

        if (StringUtils.isEmpty(appId)) {
            PluginLogger.log(logger, "ERROR : App Id empty. ABORTING!!!");
            return null;
        }

        HashMap<String, String> overridesMap = new HashMap<String, String>();
        overridesMap.put(BrowserStackEnvVars.BROWSERSTACK_APP_ID, appId);

        body = getContext().newBodyInvoker()
                .withContext(EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
                        new ExpanderImpl(overridesMap)))
                .withCallback(BodyExecutionCallback.wrap(getContext())).start();
        PluginLogger.log(logger, "Environment variable BROWSERSTACK_APP_ID set with value : " + appId);
        return null;
    }
}
