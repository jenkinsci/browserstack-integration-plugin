package com.browserstack.automate.ci.jenkins;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomateActionData extends TestResultAction.Data {

    private final Map<String, TestAction> testActionMap;

    public AutomateActionData() {
        this.testActionMap = new HashMap<String, TestAction>();
    }

    public void registerTestAction(final String testCaseId, final TestAction testAction) {
        testActionMap.put(testCaseId, testAction);
    }

    @Override
    public List<? extends TestAction> getTestAction(TestObject testObject) {
        if (testObject instanceof CaseResult) {
            String caseResultId = testObject.getId();
            if (testActionMap.containsKey(caseResultId)) {
                return Collections.singletonList(testActionMap.get(caseResultId));
            }
        }

        return Collections.emptyList();
    }

}
