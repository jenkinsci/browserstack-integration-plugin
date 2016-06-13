package com.browserstack.automate.ci.jenkins;

import com.browserstack.automate.ci.common.report.XmlReporter;
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
 * and parses them for test case => session mapping.
 *
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class BrowserStackReportFileCallable extends MasterToSlaveFileCallable<Map<String, String>> {

    private final String filePattern;

    public BrowserStackReportFileCallable(String filePattern) {
        this.filePattern = filePattern;
    }

    @Override
    public Map<String, String> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        FileSet fs = Util.createFileSet(workspace, filePattern);
        DirectoryScanner ds = fs.getDirectoryScanner();
        ds.scan();

        Map<String, String> testSessionMap = new HashMap<String, String>();
        String[] filePaths = ds.getIncludedFiles();
        for (String filePath : filePaths) {
            File f = new File(workspace, filePath);
            if (!f.exists()) {
                continue;
            }

            testSessionMap.putAll(XmlReporter.parse(f));
        }

        return testSessionMap;
    }
}
