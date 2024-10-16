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
  private static final int MAX_RETRY_ATTEMPTS = 2;
  private static final long RETRY_DELAY_MS = 1000;

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

  public static String uploadApp(Actionable build, PrintStream logger, String appPath, final String customProxy) {
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

    AppUploader appUploader = new AppUploader(appPath, credentials, customProxy, logger);
    String appId = "";
    for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
      try {
        PluginLogger.log(logger, String.format("Uploading app %s to Browserstack. Attempt %d of %d", appPath, attempt, MAX_RETRY_ATTEMPTS));
        appId = appUploader.uploadFile();
        PluginLogger.log(logger,
                String.format("%s uploaded successfully to Browserstack with app_url : %s", appPath, appId));
        return appId;
      } catch (AppAutomateException e) {
        int statusCode = e.getStatusCode();
        PluginLogger.logError(logger, String.format("App upload failed with status code: %d. Attempt %d of %d", statusCode, attempt, MAX_RETRY_ATTEMPTS));
        PluginLogger.logError(logger, e.getMessage());
        if ((statusCode >= 500 || statusCode == 0) && attempt < MAX_RETRY_ATTEMPTS) {
          PluginLogger.log(logger, String.format("Retrying in %d seconds...", RETRY_DELAY_MS / 1000));
          try {
            Thread.sleep(RETRY_DELAY_MS);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            PluginLogger.log(logger, "Upload retry interrupted. Error: " + ie.getMessage());
            return null;
          }
        } else {
          return null;
        }
      } catch (InvalidFileExtensionException | FileNotFoundException e) {
        PluginLogger.logError(logger, e.getMessage());
        return null;
      }
    }
    PluginLogger.logError(logger, String.format("App upload failed after %d attempts", MAX_RETRY_ATTEMPTS));
    return null;
  }

}
