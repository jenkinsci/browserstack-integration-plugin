package com.browserstack.automate.ci.jenkins.pipeline;

import com.browserstack.automate.ci.common.BrowserStackBuildWrapperOperations;
import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildAction;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.browserstack.automate.ci.jenkins.local.BrowserStackLocalUtils;
import com.browserstack.automate.ci.jenkins.local.JenkinsBrowserStackLocal;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.browserstack.automate.ci.jenkins.observability.ObservabilityConfig;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Optional;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logError;

public class BrowserStackPipelineStepExecution extends StepExecution {
    private static final long serialVersionUID = -8810137779949881645L;
    private String credentialsId;
    private StepContext context;
    private BodyExecution body;
    private LocalConfig localConfig;
    private JenkinsBrowserStackLocal browserStackLocal;
    private ObservabilityConfig observabilityConfig;

    protected BrowserStackPipelineStepExecution(StepContext context, String credentialsId,
                                                LocalConfig localConfig, ObservabilityConfig observabilityConfig) {
        super(context);
        this.context = context;
        this.credentialsId = credentialsId;
        this.localConfig = localConfig;
        this.observabilityConfig = observabilityConfig;
    }

    @Override
    public boolean start() throws Exception {
        Run run = context.get(Run.class);
        TaskListener taskListener = context.get(TaskListener.class);
        Launcher launcher = context.get(Launcher.class);
        PrintStream logger = taskListener.getLogger();
        EnvVars parentContextEnvVars = context.get(EnvVars.class);

        String customProxy = parentContextEnvVars.get("https_proxy");
        customProxy = Optional.ofNullable(customProxy).orElse(parentContextEnvVars.get("http_proxy"));

        final PluginsTracker tracker = new PluginsTracker(customProxy);

        BrowserStackCredentials credentials =
                BrowserStackCredentials.getCredentials(run.getParent(), credentialsId);

        if (credentials == null) {
            logError(logger, "Credentials id is invalid. Aborting!!!");
            tracker.sendError("No Credentials Available", true, "PipelineExecution");
            context.onFailure(new Exception("No Credentials Available"));
            return true;
        }

        if (credentials.hasUsername() && credentials.hasAccesskey()) {
            tracker.setCredentials(credentials.getUsername(), credentials.getDecryptedAccesskey());
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
                        launcher, getContext().get(EnvVars.class));
            } catch (Exception e) {
                taskListener.fatalError(e.getMessage());
                tracker.sendError(e.getMessage().substring(0, Math.min(100, e.getMessage().length())),
                        true, "LocalInitialization");
                throw new IOException(e.getCause());
            }
        }

        BrowserStackBuildWrapperOperations buildWrapperOperations =
                new BrowserStackBuildWrapperOperations(credentials, false, taskListener.getLogger(),
                        localConfig, browserStackLocal, observabilityConfig);

        EnvVars overrides = run.getEnvironment(taskListener);
        HashMap<String, String> overridesMap = new HashMap<String, String>();
        overridesMap.put(Constants.JENKINS_BUILD_TAG, overrides.get(Constants.JENKINS_BUILD_TAG));
        buildWrapperOperations.buildEnvVars(overridesMap);

        body = getContext()
                .newBodyInvoker().withContext(credentials).withContext(EnvironmentExpander
                        .merge(getContext().get(EnvironmentExpander.class), new ExpanderImpl(overridesMap)))
                .withCallback(new Callback(browserStackLocal)).start();

        tracker.pluginInitialized(overrides.get(Constants.JENKINS_BUILD_TAG),
                (this.localConfig != null), true);
        return false;
    }

    public void startBrowserStackLocal(String buildTag, PrintStream logger, String accessKey,
                                       Launcher launcher, EnvVars envVars) throws Exception {
        browserStackLocal = new JenkinsBrowserStackLocal(accessKey, localConfig, buildTag, envVars, logger);
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
