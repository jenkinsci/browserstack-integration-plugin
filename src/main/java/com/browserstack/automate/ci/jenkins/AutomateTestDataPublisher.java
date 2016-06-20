package com.browserstack.automate.ci.jenkins;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logDebug;


import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;

import com.browserstack.automate.ci.common.AutomateTestCase;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomateTestDataPublisher extends TestDataPublisher {
    private static final String TAG = "[BrowserStack]";
    private static final String REPORT_FILE_PATTERN = "**/browserstack-reports/REPORT-*.xml";

    @Extension(ordinal = 1000) // JENKINS-12161
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public AutomateTestDataPublisher() {

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
        Map<String, String> testSessionMap = workspace.act(new BrowserStackReportFileCallable(REPORT_FILE_PATTERN, run.getTimeInMillis()));
        AutomateActionData automateActionData = new AutomateActionData();
        Map<String, Long> testCaseIndices = new HashMap<String, Long>();

        int testCount = 0;
        int sessionCount = 0;

        for (SuiteResult suiteResult : testResult.getSuites()) {
            List<CaseResult> cases = suiteResult.getCases();
            testCount += cases.size();
            logDebug(listener.getLogger(), suiteResult.getName() + ": " + cases.size() + " test cases found.");

            for (CaseResult caseResult : cases) {
                String testCaseName = getTestCaseName(caseResult);

                Long testIndex = testCaseIndices.containsKey(testCaseName) ? testCaseIndices.get(testCaseName) : -1L;
                testCaseIndices.put(testCaseName, ++testIndex);
                logDebug(listener.getLogger(), testCaseName + " / " + testCaseName + " <=> " + testIndex);

                String testId = String.format("%s{%d}", testCaseName, testIndex);
                if (testSessionMap.containsKey(testId)) {
                    AutomateTestAction automateTestAction = new AutomateTestAction(run, caseResult, testSessionMap.get(testId));
                    automateActionData.registerTestAction(caseResult.getId(), automateTestAction);
                    logDebug(listener.getLogger(), "registerTestAction: " + testId + " => " + automateTestAction);
                    sessionCount++;
                }
            }
        }

        testCaseIndices.clear();
        log(listener.getLogger(), testCount + " tests recorded");
        log(listener.getLogger(), sessionCount + " sessions captured");
        log(listener.getLogger(), "Publishing test results: SUCCESS");
        return automateActionData;
    }

    public static String getTestCaseName(CaseResult caseResult) {
        return caseResult.getClassName() + "." + AutomateTestCase.stripTestParams(caseResult.getDisplayName());
    }

    private static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Embed BrowserStack Report";
        }
    }
}
