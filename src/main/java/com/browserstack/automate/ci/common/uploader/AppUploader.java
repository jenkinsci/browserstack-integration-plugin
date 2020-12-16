package com.browserstack.automate.ci.common.uploader;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.ci.common.proxysettings.JenkinsProxySettings;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.browserstack.automate.exception.AppAutomateException;
import com.browserstack.automate.exception.InvalidFileExtensionException;

public class AppUploader {

    String appPath;
    BrowserStackCredentials credentials;
    private final PrintStream logger;
    private final String customProxy;

    public AppUploader(String appPath, BrowserStackCredentials credentials, final String customProxy, final PrintStream logger) {
        this.appPath = appPath;
        this.credentials = credentials;
        this.logger = logger;
        this.customProxy = customProxy;
    }

    public String uploadFile()
            throws AppAutomateException, FileNotFoundException, InvalidFileExtensionException {
        AppAutomateClient appAutomateClient =
                new AppAutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());

        JenkinsProxySettings proxy;
        if (customProxy != null) {
            proxy = new JenkinsProxySettings(customProxy, logger);
        } else {
            proxy = new JenkinsProxySettings(logger);
        }

        if (proxy.hasProxy()) {
            appAutomateClient.setProxy(proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy.getPassword());
        }
        return appAutomateClient.uploadApp(this.appPath).getAppUrl();
    }
}
