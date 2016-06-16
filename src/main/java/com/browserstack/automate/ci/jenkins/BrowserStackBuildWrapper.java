package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.analytics.Analytics;
import com.browserstack.automate.ci.jenkins.local.JenkinsBrowserStackLocal;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.browserstack.automate.ci.jenkins.util.BrowserListingInfo;
import com.browserstack.client.model.BrowserStackObject;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.BuildListener;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.DescribableList;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.browserstack.automate.ci.common.TestCaseTracker.log;


public class BrowserStackBuildWrapper extends BuildWrapper {
    private static final boolean ENABLE_BROWSER_LISTING = false;


    private final BrowserConfig[] browserConfigs;
    private final LocalConfig localConfig;

    private String credentialsId;
    private String username;
    private String accesskey;
    private boolean hasLoadedBrowsers;
    private BrowserListingInfo browserListingInfo;

    @DataBoundConstructor
    public BrowserStackBuildWrapper(String credentialsId, BrowserConfig[] browserConfig, LocalConfig localConfig) {
        this.credentialsId = credentialsId;
        this.browserConfigs = browserConfig;
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
                buildEnv.startBrowserStackLocal();
            } catch (Exception e) {
                listener.fatalError(e.getMessage());
                throw new IOException(e.getCause());
            }
        }

        if (ENABLE_BROWSER_LISTING) {
            loadBrowsers(logger);
        }

        recordBuildStats(build);
        return buildEnv;
    }

    @Override
    public BrowserStackBuildWrapperDescriptor getDescriptor() {
        return (BrowserStackBuildWrapperDescriptor) super.getDescriptor();
    }

    @Override
    public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException, Run.RunnerAbortedException {
        return new BuildOutputStream(logger, accesskey);
    }

    public BrowserConfig[] getBrowserConfigs() {
        return this.browserConfigs;
    }

    public LocalConfig getLocalConfig() {
        return this.localConfig;
    }

    private void loadBrowsers(PrintStream logger) {
        if (!ENABLE_BROWSER_LISTING || hasLoadedBrowsers) {
            return;
        }

        if (username == null || accesskey == null) {
            log(logger, "Missing BrowserStack credentials");
            return;
        }

        browserListingInfo = BrowserListingInfo.getInstance();
        if (browserListingInfo == null) {
            log(logger, "Error loading browsers: Failed to load OS/Browser list");
            return;
        }

        try {
            browserListingInfo.init(this.username, this.accesskey);
            hasLoadedBrowsers = true;
            log(logger, "Loading browsers... DONE");
        } catch (IOException e) {
            log(logger, "Error loading browsers: " + e.getMessage());
        }
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    private void recordBuildStats(AbstractBuild build) {
        boolean localEnabled = (localConfig != null);
        boolean localPathSet = localEnabled && StringUtils.isNotBlank(localConfig.getLocalPath());
        boolean localOptionsSet = localEnabled && StringUtils.isNotBlank(localConfig.getLocalOptions());
        boolean isReportEnabled = false;

        DescribableList publishersList = build.getProject().getPublishersList();
        if (publishersList != null) {
            Describable describable = publishersList.get(JUnitResultArchiver.class);

            if (describable instanceof JUnitResultArchiver) {
                JUnitResultArchiver jUnitResultArchiver = (JUnitResultArchiver) describable;

                for (TestDataPublisher testDataPublisher : jUnitResultArchiver.getTestDataPublishers()) {
                    if (testDataPublisher instanceof AutomateTestDataPublisher) {
                        isReportEnabled = true;
                        break;
                    }
                }
            }
        }

        Analytics.trackBuildRun(localEnabled, localPathSet, localOptionsSet, isReportEnabled);
    }


    private interface EnvVars {
        String BROWSERSTACK_USER = "BROWSERSTACK_USER";
        String BROWSERSTACK_ACCESSKEY = "BROWSERSTACK_ACCESSKEY";
        String BROWSERSTACK_BROWSERS = "BROWSERSTACK_BROWSERS";
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
                    env.put(EnvVars.BROWSERSTACK_USER, username);
                    logEnvVar(EnvVars.BROWSERSTACK_USER, username);
                }

                if (credentials.hasAccesskey()) {
                    String accesskey = credentials.getDecryptedAccesskey();
                    env.put(EnvVars.BROWSERSTACK_ACCESSKEY, accesskey);
                    logEnvVar(EnvVars.BROWSERSTACK_ACCESSKEY, accesskey);
                }
            }

            if (ENABLE_BROWSER_LISTING) {
                try {
                    String browsersJson = new ObjectMapper().writeValueAsString(generateBrowserList());
                    env.put(EnvVars.BROWSERSTACK_BROWSERS, browsersJson);
                    logEnvVar(EnvVars.BROWSERSTACK_BROWSERS, browsersJson);
                } catch (JsonProcessingException e) {
                    // ignore
                }
            }

            String isLocalEnabled = BrowserStackBuildWrapper.this.localConfig != null ? "true" : "false";
            env.put(EnvVars.BROWSERSTACK_LOCAL, "" + isLocalEnabled);
            logEnvVar(EnvVars.BROWSERSTACK_LOCAL, isLocalEnabled);

            String localIdentifier = (browserstackLocal != null) ? browserstackLocal.getLocalIdentifier() : "";
            if (StringUtils.isNotBlank(localIdentifier)) {
                env.put(EnvVars.BROWSERSTACK_LOCAL_IDENTIFIER, localIdentifier);
                logEnvVar(EnvVars.BROWSERSTACK_LOCAL_IDENTIFIER, localIdentifier);
            }

            String buildTag = env.get(ENV_JENKINS_BUILD_TAG);
            if (buildTag != null) {
                env.put(EnvVars.BROWSERSTACK_BUILD, buildTag);
                logEnvVar(EnvVars.BROWSERSTACK_BUILD, buildTag);
            }

            super.buildEnvVars(env);
        }

        private void logEnvVar(String key, String value) {
            if (!isTearDownPhase) {
                log(logger, key + "=" + value);
            }
        }

        public void startBrowserStackLocal() throws Exception {
            browserstackLocal = new JenkinsBrowserStackLocal(accesskey, localConfig.getLocalOptions());
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

        private List<BrowserStackObject> generateBrowserList() {
            List<BrowserStackObject> allBrowsers = new ArrayList<BrowserStackObject>();

            browserListingInfo = BrowserListingInfo.getInstance();
            if (browserListingInfo == null) {
                return allBrowsers;
            }

            if (browserConfigs != null) {
                for (BrowserConfig browserConfig : browserConfigs) {
                    if (browserConfig.getOs() != null && !browserConfig.getOs().equals("null")) {
                        BrowserStackObject browser = browserListingInfo.getDisplayBrowser(browserConfig.getOs(), browserConfig.getBrowser());
                        if (browser != null) {
                            BrowserStackObject automateBrowser = browserListingInfo.getAutomateBrowser(browser);
                            if (automateBrowser != null) {
                                allBrowsers.add(automateBrowser);
                            }
                        }
                    }
                }
            }

            return allBrowsers;
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
