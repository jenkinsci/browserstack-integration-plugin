package com.browserstack.automate.ci.jenkins.pipeline;

//import com.browserstack.automate.ci.common.tracking.PluginsTracker;
import com.browserstack.automate.jenkins.helpers.TempCredentialIdGenerator;
import mockit.Mock;
import mockit.MockUp;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BrowserStackPipelineStepTest {

    private static final String DUMMY_BSTACK_USERNAME = "aDummyUsername";
    private static final String DUMMY_BSTACK_ACCESS_KEY = "1DummyAccessKey4DummyUser7";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testBrowserStepWithoutLocal() throws Exception {
//        new MockPluginsTracker();
        String credentialsId = TempCredentialIdGenerator.generateTempCredentialId(DUMMY_BSTACK_USERNAME,
                DUMMY_BSTACK_ACCESS_KEY);

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class,
                "BrowserStackStepTest-browserStackTest");
        p.setDefinition(new CpsFlowDefinition(
                "node { browserstack(credentialsId: '" + credentialsId + "') { \n" +
                        "echo 'USERNAME = ' + env.BROWSERSTACK_USERNAME\n" +
                        "echo 'ACCESS_KEY = ' + env.BROWSERSTACK_ACCESS_KEY\n" +
                        "}}",
                true));
        WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("USERNAME = " + DUMMY_BSTACK_USERNAME, run);
        jenkinsRule.assertLogContains("ACCESS_KEY = " + DUMMY_BSTACK_ACCESS_KEY, run);
    }

    @Test
    public void testBrowserStepWithLocal() throws Exception {
//        new MockPluginsTracker();
        String username = System.getenv("BROWSERSTACK_USERNAME");
        String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");

        String credentialsId = TempCredentialIdGenerator.generateTempCredentialId(username,
                accessKey);

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class,
                "BrowserStackStepTest-browserStackTest");
        p.setDefinition(new CpsFlowDefinition(
                "node { browserstack(credentialsId: '" + credentialsId + "', localConfig: [localOptions: '', localPath: '']) { \n" +
                        "echo 'USERNAME = ' + env.BROWSERSTACK_USERNAME\n" +
                        "echo 'ACCESS_KEY = ' + env.BROWSERSTACK_ACCESS_KEY\n" +
                        "echo 'BROWSERSTACK_LOCAL = ' + env.BROWSERSTACK_LOCAL\n" +
                        "echo 'BROWSERSTACK_LOCAL_IDENTIFIER = ' + env.BROWSERSTACK_LOCAL_IDENTIFIER\n" +
                        "}}",
                true));
        WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(p.scheduleBuild2(0));
        jenkinsRule.assertLogContains("USERNAME = " + username, run);
        jenkinsRule.assertLogContains("ACCESS_KEY = " + accessKey, run);
        jenkinsRule.assertLogContains("BROWSERSTACK_LOCAL = true", run);
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
