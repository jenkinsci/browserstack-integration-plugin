package com.browserstack.automate.ci.common;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutomateTestCase implements Serializable {

    private static final Pattern PATTERN_TEST_SESSION = Pattern.compile("^browserstack:session:([^:]+):test:([^\\{]+)\\{([^\\}]+)\\}(.*)");
    private static final String REGEX_TEST_CLASSPATH = "^\\w+(\\.\\w+)+$";
    private static final String REGEX_TEST_ID_HASH = "^\\w+$";
    private static final String PACKAGE_DEFAULT = "(root)";
    private static final String CLASS_DEFAULT = "";
    private static final String TEST_DEFAULT = "";

    public final String sessionId;
    public final String packageName;
    public final String className;
    public final String testName;
    public final String testFullPath;
    public final String testHash;
    public final long testIndex;
    public final String testCaseObjectId;

    public AutomateTestCase(String sessionId, String testHash, long testIndex, String testCaseObjectId) {
        this.sessionId = sessionId;
        this.testHash = testHash;
        this.testIndex = testIndex;
        this.testCaseObjectId = testCaseObjectId;
        this.packageName = PACKAGE_DEFAULT;
        this.className = CLASS_DEFAULT;
        this.testName = TEST_DEFAULT;
        this.testFullPath = null;
    }

    public AutomateTestCase(String sessionId, String packageName, String className, String testName, long testIndex,
                            String testCaseObjectId) {
        this.sessionId = sessionId;
        this.packageName = packageName;
        this.className = className;
        this.testName = stripTestParams(testName);
        this.testIndex = testIndex;
        this.testCaseObjectId = testCaseObjectId;
        this.testFullPath = (this.packageName.equals(PACKAGE_DEFAULT) ? "" : this.packageName + '.')
                + this.className + '.' + this.testName;
        this.testHash = null;
    }

    public boolean hasTestHash() {
        return (testHash != null && testHash.length() > 0);
    }

    public static String stripTestParams(String testCaseName) {
        if (StringUtils.isEmpty(testCaseName)) {
            return null;
        }

        int subscriptIndex = testCaseName.indexOf('[');
        if (subscriptIndex != -1) {
            return testCaseName.substring(0, subscriptIndex);
        }

        return testCaseName;
    }

    public static AutomateTestCase parse(final String line) {
        Matcher matcher = PATTERN_TEST_SESSION.matcher(line);
        if (matcher.find()) {
            String sessionId = matcher.group(1);
            String testCasePath = matcher.group(2);
            String testCaseIndexStr = matcher.group(3);
            String testCaseObjectId = matcher.group(4);
            long testCaseIndex;

            try {
                testCaseIndex = Long.parseLong(testCaseIndexStr);
            } catch (NumberFormatException e) {
                TestCaseTracker.logDebug(System.out, "ERROR: Failed to parse testCaseIndex as Long: " + testCaseIndexStr);
                return null;
            }

            return parseTestCasePath(testCasePath, sessionId, testCaseIndex, testCaseObjectId);
        }

        return null;
    }

    private static AutomateTestCase parseTestCasePath(final String testCasePath, final String sessionId,
                                                      final long testCaseIndex, final String testCaseObjectId) {
        if (testCasePath.matches(REGEX_TEST_CLASSPATH)) {
            TestCaseTracker.logDebug(System.out, "Parsing as package.class.testName");

            int pos = testCasePath.lastIndexOf(".");
            if (pos != -1) {
                // try for package.class.[testName]
                String testCaseName = testCasePath.substring(pos + 1, testCasePath.length());
                String path = testCasePath.substring(0, pos);

                pos = path.lastIndexOf(".");
                if (pos != -1) {
                    // try for [package.class].testName
                    String className = path.substring(pos + 1, testCasePath.length());
                    String packageName = path.substring(0, pos);
                    TestCaseTracker.logDebug(System.out, "Parsed as package.class.testName");
                    return new AutomateTestCase(sessionId, packageName, className, testCaseName, testCaseIndex, testCaseObjectId);
                } else {
                    // try for [class].testName
                    TestCaseTracker.logDebug(System.out, "Parsed as (root).class.testName");
                    return new AutomateTestCase(sessionId, PACKAGE_DEFAULT, path, testCaseName, testCaseIndex, testCaseObjectId);
                }
            }
        }

        if (testCasePath.matches(REGEX_TEST_ID_HASH)) {
            TestCaseTracker.logDebug(System.out, "Parsed as test hash");
            return new AutomateTestCase(sessionId, testCasePath, testCaseIndex, testCaseObjectId);
        }

        return null;
    }
}
