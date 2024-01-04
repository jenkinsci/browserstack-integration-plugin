package com.browserstack.automate.ci.jenkins.qualityDashboard;

import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

public class QualityDashboardUtil {
    public static BrowserStackCredentials getBrowserStackCreds() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        List<StandardCredentials> creds = CredentialsProvider.lookupCredentials(StandardCredentials.class,jenkins,null,new ArrayList<>());
        BrowserStackCredentials browserStackCredentials = (BrowserStackCredentials) creds.stream().filter(c -> c instanceof BrowserStackCredentials).findFirst().orElse(null);
        return browserStackCredentials;
    }
}
