package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.report.XmlReporter;
import hudson.AbortException;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds XML files in the workspace that match the supplied Ant pattern,
 * and parses them for test case - session mapping.
 *
 * Logic for detection of stale report files is from Jenkins JUnit plugin.
 *
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class BrowserStackReportFileCallable extends MasterToSlaveFileCallable<Map<String, String>> {

    private final String filePattern;

    private final long buildTime;

    private final long masterTime;

    public BrowserStackReportFileCallable(String filePattern, long buildTime) {
        this.filePattern = filePattern;
        this.buildTime = buildTime;
        this.masterTime = System.currentTimeMillis();
    }

    @Override
    public Map<String, String> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        long slaveTime = System.currentTimeMillis();

        FileSet fs = Util.createFileSet(workspace, filePattern);
        DirectoryScanner ds = fs.getDirectoryScanner();
        ds.scan();

        Map<String, String> testSessionMap = new HashMap<String, String>();
        String[] filePaths = ds.getIncludedFiles();
        if (filePaths.length == 0) {
            return testSessionMap;
        }

        boolean parsed = false;
        long buildTime = this.buildTime + (slaveTime - masterTime);

        for (String filePath : filePaths) {
            File f = new File(workspace, filePath);
            if (!f.exists()) {
                continue;
            }

            if (buildTime - 3000 <= f.lastModified()) {
                Map<String, String> results = XmlReporter.parse(f);
                if (!results.isEmpty()) {
                    testSessionMap.putAll(results);
                    parsed = true;
                }
            }
        }

        if (!parsed) {
            if (slaveTime < buildTime - 1000) {
                // build time is in the the future. clock on this slave must be running behind
                throw new AbortException(
                        "Clock on this slave is out of sync with the master, and therefore \n" +
                                "I can't figure out what test results are new and what are old.\n" +
                                "Please keep the slave clock in sync with the master.");
            }

            File f = new File(workspace, filePaths[0]);
            throw new AbortException(
                    String.format(
                            "Test reports were found but none of them are new. Did tests run? %n" +
                                    "For example, %s is %s old%n", f,
                            Util.getTimeSpanString(buildTime - f.lastModified())));
        }

        return testSessionMap;
    }
}
