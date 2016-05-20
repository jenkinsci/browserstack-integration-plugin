package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.AutomateTestCase;
import com.browserstack.automate.ci.common.TestCaseTracker;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

class BuildOutputStream extends LineTransformationOutputStream {

    private final AbstractBuild build;
    private final OutputStream outputStream;
    private final Pattern accessKeyPattern;
    private final String accessKeyMask;
    private final List<AutomateTestCase> testCaseList;
    private boolean isDebugEnabled;

    BuildOutputStream(AbstractBuild build, OutputStream outputStream, String accessKey) {
        boolean hasAccesskey = StringUtils.isNotEmpty(accessKey);
        this.build = build;
        this.outputStream = outputStream;
        this.accessKeyPattern = hasAccesskey ? Pattern.compile(accessKey) : null;
        this.accessKeyMask = hasAccesskey ? loadAccessKeyMask(accessKey.length()) : null;
        this.testCaseList = new ArrayList<AutomateTestCase>();
        this.isDebugEnabled = TestCaseTracker.isDebugEnabled();
    }

    private static String loadAccessKeyMask(int len) {
        char[] maskChars = new char[len];
        Arrays.fill(maskChars, '*');
        return new String(maskChars);
    }

    @Override
    protected void eol(byte[] bytes, int len) throws IOException {
        String line = new String(bytes, 0, len);

        AutomateTestCase testCase = AutomateTestCase.parse(line);
        if (testCase != null) {
            if (isDebugEnabled) {
                String testId = testCase.hasTestHash() ? testCase.testHash : testCase.testName;
                String detectedTest = "DEBUG: Detected Test " + testId + "{" + testCase.testIndex + "}";
                System.out.println(detectedTest);
                outputStream.write((detectedTest + "\n").getBytes());
            }

            testCaseList.add(testCase);

            AutomateBuildAction action = build.getAction(AutomateBuildAction.class);
            if (action == null) {
                build.addAction(new AutomateBuildAction(build, testCaseList));
            }
        }

        if (accessKeyPattern != null) {
            outputStream.write(accessKeyPattern.matcher(line).replaceAll(accessKeyMask).getBytes());
        } else {
            outputStream.write(bytes, 0, len);
        }
    }
}