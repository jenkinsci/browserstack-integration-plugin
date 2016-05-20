package com.browserstack.automate.ci.jenkins;

import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomateActionData extends TestResultAction.Data {

    private transient final Run<?, ?> run;
    private final Map<String, TestAction> testActionMap;

    public AutomateActionData(Run<?, ?> run) {
        this.run = run;
        this.testActionMap = new HashMap<String, TestAction>();
    }

    public void registerTestAction(final String testCaseId, final TestAction testAction) {
        testActionMap.put(testCaseId, testAction);
    }

    @Override
    public List<? extends TestAction> getTestAction(TestObject testObject) {
        if (testObject instanceof CaseResult) {
            CaseResult caseResult = (CaseResult) testObject;

            String testCaseHash = AutomateTestDataPublisher.getTestCaseHash(caseResult);
            if (testActionMap.containsKey(testCaseHash)) {
                return Collections.singletonList(testActionMap.get(testCaseHash));
            }

            String testCaseName = AutomateTestDataPublisher.getTestCaseName(caseResult);
            if (testActionMap.containsKey(testCaseName)) {
                return Collections.singletonList(testActionMap.get(testCaseName));
            }
        }

        return Collections.emptyList();
    }


}
