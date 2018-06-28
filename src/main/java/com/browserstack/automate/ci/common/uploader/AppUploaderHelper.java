package com.browserstack.automate.ci.common.uploader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import com.browserstack.automate.ci.common.logger.PluginLogger;
import com.browserstack.automate.ci.jenkins.BrowserStackBuildAction;
import com.browserstack.automate.ci.jenkins.BrowserStackCredentials;
import com.browserstack.automate.exception.AppAutomateException;
import com.browserstack.automate.exception.InvalidFileExtensionException;
import hudson.model.Actionable;
import hudson.util.FormValidation;

public class AppUploaderHelper {

  public static FormValidation validateAppPath(String appPath) {
    if (appPath == null || appPath.isEmpty()) {
      return FormValidation.error("Please enter absolute path to your app.");
    }
    File file = new File(appPath);
    if (!file.exists()) {
      return FormValidation.error("File not found : " + appPath);
    }

    if (!appPath.endsWith(".apk") && !appPath.endsWith(".ipa")) {
      return FormValidation.error("File extension should be only .apk or .ipa.");
    }

    return FormValidation.ok();
  }

  public static String uploadApp(Actionable build, PrintStream logger, String appPath) {
    PluginLogger.log(logger, "Starting upload process.");

    BrowserStackBuildAction browserStackBuildAction =
        build.getAction(BrowserStackBuildAction.class);
    if (browserStackBuildAction == null) {
      PluginLogger.logError(logger, "Error in fetching browserStackBuildAction.");
      return null;
    }

    BrowserStackCredentials credentials = browserStackBuildAction.getBrowserStackCredentials();
    if (credentials == null) {
      PluginLogger.logError(logger, "Was not able to fetch credentials in AppUploadBuilder.");
      return null;
    }

    AppUploader appUploader = new AppUploader(appPath, credentials);
    String appId = "";
    try {
      PluginLogger.log(logger, "Uploading app " + appPath + " to Browserstack.");
      appId = appUploader.uploadFile();
      PluginLogger.log(logger,
          appPath + " uploaded successfully to Browserstack with app_url : " + appId);
    } catch (AppAutomateException e) {
      PluginLogger.logError(logger, "App upload failed.");
      PluginLogger.logError(logger, e.getMessage());
      return null;
    } catch (InvalidFileExtensionException e) {
      PluginLogger.logError(logger, e.getMessage());
      return null;
    } catch (FileNotFoundException e) {
      PluginLogger.logError(logger, e.getMessage());
      return null;
    }
    return appId;
  }

}
