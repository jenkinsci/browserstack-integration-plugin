package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.AutomateClient;
//import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import com.browserstack.automate.ci.jenkins.local.JenkinsBrowserStackLocal;
import com.browserstack.automate.ci.jenkins.local.LocalConfig;
import com.browserstack.automate.jenkins.helpers.CopyResourceFileToWorkspaceTarget;
import com.browserstack.automate.model.Session;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;
import mockit.Deencapsulation;
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
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Anirudha Khanna
 */
public class AutomateTestDataPublisherTest {

    private static final String DUMMY_BSTACK_USERNAME = "aDummyUsername";
    private static final String DUMMY_BSTACK_ACCESSKEY = "1DummyAccessKey4DummyUser7";
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    @Mocked
    private JenkinsBrowserStackLocal mockedJenkinsBrowserStackLocal;
    @Mocked
    private GoogleAnalytics googleAnalytics;
    private FreeStyleProject project;

    private static void addCredentials(String id, String username, String accessKey) throws IOException {
        BrowserStackCredentials credentials = new BrowserStackCredentials(id,
                "browserstack-credentials-description",
                username,
                accessKey);
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
    public void testThatReportsArePresent() throws Exception {
        /* =================== Prepare ================= */
//        new MockPluginsTracker();
        new MockAutomateClient();

        addBuildStep();
        project.getBuildersList().add(new TouchBuilder());
        project.getBuildersList().add(new CopyResourceFileToWorkspaceTarget("browserstack-reports", "REPORT-com.browserstack.automate.application.tests.TestCaseWithFourUniqueSessions.xml"));
        project.getBuildersList().add(new CopyResourceFileToWorkspaceTarget("surefire-reports", "TEST-com.browserstack.automate.application.tests.TestCaseWithFourUniqueSessions.xml"));

        JUnitResultArchiver jUnitResultArchiver = new JUnitResultArchiver("**/surefire-reports/*.xml");
        jUnitResultArchiver.setTestDataPublishers(Collections.singletonList(new AutomateTestDataPublisher()));
        project.getPublishersList().add(jUnitResultArchiver);

        /* =================== Execute ================= */
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        TestResultAction action = build.getAction(TestResultAction.class);

        /* =================== Verify ================= */
        List<TestResultAction.Data> testData = Deencapsulation.getField(action, "testData");
        // Assumption here is that there is only one suite result based on the XML files
        // copied into the workspace above.
        SuiteResult suiteResult = action.getResult().getSuites().iterator().next();

        jenkinsRule.assertBuildStatus(Result.SUCCESS, build);
        Assert.assertNotNull("Additional data on the test MUST be set.", testData);
        Assert.assertTrue("Additional Test data MUST have values.", testData.size() >= 1);
        TestResultAction.Data data = testData.get(0);
        Assert.assertTrue("Additional Test data MUST be an instance of AutomateActionData.", data instanceof AutomateActionData);

        for (CaseResult caseResult : suiteResult.getCases()) {
            AutomateTestAction automateTestAction = (AutomateTestAction) data.getTestAction(caseResult).get(0);
            Session automateSession = automateTestAction.getSession();
            Assert.assertNotNull("Automate Session should not be null.", automateSession);
            Assert.assertTrue("Session Id should not be null or empty.", StringUtils.isNotEmpty(automateSession.getId()));
        }
    }

    public void addBuildStep() throws IOException {
        String credentialsId = "1";
        addCredentials("1", DUMMY_BSTACK_USERNAME, DUMMY_BSTACK_ACCESSKEY);
        LocalConfig localConfig = new LocalConfig();
        localConfig.setLocalOptions("-force");

        BrowserStackBuildWrapper buildWrapper = new BrowserStackBuildWrapper(credentialsId, localConfig);
        project.getBuildWrappersList().add(buildWrapper);
    }

    private static final class MockAutomateClient extends MockUp<AutomateClient> {

        @Mock
        public void $init(String userName, String accessKey) {
            Assert.assertEquals("User name not equal to what is set.", DUMMY_BSTACK_USERNAME, userName);
            Assert.assertEquals("User Access Key not equal to what is set.", DUMMY_BSTACK_ACCESSKEY, accessKey);
        }

        @Mock
        public Session getSession(String sessionId) {
            Session session = new Session(null, sessionId);
            return session;
        }
    }

//    private static final class MockPluginsTracker extends MockUp<PluginsTracker> {
//        @Mock
//        public void sendError(String errorMessage, boolean pipelineStatus, String phase) {
//            return;
//        }
//
//        @Mock
//        public void pluginInitialized(String buildName, boolean localStatus, boolean pipelineStatus) {
//            return;
//        }
//
//        @Mock
//        public void reportGenerationInitialized(String buildName, String product, boolean pipelineStatus) {
//            return;
//        }
//
//        @Mock
//        public void reportGenerationCompleted(String status, String product, boolean pipelineStatus, String buildName, String buildId) {
//            return;
//        }
//    }


}
