package com.browserstack.automate.ci.jenkins;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;


import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.ci.jenkins.local.JenkinsBrowserStackLocal;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.BuildListener;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;


public class BrowserStackBuildWrapper extends BuildWrapper {

    private static final char CHAR_MASK = '*';

    private final LocalConfig localConfig;

    private String credentialsId;
    private String username;
    private String accesskey;

    @DataBoundConstructor
    public BrowserStackBuildWrapper(String credentialsId, LocalConfig localConfig) {
        this.credentialsId = credentialsId;
        this.localConfig = localConfig;
    }

    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher,
                             final BuildListener listener) throws IOException, InterruptedException {
        final PrintStream logger = listener.getLogger();

        final BrowserStackCredentials credentials = BrowserStackCredentials.getCredentials(build.getProject(), credentialsId);
        if (credentials != null) {
            this.username = credentials.getUsername();
            this.accesskey = credentials.getDecryptedAccesskey();
        }

        AutomateBuildEnvironment buildEnv = new AutomateBuildEnvironment(credentials, launcher, logger);
        if (accesskey != null && this.localConfig != null) {
            try {
                buildEnv.startBrowserStackLocal(build.getFullDisplayName());
            } catch (Exception e) {
                listener.fatalError(e.getMessage());
                throw new IOException(e.getCause());
            }
        }

        recordBuildStats();
        return buildEnv;
    }

    @Override
    public BrowserStackBuildWrapperDescriptor getDescriptor() {
        return (BrowserStackBuildWrapperDescriptor) super.getDescriptor();
    }


    public LocalConfig getLocalConfig() {
        return this.localConfig;
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


    private interface EnvVars {
        String BROWSERSTACK_USER = "BROWSERSTACK_USER";
        String BROWSERSTACK_USERNAME = "BROWSERSTACK_USERNAME";
        String BROWSERSTACK_ACCESSKEY = "BROWSERSTACK_ACCESSKEY";
        String BROWSERSTACK_ACCESS_KEY = "BROWSERSTACK_ACCESS_KEY";
        String BROWSERSTACK_LOCAL = "BROWSERSTACK_LOCAL";
        String BROWSERSTACK_LOCAL_IDENTIFIER = "BROWSERSTACK_LOCAL_IDENTIFIER";
        String BROWSERSTACK_BUILD = "BROWSERSTACK_BUILD";
    }

    private class AutomateBuildEnvironment extends BuildWrapper.Environment {
        private static final String ENV_JENKINS_BUILD_TAG = "BUILD_TAG";

        private final BrowserStackCredentials credentials;
        private final Launcher launcher;
        private final PrintStream logger;
        private JenkinsBrowserStackLocal browserstackLocal;
        private boolean isTearDownPhase;

        AutomateBuildEnvironment(BrowserStackCredentials credentials, Launcher launcher, PrintStream logger) {
            this.credentials = credentials;
            this.launcher = launcher;
            this.logger = logger;
        }

        public void buildEnvVars(Map<String, String> env) {
            if (credentials != null) {
                if (credentials.hasUsername()) {
                    String username = credentials.getUsername();

                    env.put(EnvVars.BROWSERSTACK_USER, username + "-jenkins");
                    env.put(EnvVars.BROWSERSTACK_USERNAME, username + "-jenkins");
                    logEnvVar(EnvVars.BROWSERSTACK_USERNAME, username);
                }

                if (credentials.hasAccesskey()) {
                    String accesskey = credentials.getDecryptedAccesskey();
                    env.put(EnvVars.BROWSERSTACK_ACCESSKEY, accesskey);
                    env.put(EnvVars.BROWSERSTACK_ACCESS_KEY, accesskey);
                    logEnvVar(EnvVars.BROWSERSTACK_ACCESS_KEY, maskString(accesskey));
                }
            }


            String buildTag = env.get(ENV_JENKINS_BUILD_TAG);
            if (buildTag != null) {
                env.put(EnvVars.BROWSERSTACK_BUILD, buildTag);
                logEnvVar(EnvVars.BROWSERSTACK_BUILD, buildTag);
            }

            String isLocalEnabled = BrowserStackBuildWrapper.this.localConfig != null ? "true" : "false";
            env.put(EnvVars.BROWSERSTACK_LOCAL, "" + isLocalEnabled);
            logEnvVar(EnvVars.BROWSERSTACK_LOCAL, isLocalEnabled);

            String localIdentifier = (browserstackLocal != null) ? browserstackLocal.getLocalIdentifier() : "";
            
            if (StringUtils.isNotBlank(localIdentifier)){
                env.put(EnvVars.BROWSERSTACK_LOCAL_IDENTIFIER, localIdentifier);
                logEnvVar(EnvVars.BROWSERSTACK_LOCAL_IDENTIFIER, localIdentifier);
            }

            super.buildEnvVars(env);
        }

        /**
         * Returns a string with only '*' of length equal to the length of the inputStr
         * @param strToMask
         * @return masked string
         */
        private String maskString(String strToMask) {
            char[] maskChars = new char[strToMask.length()];
            Arrays.fill(maskChars, CHAR_MASK);
            return new String(maskChars);
        }

        private void logEnvVar(String key, String value) {
            if (!isTearDownPhase) {
                log(logger, key + "=" + value);
            }
        }

        public void startBrowserStackLocal(String buildTag) throws Exception {
            browserstackLocal = new JenkinsBrowserStackLocal(accesskey, localConfig.getLocalOptions(), buildTag);
            log(logger, "Local: Starting BrowserStack Local...");
            browserstackLocal.start(launcher);
            log(logger, "Local: Started");
        }

        public void stopBrowserStackLocal() throws Exception {
            if (browserstackLocal != null) {
                log(logger, "Local: Stopping BrowserStack Local...");

                try {
                    browserstackLocal.stop(launcher);
                    log(logger, "Local: Stopped");
                } catch (Exception e) {
                    log(logger, "Local: ERROR: " + e.getMessage());
                }
            }
        }

        public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
            isTearDownPhase = true;

            try {
                stopBrowserStackLocal();
            } catch (Exception e) {
                throw new IOException(e.getCause());
            }

            return true;
        }

    }

    static BuildWrapperItem<BrowserStackBuildWrapper> findBrowserStackBuildWrapper(final Job<?, ?> job) {
        BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem = findItemWithBuildWrapper(job, BrowserStackBuildWrapper.class);
        return (wrapperItem != null) ? wrapperItem : null;
    }

    private static <T extends BuildWrapper> BuildWrapperItem<T> findItemWithBuildWrapper(final AbstractItem buildItem, Class<T> buildWrapperClass) {
        if (buildItem == null) {
            return null;
        }

        if (buildItem instanceof BuildableItemWithBuildWrappers) {
            BuildableItemWithBuildWrappers buildWrapper = (BuildableItemWithBuildWrappers) buildItem;
            DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappersList = buildWrapper.getBuildWrappersList();

            if (buildWrappersList != null && !buildWrappersList.isEmpty()) {
                return new BuildWrapperItem<T>(buildWrappersList.get(buildWrapperClass), buildItem);
            }
        }

        if (buildItem.getParent() instanceof AbstractItem) {
            return findItemWithBuildWrapper((AbstractItem) buildItem.getParent(), buildWrapperClass);
        }

        return null;
    }

    static class BuildWrapperItem<T> {
        final T buildWrapper;
        final AbstractItem buildItem;

        BuildWrapperItem(T buildWrapper, AbstractItem buildItem) {
            this.buildWrapper = buildWrapper;
            this.buildItem = buildItem;
        }
    }
}
