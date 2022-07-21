package com.browserstack.automate.ci.jenkins;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class BrowserStackCypressReportFileCallable extends MasterToSlaveFileCallable<String> {

    private final String filepath;

    public BrowserStackCypressReportFileCallable(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        String jsonTxt = null;
        final String reportJSONPath = filepath + "/results/browserstack-cypress-report.json";
        try {
            InputStream is = new FileInputStream(reportJSONPath);
            jsonTxt = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        return jsonTxt;
    }
}
