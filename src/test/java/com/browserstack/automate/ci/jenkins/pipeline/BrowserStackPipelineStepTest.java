package com.browserstack.automate.ci.jenkins.pipeline;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.browserstack.automate.jenkins.helpers.TempCredentialIdGenerator;

public class BrowserStackPipelineStepTest {

  private static final String DUMMY_BSTACK_USERNAME = "aDummyUsername";
  private static final String DUMMY_BSTACK_ACCESS_KEY = "1DummyAccessKey4DummyUser7";

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void testBrowserStepWithoutLocal() throws Exception {
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



}
