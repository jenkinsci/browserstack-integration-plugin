package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.AutomateTestCase;
import com.browserstack.automate.ci.common.TestCaseTracker;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildWrapper.BuildWrapperItem;
import com.browserstack.automate.model.Session;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.junit.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.browserstack.automate.ci.common.TestCaseTracker.*;

public class AutomateTestDataPublisher extends TestDataPublisher {
    private static final String TAG = "[BrowserStack]";
    private static final String PROPERTY_DEBUG = "browserstack.testassist.debug";

    @Extension(ordinal = 1000) // JENKINS-12161
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private transient TestCaseTracker testCaseTracker;

    @DataBoundConstructor
    public AutomateTestDataPublisher() {

    }

    private TestCaseTracker getTestCaseTracker(final Run<?, ?> run) {
        if (testCaseTracker != null) {
            return testCaseTracker;
        }

        BuildWrapperItem<BrowserStackBuildWrapper> wrapperItem = BrowserStackBuildWrapper.findBrowserStackBuildWrapper(run.getParent());
        if (wrapperItem != null) {
            BrowserStackCredentials credentials = BrowserStackCredentials.getCredentials(wrapperItem.buildItem, wrapperItem.buildWrapper.getCredentialsId());
            if (credentials != null) {
                testCaseTracker = new TestCaseTracker(credentials.getUsername(), credentials.getDecryptedAccesskey());
                TestCaseTracker.setTag(TAG);
            }
        }

        return testCaseTracker;
    }

    @Override
    public TestResultAction.Data getTestData(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener, TestResult testResult) throws IOException, InterruptedException {
        return contributeTestData(abstractBuild, abstractBuild.getWorkspace(), launcher, buildListener, testResult);
    }

    @Override
    public TestResultAction.Data contributeTestData(Run<?, ?> run, @Nonnull FilePath workspace,
                                                    Launcher launcher, TaskListener listener,
                                                    TestResult testResult) throws IOException, InterruptedException {
        log(listener.getLogger(), "Publishing test results");

        AutomateBuildAction action = run.getAction(AutomateBuildAction.class);
        if (action != null) {
            List<AutomateTestCase> testCaseList = action.getTestCaseList();
            logDebug(listener.getLogger(), testCaseList.size() + " test results found.");

            AutomateActionData automateActionData = new AutomateActionData(run);
            Map<String, Long> testCaseIndices = new HashMap<String, Long>();
            Map<String, Session> sessionCache = new HashMap<String, Session>();

            int testCount = 0;
            int sessionCount = 0;

            for (SuiteResult suiteResult : testResult.getSuites()) {
                List<CaseResult> cases = suiteResult.getCases();
                testCount += cases.size();
                logDebug(listener.getLogger(), suiteResult.getName() + ": " + cases.size() + " test cases found.");

                for (CaseResult caseResult : cases) {
                    String testCaseName = getTestCaseName(caseResult);
                    String testCaseHash = getTestCaseHash(caseResult);

                    Long testIndex = testCaseIndices.containsKey(testCaseHash) ? testCaseIndices.get(testCaseHash) : -1L;
                    testCaseIndices.put(testCaseHash, ++testIndex);
                    logDebug(listener.getLogger(), testCaseName + " / " + testCaseHash + " <=> " + testIndex);

                    String[] sessionMatch = findTestCaseSession(testCaseList, testCaseName, testCaseHash, testIndex);
                    if (sessionMatch != null) {
                        String sessionId = sessionMatch[0];
                        String testId = sessionMatch[1];
                        Session session = null;

                        if (sessionCache.containsKey(sessionId)) {
                            session = sessionCache.get(sessionId);
                        } else {
                            try {
                                TestCaseTracker testCaseTracker = getTestCaseTracker(run);
                                if (testCaseTracker == null || testCaseTracker.getAutomateClient() == null) {
                                    logDebug(listener.getLogger(), "Failed to create TestCaseTracker");
                                    continue;
                                }

                                session = testCaseTracker.getAutomateClient().getSession(sessionId);
                                sessionCache.put(session.getId(), session);
                            } catch (Exception e) {
                                logDebug(listener.getLogger(), "ERROR: " + e.getMessage());
                            }
                        }

                        if (session != null) {
                            AutomateTestAction automateTestAction = new AutomateTestAction(run, caseResult);
                            automateTestAction.addSession(session);
                            automateActionData.registerTestAction(testId, automateTestAction);
                            logDebug(listener.getLogger(), "registerTestAction: " + testId + " => " + automateTestAction);
                            sessionCount++;
                        }
                    } else {
                        logDebug(listener.getLogger(), testCaseName + ": No match found");
                    }
                }
            }

            testCaseIndices.clear();
            sessionCache.clear();

            log(listener.getLogger(), testCount + " tests recorded");
            log(listener.getLogger(), sessionCount + " sessions captured");
            log(listener.getLogger(), "Publishing test results: SUCCESS");
            return automateActionData;
        }

        log(listener.getLogger(), "Publishing test results: DONE");
        return null;
    }

    public static String getTestCaseName(CaseResult caseResult) {
        return caseResult.getClassName() + "." + AutomateTestCase.stripTestParams(caseResult.getDisplayName());
    }

    public static String getTestCaseHash(CaseResult caseResult) {
        return TestCaseTracker.getTestCaseHash(getTestCaseName(caseResult));
    }

    private static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Embed BrowserStack Automate Report";
        }
    }
}
