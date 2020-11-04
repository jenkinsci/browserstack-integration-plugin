package com.browserstack.automate.ci.common.uploader;

import java.io.FileNotFoundException;
import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.ci.common.proxysettings.JenkinsProxySettings;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.browserstack.automate.exception.AppAutomateException;
import com.browserstack.automate.exception.InvalidFileExtensionException;

public class AppUploader {

  String appPath;
  BrowserStackCredentials credentials;

  public AppUploader(String appPath, BrowserStackCredentials credentials) {
    this.appPath = appPath;
    this.credentials = credentials;
  }

  public String uploadFile()
      throws AppAutomateException, FileNotFoundException, InvalidFileExtensionException {
    AppAutomateClient appAutomateClient =
        new AppAutomateClient(credentials.getUsername(), credentials.getDecryptedAccesskey());
        if(JenkinsProxySettings.hasProxy()){
         appAutomateClient.setProxy(JenkinsProxySettings.getHost(), JenkinsProxySettings.getPort(), JenkinsProxySettings.getUsername(), JenkinsProxySettings.getPassword());
        }
    return appAutomateClient.uploadApp(this.appPath).getAppUrl();
  }
}
