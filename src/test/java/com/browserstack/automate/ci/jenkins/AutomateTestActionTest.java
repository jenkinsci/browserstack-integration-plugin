package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.AutomateClient;
import com.browserstack.automate.ci.common.model.BrowserStackSession;
import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import com.browserstack.automate.ci.jenkins.local.JenkinsBrowserStackLocal;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.browserstack.automate.exception.AutomateException;
import com.browserstack.automate.exception.SessionNotFound;
import com.browserstack.automate.model.Session;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.junit.CaseResult;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TouchBuilder;

import java.io.IOException;

/**
 * Unit test for {@link AutomateTestAction} class.
 *
 * @author Anirudha Khanna
 */
public class AutomateTestActionTest {

    private static final String DUMMY_BSTACK_USERNAME = "aDummyUsername";
    private static final String DUMMY_BSTACK_ACCESSKEY = "1DummyAccessKey4DummyUser7";
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    @Mocked
    private JenkinsBrowserStackLocal mockedJenkinsBrowserStackLocal;
    @Mocked
    private GoogleAnalytics googleAnalytics;
    @Mocked
    private CaseResult mockedCaseResult;
    private FreeStyleProject project;

    private static void addCredentials(String id, String username, String accessKey)
            throws IOException {
        BrowserStackCredentials credentials = new BrowserStackCredentials(id,
                "browserstack-credentials-description", username, accessKey);
        addCredentials(credentials);
    }

    private static void addCredentials(BrowserStackCredentials credentials) throws IOException {
        CredentialsStore store = new SystemCredentialsProvider.UserFacingAction().getStore();
        store.addCredentials(Domain.global(), credentials);
    }

    @Before
    public void setUp() throws Exception {
        jenkinsRule.recipeLoadCurrentPlugin();
        jenkinsRule.configRoundtrip();
        project = jenkinsRule.createFreeStyleProject("browserstack-plugin-test");
    }

    @Test
    public void testAutomateExceptionIsHandled() throws Exception {
        /* =================== Prepare ================= */
        new MockAutomateClientThatThrowsAutomateException();
        new MockPluginsTracker();
        addBuildStep();
        project.getBuildersList().add(new TouchBuilder());

        /* =================== Execute ================= */
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BrowserStackSession browserStackSession = new BrowserStackSession("Random4756SessionId", "");
        Gson gson = new GsonBuilder().create();
        AutomateTestAction automateTestAction =
                new AutomateTestAction(build, mockedCaseResult, gson.toJson(browserStackSession));
        Session automateSession = automateTestAction.getSession();
        /* =================== Verify ================= */
        Assert.assertNull("Automate Session MUST be null.", automateSession);
        Assert.assertNotNull("Exception MUST not be null.", automateTestAction.getLastException());
        Assert.assertTrue("Exception should be of Type AutomateException",
                automateTestAction.getLastException() instanceof AutomateException);
        Assert.assertTrue("Exception message MUST not be empty",
                StringUtils.isNotEmpty(automateTestAction.getLastError()));
    }

    @Test
    public void testSessionNotFoundExceptionIsHandled() throws Exception {
        /* =================== Prepare ================= */
        new MockAutomateClientThatThrowsSessionNotFoundException();
        new MockPluginsTracker();
        addBuildStep();
        project.getBuildersList().add(new TouchBuilder());

        /* =================== Execute ================= */
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        BrowserStackSession browserStackSession = new BrowserStackSession("Random4756SessionId", "");
        Gson gson = new GsonBuilder().create();
        AutomateTestAction automateTestAction =
                new AutomateTestAction(build, mockedCaseResult, gson.toJson(browserStackSession));
        Session automateSession = automateTestAction.getSession();

        /* =================== Verify ================= */
        Assert.assertNull("Automate Session MUST be null.", automateSession);
        Assert.assertNotNull("Exception MUST not be null.", automateTestAction.getLastException());
        Assert.assertTrue("Exception should be of Type SessionNotFound",
                automateTestAction.getLastException() instanceof SessionNotFound);
        Assert.assertTrue("Exception message MUST not be empty",
                StringUtils.isNotEmpty(automateTestAction.getLastError()));
    }

    public void addBuildStep() throws IOException {
        String credentialsId = "1";
        addCredentials("1", DUMMY_BSTACK_USERNAME, DUMMY_BSTACK_ACCESSKEY);
        LocalConfig localConfig = new LocalConfig();
        localConfig.setLocalOptions("-force");

        BrowserStackBuildWrapper buildWrapper =
                new BrowserStackBuildWrapper(credentialsId);
        buildWrapper.setLocalConfig(localConfig);
        project.getBuildWrappersList().add(buildWrapper);
    }

    private static final class MockAutomateClientThatThrowsAutomateException
            extends MockUp<AutomateClient> {

        @Mock
        public Session getSession(String sessionId) throws Exception {
            throw new AutomateException("Random Exception", 400);
        }

    }

    private static final class MockAutomateClientThatThrowsSessionNotFoundException
            extends MockUp<AutomateClient> {

        @Mock
        public Session getSession(String sessionId) throws Exception {
            throw new SessionNotFound("Random session not found.");
        }

    }

    private static final class MockPluginsTracker extends MockUp<PluginsTracker> {
        @Mock
        public void sendError(String errorMessage, boolean pipelineStatus, String phase) {
            return;
        }

        @Mock
        public void pluginInitialized(String buildName, boolean localStatus, boolean pipelineStatus) {
            return;
        }

        @Mock
        public void reportGenerationInitialized(String buildName, String product, boolean pipelineStatus) {
            return;
        }

        @Mock
        public void reportGenerationCompleted(String status, String product, boolean pipelineStatus, String buildName, String buildId) {
            return;
        }
    }

}
