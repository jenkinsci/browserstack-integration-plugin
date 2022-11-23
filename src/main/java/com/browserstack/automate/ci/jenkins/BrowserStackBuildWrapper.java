package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.BrowserStackBuildWrapperOperations;
import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.ci.common.constants.Constants;
import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import com.browserstack.automate.ci.jenkins.local.BrowserStackLocalUtils;
import com.browserstack.automate.ci.jenkins.local.JenkinsBrowserStackLocal;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.browserstack.automate.ci.jenkins.observability.ObservabilityConfig;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.BuildListener;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;


public class BrowserStackBuildWrapper extends BuildWrapper {

    private static final char CHAR_MASK = '*';

    private LocalConfig localConfig;
    private ObservabilityConfig observabilityConfig;

    private String credentialsId;
    private String username;
    private String accesskey;

    @DataBoundConstructor
    public BrowserStackBuildWrapper(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setLocalConfig(LocalConfig localConfig) {
        this.localConfig = localConfig;
    }

    @DataBoundSetter
    public void setObservabilityConfig(ObservabilityConfig observabilityConfig) { this.observabilityConfig = observabilityConfig; }

    static BuildWrapperItem<BrowserStackBuildWrapper> findBrowserStackBuildWrapper(
            final Job<?, ?> job) {
        BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem =
                findItemWithBuildWrapper(job, BrowserStackBuildWrapper.class);
        return (wrapperItem != null) ? wrapperItem : null;
    }

    private static <T extends BuildWrapper> BuildWrapperItem<T> findItemWithBuildWrapper(
            final AbstractItem buildItem, Class<T> buildWrapperClass) {
        if (buildItem == null) {
            return null;
        }

        if (buildItem instanceof BuildableItemWithBuildWrappers) {
            BuildableItemWithBuildWrappers buildWrapper = (BuildableItemWithBuildWrappers) buildItem;
            DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappersList =
                    buildWrapper.getBuildWrappersList();

            if (buildWrappersList != null && !buildWrappersList.isEmpty()) {
                return new BuildWrapperItem<T>(buildWrappersList.get(buildWrapperClass), buildItem);
            }
        }

        if (buildItem.getParent() instanceof AbstractItem) {
            return findItemWithBuildWrapper((AbstractItem) buildItem.getParent(), buildWrapperClass);
        }

        return null;
    }

    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher,
                             final BuildListener listener) throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();
        final PluginsTracker tracker = new PluginsTracker();

        final BrowserStackCredentials credentials =
                BrowserStackCredentials.getCredentials(build.getProject(), credentialsId);

        BrowserStackBuildAction action = build.getAction(BrowserStackBuildAction.class);
        if (action == null) {
            action = new BrowserStackBuildAction(credentials);
            build.addAction(action);
        }

        if (credentials != null) {
            this.username = credentials.getUsername();
            this.accesskey = credentials.getDecryptedAccesskey();
            tracker.setCredentials(this.username, this.accesskey);
        } else {
            tracker.sendError("No Credentials Available", false, "PluginInitialization");
        }

        AutomateBuildEnvironment buildEnv = new AutomateBuildEnvironment(credentials, launcher, logger);
        if (accesskey != null && this.localConfig != null) {
            try {
                buildEnv.startBrowserStackLocal(build.getFullDisplayName(), build.getEnvironment(listener));
            } catch (Exception e) {
                listener.fatalError(e.getMessage());
                tracker.sendError(e.getMessage().substring(0, Math.min(100, e.getMessage().length())),
                        false, "LocalInitialization");
                throw new IOException(e.getCause());
            }
        }

        recordBuildStats();
        EnvVars envs = build.getEnvironment(listener);
        tracker.pluginInitialized(envs.get(Constants.JENKINS_BUILD_TAG), (this.localConfig != null), false);

        return buildEnv;
    }

    @Override
    public BrowserStackBuildWrapperDescriptor getDescriptor() {
        return (BrowserStackBuildWrapperDescriptor) super.getDescriptor();
    }

    public LocalConfig getLocalConfig() {
        return this.localConfig;
    }

    public ObservabilityConfig getObservabilityConfig() {
        return this.observabilityConfig;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    private void recordBuildStats() {
        boolean localEnabled = (localConfig != null);
        boolean localPathSet = localEnabled && StringUtils.isNotBlank(localConfig.getLocalPath());
        boolean localOptionsSet = localEnabled && StringUtils.isNotBlank(localConfig.getLocalOptions());
        Analytics.trackBuildRun(localEnabled, localPathSet, localOptionsSet);
    }

    static class BuildWrapperItem<T> {
        final T buildWrapper;
        final AbstractItem buildItem;

        BuildWrapperItem(T buildWrapper, AbstractItem buildItem) {
            this.buildWrapper = buildWrapper;
            this.buildItem = buildItem;
        }
    }

    private class AutomateBuildEnvironment extends BuildWrapper.Environment {
        private static final String ENV_JENKINS_BUILD_TAG = "BUILD_TAG";

        private final BrowserStackCredentials credentials;
        private final Launcher launcher;
        private final PrintStream logger;
        private JenkinsBrowserStackLocal browserstackLocal;
        private boolean isTearDownPhase;

        AutomateBuildEnvironment(BrowserStackCredentials credentials, Launcher launcher,
                                 PrintStream logger) {
            this.credentials = credentials;
            this.launcher = launcher;
            this.logger = logger;
        }

        public void buildEnvVars(Map<String, String> env) {
            BrowserStackBuildWrapperOperations buildWrapperOperations =
                    new BrowserStackBuildWrapperOperations(credentials, isTearDownPhase, logger, localConfig,
                            browserstackLocal, observabilityConfig);
            buildWrapperOperations.buildEnvVars(env);
            super.buildEnvVars(env);
        }

        public void startBrowserStackLocal(String buildTag, EnvVars envVars) throws Exception {
            browserstackLocal = new JenkinsBrowserStackLocal(accesskey, localConfig, buildTag, envVars, logger);
            log(logger, "Local: Starting BrowserStack Local...");
            browserstackLocal.start(launcher);
            log(logger, "Local: Started");
        }

        public boolean tearDown(AbstractBuild build, BuildListener listener)
                throws IOException, InterruptedException {
            isTearDownPhase = true;
            try {
                BrowserStackLocalUtils.stopBrowserStackLocal(browserstackLocal, launcher, logger);
            } catch (Exception e) {
                throw new IOException(e.getCause());
            }
            return true;
        }

    }
}
